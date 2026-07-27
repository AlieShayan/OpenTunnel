package dev.opentunnel.vpn.core

import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import dev.opentunnel.vpn.BuildConfig
import dev.opentunnel.vpn.data.AppSettings
import dev.opentunnel.vpn.data.SplitTunnelMode
import dev.opentunnel.vpn.data.VpnProfile
import dev.opentunnel.vpn.util.Cidr
import dev.opentunnel.vpn.util.Net
import org.infradead.libopenconnect.LibOpenConnect
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

/** What the runner needs from the hosting VpnService. */
interface TunnelHost {
    fun newBuilder(): VpnService.Builder
    fun protectSocket(socket: Int): Boolean

    /** Path to a PEM bundle of the device's trust anchors, or null. */
    fun caBundlePath(): String?
    fun persistCertificatePin(fingerprint: String)
    fun onTunnelFinished(error: String?)
}

/** Callbacks libopenconnect raises on the tunnel thread. */
private interface SessionCallbacks {
    fun onProgress(level: Int, message: String?)
    fun onProtectSocket(fd: Int)
    fun onStatsUpdate(stats: LibOpenConnect.VPNStats?)
    fun onReconnected()
    fun onSetupTun()
    fun onValidatePeerCert(reason: String?): Int
    fun onProcessAuthForm(form: LibOpenConnect.AuthForm?): Int
}

/**
 * Thin subclass of the upstream JNI binding that forwards every callback to a
 * [SessionCallbacks]. Two constructors mirror the two upstream ones so a custom
 * user agent can be supplied without ever passing null down into JNI (the C
 * side calls GetStringUTFChars unconditionally).
 */
private class Session : LibOpenConnect {

    lateinit var callbacks: SessionCallbacks

    constructor(userAgent: String) : super(userAgent)

    override fun onProgress(level: Int, msg: String?) = callbacks.onProgress(level, msg)
    override fun onProtectSocket(fd: Int) = callbacks.onProtectSocket(fd)
    override fun onStatsUpdate(stats: LibOpenConnect.VPNStats?) = callbacks.onStatsUpdate(stats)
    override fun onReconnected() = callbacks.onReconnected()
    override fun onSetupTun() = callbacks.onSetupTun()
    override fun onValidatePeerCert(msg: String?): Int = callbacks.onValidatePeerCert(msg)
    override fun onProcessAuthForm(form: LibOpenConnect.AuthForm?): Int =
        callbacks.onProcessAuthForm(form)
}

/**
 * Drives one libopenconnect session on its own thread.
 *
 * Sequence mirrors the upstream Android reference client:
 *
 *   parseURL → obtainCookie (auth) → makeCSTPConnection → getIPInfo →
 *   VpnService.Builder.establish() → setupTunFD → setupDTLS → mainloop
 *
 * mainloop() blocks until the session ends; transport-level reconnects happen
 * inside the library and reuse the same tun fd.
 */
class TunnelRunner(
    private val host: TunnelHost,
    private val profile: VpnProfile,
    private val settings: AppSettings,
) : Thread("openconnect-tunnel") {

    private val disconnectRequested = AtomicBoolean(false)
    private val userCancelled = AtomicBoolean(false)

    @Volatile
    private var session: Session? = null

    @Volatile
    private var tunFd: ParcelFileDescriptor? = null

    /** Password for this attempt — from the profile, or from a prompt. */
    @Volatile
    private var password: String = profile.password

    private var passwordConsumed = false
    private var authGroupApplied = false

    // ── control surface (callable from any thread) ──────────────────────────

    fun requestDisconnect() {
        if (!disconnectRequested.compareAndSet(false, true)) return
        VpnBus.setStage(ConnectionStage.DISCONNECTING)
        Interaction.cancelPending()
        runCatching { session?.cancel() }
    }

    /** Ask libopenconnect to drop and re-establish the transport. */
    fun requestReconnect() {
        if (disconnectRequested.get()) return
        VpnBus.setStage(ConnectionStage.RECONNECTING)
        runCatching { session?.pause() }
    }

    fun pollStats() {
        runCatching { session?.requestStats() }
    }

    // ── main flow ───────────────────────────────────────────────────────────

    override fun run() {
        var failure: String? = null
        try {
            failure = connect()
        } catch (t: Throwable) {
            failure = t.message ?: t.javaClass.simpleName
            VpnBus.error("Tunnel stopped unexpectedly: $failure")
        } finally {
            closeTun()
            runCatching { session?.destroy() }
            session = null
            host.onTunnelFinished(if (disconnectRequested.get()) null else failure)
        }
    }

    private fun connect(): String? {
        if (!NativeLibrary.isAvailable) {
            VpnBus.error(NativeLibrary.MISSING_MESSAGE)
            return NativeLibrary.MISSING_MESSAGE
        }

        VpnBus.setStage(ConnectionStage.PREPARING)
        VpnBus.info("openconnect ${NativeLibrary.version() ?: BuildConfig.OPENCONNECT_VERSION}")

        val userAgent = profile.userAgent.trim().ifEmpty { defaultUserAgent }
        val lib = Session(userAgent)
        lib.callbacks = Callbacks()
        session = lib

        applyPreferences(lib)

        val rawUrl = normaliseServer(profile.server)
        val url = resolveServerUrl(rawUrl, lib)
        VpnBu