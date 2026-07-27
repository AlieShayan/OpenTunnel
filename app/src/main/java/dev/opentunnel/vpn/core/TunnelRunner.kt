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
        VpnBus.info("Connecting to $url")
        if (lib.parseURL(url) != 0) {
            return "Could not parse the server address “${profile.server}”."
        }

        VpnBus.setStage(ConnectionStage.AUTHENTICATING)
        val cookieResult = lib.obtainCookie()
        if (disconnectRequested.get()) return null
        if (cookieResult != 0) {
            return when {
                userCancelled.get() -> "Sign-in was cancelled."
                cookieResult > 0 -> "Sign-in was cancelled."
                else -> "Authentication failed. Check the server address, username and password."
            }
        }

        VpnBus.setStage(ConnectionStage.CONNECTING)
        if (lib.makeCSTPConnection() != 0) {
            return "The gateway accepted the login but refused to establish the tunnel."
        }

        val ipInfo = lib.getIPInfo() ?: return "The gateway did not send an IP configuration."
        val descriptor = establishTun(ipInfo) ?: return "Android refused to create the tunnel interface."
        tunFd = descriptor

        if (lib.setupTunFD(descriptor.fd) != 0) {
            return "Could not hand the tunnel interface to openconnect."
        }

        if (profile.enableDtls) {
            lib.setupDTLS(DTLS_ATTEMPT_SECONDS)
        }

        VpnBus.resetStatsWindow()
        VpnBus.setConnected(SystemClock.elapsedRealtime(), describe(lib, ipInfo))
        VpnBus.info("Tunnel is up")

        // mainloop() returns >= 0 when it has paused and wants to be called
        // again, and < 0 once the session is finished (including after cancel()).
        while (true) {
            if (lib.mainloop(RECONNECT_TIMEOUT_SECONDS, LibOpenConnect.RECONNECT_INTERVAL_MIN) < 0) break
        }
        return null
    }

    // ── configuration ───────────────────────────────────────────────────────

    private fun applyPreferences(lib: Session) {
        lib.setLogLevel(
            if (settings.verboseLogging) LibOpenConnect.PRG_DEBUG else LibOpenConnect.PRG_INFO
        )

        if (lib.setProtocol(profile.protocol) != 0) {
            VpnBus.error("Protocol “${profile.protocol}” is not supported; falling back to anyconnect")
            lib.setProtocol("anyconnect")
        }

        if (profile.caCertPath.isNotBlank()) {
            lib.setCAFile(profile.caCertPath)
            VpnBus.log(LogLevel.DEBUG, "Using custom CA certificate: ${profile.caCertPath}")
        } else {
            host.caBundlePath()?.let { path ->
                lib.setCAFile(path)
                VpnBus.log(LogLevel.DEBUG, "Using the device trust store for certificate validation")
            }
        }

        if (profile.userCertPath.isNotBlank()) {
            lib.setClientCert(profile.userCertPath, profile.privateKeyPath)
            VpnBus.log(LogLevel.DEBUG, "Client certificate enabled")
        }

        if (profile.softwareTokenMode > 0 && profile.tokenString.isNotBlank()) {
            lib.setTokenMode(profile.softwareTokenMode, profile.tokenString)
            VpnBus.log(LogLevel.DEBUG, "Software token configured (mode ${profile.softwareTokenMode})")
        }

        if (profile.csdWrapper.isNotBlank()) {
            lib.setCSDWrapper(profile.csdWrapper, "", "")
        }

        lib.setReportedOS(profile.reportedOs)
        lib.setMobileInfo("1.0", profile.reportedOs, DEVICE_ID)
        lib.setXMLPost(!profile.disableXmlPost)
        lib.setPFS(profile.requirePfs)

        val reqMtu = if (profile.mtu > 0) profile.mtu else 1350
        lib.setReqMTU(reqMtu)
        VpnBus.log(LogLevel.DEBUG, "Requested MTU $reqMtu")

        if (profile.dpdSeconds > 0) lib.setDPD(profile.dpdSeconds)
        if (!profile.enableDtls) lib.disableDTLS()
        if (!profile.enableIpv6) lib.disableIPv6()
        if (profile.allowInsecureCrypto) {
            lib.setAllowInsecureCrypto(true)
            VpnBus.log(LogLevel.DEBUG, "Legacy cipher suites enabled for this profile")
        }
    }

    /** openconnect wants a URL; people type "vpn.example.com". */
    private fun normaliseServer(raw: String): String {
        val value = raw.trim()
        return when {
            value.isEmpty() -> value
            value.startsWith("https://", ignoreCase = true) -> value
            value.startsWith("http://", ignoreCase = true) -> "https://" + value.substring(7)
            else -> "https://$value"
        }
    }

    private fun resolveServerUrl(rawUrl: String, lib: Session): String {
        return runCatching {
            val uri = java.net.URI(rawUrl)
            val host = uri.host ?: return rawUrl
            if (Net.isValidIp(host)) return rawUrl   // already an IP

            lib.setHostname(host)

            // Try system DNS first
            val systemIp = runCatching {
                java.net.InetAddress.getByName(host).hostAddress
            }.getOrNull()

            if (systemIp != null && systemIp.isNotBlank()) {
                VpnBus.log(LogLevel.DEBUG, "Resolved $host → $systemIp (system DNS)")
                return buildResolvedUrl(uri, systemIp)
            }

            // System DNS failed - fallback to DoH via Cloudflare / Google
            VpnBus.log(LogLevel.DEBUG, "System DNS failed for $host, trying DoH fallback")
            val dohIp = resolveViaDoh(host)
            if (dohIp != null) {
                VpnBus.log(LogLevel.DEBUG, "Resolved $host → $dohIp (DoH fallback)")
                return buildResolvedUrl(uri, dohIp)
            }

            VpnBus.log(LogLevel.DEBUG, "DNS resolution failed for $host, trying hostname directly")
            rawUrl
        }.getOrDefault(rawUrl)
    }

    private fun buildResolvedUrl(uri: java.net.URI, ip: String): String {
        val safeIp = if (ip.contains(':')) "[$ip]" else ip  // IPv6 needs brackets
        val port = if (uri.port > 0) ":${uri.port}" else ""
        val path = uri.rawPath.ifEmpty { "/" }
        return "https://$safeIp$port$path"
    }

    /**
     * DNS-over-HTTPS fallback via Cloudflare 1.1.1.1.
     * VpnService.protect() is NOT called here because the VPN tunnel is not yet up.
     */
    private fun resolveViaDoh(hostname: String): String? {
        val encodedName = java.net.URLEncoder.encode(hostname, "UTF-8")
        val dohUrls = listOf(
            "https://1.1.1.1/dns-query?name=$encodedName&type=A",
            "https://8.8.8.8/dns-query?name=$encodedName&type=A",
        )
        for (url in dohUrls) {
            val ip = runCatching {
                val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                conn.setRequestProperty("Accept", "application/dns-json")
                conn.connectTimeout = 5_000
                conn.readTimeout = 5_000
                conn.connect()
                val body = conn.inputStream.bufferedReader().readText()
                conn.disconnect()
                // Simple JSON regex parsing to extract first A record
                val regex = Regex(""""data"\s*:\s*"([\d.]+)"""")
                regex.find(body)?.groupValues?.get(1)
            }.getOrNull()
            if (!ip.isNullOrBlank()) return ip
        }
        return null
    }

    // ── tun setup ───────────────────────────────────────────────────────────

    private fun establishTun(ip: LibOpenConnect.IPInfo): ParcelFileDescriptor? {
        val builder = host.newBuilder()
        builder.setSession(profile.displayName)

        var haveAddress = false

        Net.parseCidr(ip.addr.orEmpty(), ip.netmask)?.let { cidr ->
            runCatching { builder.addAddress(cidr.address, cidr.prefixLength) }
                .onSuccess {
                    VpnBus.log(LogLevel.DEBUG, "IPv4 ${cidr.address}/${cidr.prefixLength}")
                    haveAddress = true
                }
                .onFailure { VpnBus.error("Rejected IPv4 address ${cidr.address}/${cidr.prefixLength}") }
        }

        if (profile.enableIpv6) {
            val v6 = ip.netmask6?.takeIf { it.isNotBlank() } ?: ip.addr6
            Net.parseCidr(v6.orEmpty())?.let { cidr ->
                runCatching { builder.addAddress(cidr.address, cidr.prefixLength) }
                    .onSuccess {
                        VpnBus.log(LogLevel.DEBUG, "IPv6 ${cidr.address}/${cidr.prefixLength}")
                        haveAddress = true
                    }
            }
        }

        if (!haveAddress) {
            VpnBus.error("The gateway did not assign a usable IP address")
            return null
        }

        val mtu = when {
            profile.mtu > 0 -> profile.mtu
            ip.MTU >= MIN_MTU -> ip.MTU
            else -> DEFAULT_MTU
        }
        builder.setMtu(mtu)
        VpnBus.log(LogLevel.DEBUG, "MTU $mtu")

        applyRoutes(builder, ip)
        applyDns(builder, ip)
        applySplitTunnel(builder)

        builder.setBlocking(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            builder.setMetered(false)
        }

        return runCatching { builder.establish() }
            .onFailure { VpnBus.error("establish() failed: ${it.message}") }
            .getOrNull()
    }

    private fun applyRoutes(builder: VpnService.Builder, ip: LibOpenConnect.IPInfo) {
        val serverIncludes = ip.splitIncludes.orEmpty().mapNotNull { Net.parseCidr(it) }
        val serverExcludes = ip.splitExcludes.orEmpty().mapNotNull { Net.parseCidr(it) }

        if (serverIncludes.isNotEmpty()) {
            // The gateway is already doing route-based split tunnelling.
            serverIncludes.forEach { addRoute(builder, it) }
            VpnBus.info("Gateway supplied ${serverIncludes.size} split-tunnel route(s)")
            return
        }

        val excludes = buildList {
            addAll(serverExcludes)
            if (settings.bypassLocalNetworks) addAll(Net.LOCAL_NETWORKS)
        }

        if (excludes.isEmpty()) {
            addRoute(builder, Cidr("0.0.0.0", 0, false))
            if (profile.enableIpv6) addRoute(builder, Cidr("::", 0, true))
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            addRoute(builder, Cidr("0.0.0.0", 0, false))
            if (profile.enableIpv6) addRoute(builder, Cidr("::", 0, true))
            var excluded = 0
            for (block in excludes) {
                val applied = runCatching {
                    builder.excludeRoute(
                        android.net.IpPrefix(
                            java.net.InetAddress.getByName(block.address),
                            block.prefixLength,
                        )
                    )
                }.isSuccess
                if (applied) excluded++
            }
            VpnBus.info("Kept $excluded local range(s) off the tunnel")
        } else {
            // No excludeRoute() before Android 13 — install the complement.
            val routes = Net.ipv4DefaultMinus(excludes)
            routes.forEach { addRoute(builder, it) }
            if (profile.enableIpv6) addRoute(builder, Cidr("::", 0, true))
            VpnBus.info("Installed ${routes.size} routes so local networks bypass the tunnel")
        }
    }

    private fun addRoute(builder: VpnService.Builder, route: Cidr) {
        runCatching { builder.addRoute(route.address, route.prefixLength) }
            .onFailure {
                VpnBus.log(LogLevel.DEBUG, "Skipped route ${route.address}/${route.prefixLength}")
            }
    }

    private fun applyDns(builder: VpnService.Builder, ip: LibOpenConnect.IPInfo) {
        var added = 0
        for (server in ip.DNS.orEmpty()) {
            if (Net.isValidIp(server)) {
                runCatching { builder.addDnsServer(server) }.onSuccess { added++ }
            }
        }
        if (added == 0) VpnBus.log(LogLevel.DEBUG, "Gateway supplied no DNS servers")

        val domains = buildList {
            ip.domain?.split(' ', ',', '\t')?.let { addAll(it) }
            addAll(ip.splitDNS.orEmpty())
        }
        for (domain in domains.map { it.trim() }.filter { it.isNotEmpty() }.distinct()) {
            runCatching { builder.addSearchDomain(domain) }
        }
    }

    private fun applySplitTunnel(builder: VpnService.Builder) {
        val packages = settings.selectedPackages
        if (!settings.splitTunnelEnabled || packages.isEmpty()) {
            VpnBus.updateInfo { it.copy(excludedApps = 0) }
            return
        }

        var applied = 0
        val stale = mutableListOf<String>()
        for (packageName in packages) {
            try {
                when (settings.splitTunnelMode) {
                    SplitTunnelMode.EXCLUDE_SELECTED -> builder.addDisallowedApplication(packageName)
                    SplitTunnelMode.INCLUDE_SELECTED -> builder.addAllowedApplication(packageName)
                }
                applied++
            } catch (e: Exception) {
                stale += packageName
            }
        }
        if (stale.isNotEmpty()) {
            VpnBus.log(LogLevel.DEBUG, "Ignored uninstalled app(s): ${stale.joinToString()}")
        }
        VpnBus.updateInfo { it.copy(excludedApps = applied) }
        VpnBus.info(
            when (settings.splitTunnelMode) {
                SplitTunnelMode.EXCLUDE_SELECTED -> "$applied app(s) bypass the VPN"
                SplitTunnelMode.INCLUDE_SELECTED -> "Only $applied app(s) use the VPN"
            }
        )
    }

    private fun describe(lib: Session, ip: LibOpenConnect.IPInfo) = TunnelInfo(
        server = runCatching { lib.getHostname() }.getOrNull() ?: profile.server,
        ipv4 = ip.addr,
        ipv6 = ip.netmask6 ?: ip.addr6,
        dns = ip.DNS.orEmpty().toList(),
        domain = ip.domain,
        mtu = if (profile.mtu > 0) profile.mtu else ip.MTU,
        cstpCipher = runCatching { lib.getCSTPCipher() }.getOrNull(),
        dtlsCipher = runCatching { lib.getDTLSCipher() }.getOrNull(),
        cstpCompression = runCatching { lib.getCSTPCompression() }.getOrNull(),
        dtlsCompression = runCatching { lib.getDTLSCompression() }.getOrNull(),
        serverRoutes = ip.splitIncludes.orEmpty().toList(),
        excludedApps = VpnBus.status.value.info.excludedApps,
        certFingerprint = runCatching { lib.getPeerCertHash() }.getOrNull(),
        profileDisplayName = profile.displayName,
    )

    private fun closeTun() {
        val fd = tunFd ?: return
        tunFd = null
        try {
            fd.close()
        } catch (e: IOException) {
            // The library may already have closed it.
        }
    }

    // ── libopenconnect callbacks ────────────────────────────────────────────

    private inner class Callbacks : SessionCallbacks {

        override fun onProgress(level: Int, message: String?) {
            val text = message?.trim().orEmpty()
            if (text.isEmpty()) return
            VpnBus.log(
                when (level) {
                    LibOpenConnect.PRG_ERR -> LogLevel.ERROR
                    LibOpenConnect.PRG_INFO -> LogLevel.INFO
                    LibOpenConnect.PRG_DEBUG -> LogLevel.DEBUG
                    else -> LogLevel.TRACE
                },
                text,
            )
        }

        override fun onProtectSocket(fd: Int) {
            if (!host.protectSocket(fd)) {
                VpnBus.error("VpnService.protect() failed — the tunnel socket may loop back on itself")
            }
        }

        override fun onStatsUpdate(stats: LibOpenConnect.VPNStats?) {
            stats ?: return
            VpnBus.updateStats(
                rxBytes = stats.rxBytes,
                txBytes = stats.txBytes,
                rxPackets = stats.rxPkts,
                txPackets = stats.txPkts,
                nowElapsed = SystemClock.elapsedRealtime(),
            )
        }

        override fun onReconnected() {
            VpnBus.info("Reconnected")
            val current = VpnBus.status.value
            VpnBus.setConnected(
                current.connectedAtElapsed.takeIf { it > 0L } ?: SystemClock.elapsedRealtime(),
                current.info,
            )
        }

        /** libopenconnect wants a (new) tun device mid-session. */
        override fun onSetupTun() {
            val lib = session ?: return
            VpnBus.setStage(ConnectionStage.RECONNECTING)
            val ip = lib.getIPInfo() ?: return
            closeTun()
            val descriptor = establishTun(ip)
            if (descriptor == null) {
                VpnBus.error("Could not re-create the tunnel interface")
                return
            }
            tunFd = descriptor
            if (lib.setupTunFD(descriptor.fd) != 0) {
                VpnBus.error("Could not re-attach the tunnel interface")
                return
            }
            VpnBus.setConnected(SystemClock.elapsedRealtime(), describe(lib, ip))
        }

        override fun onValidatePeerCert(reason: String?): Int {
            val lib = session ?: return -1
            val fingerprint = runCatching { lib.getPeerCertHash() }.getOrNull().orEmpty()

            val pinned = profile.trustedCertificate
            if (pinned.isNotBlank() &&
                runCatching { lib.checkPeerCertHash(pinned) }.getOrDefault(-1) == 0
            ) {
                VpnBus.log(LogLevel.DEBUG, "Server certificate matches the pin saved for this profile")
                return 0
            }

            val prompt = UserPrompt.CertTrust(
                id = Interaction.nextId(),
                host = runCatching { lib.getHostname() }.getOrNull() ?: profile.server,
                reason = reason?.trim().orEmpty().ifEmpty { "The certificate could not be verified." },
                fingerprint = fingerprint,
                details = runCatching { lib.getPeerCertDetails() }.getOrNull().orEmpty(),
            )
            VpnBus.error("Server certificate needs review: ${prompt.reason}")

            return if (Interaction.await(prompt) is PromptResult.Accept) {
                if (fingerprint.isNotBlank()) host.persistCertificatePin(fingerprint)
                VpnBus.info("Server certificate accepted and pinned for this profile")
                0
            } else {
                userCancelled.set(true)
                VpnBus.error("Server certificate rejected")
                -1
            }
        }

        override fun onProcessAuthForm(form: LibOpenConnect.AuthForm?): Int {
            form ?: return LibOpenConnect.OC_FORM_RESULT_ERR

            form.banner?.trim()?.takeIf { it.isNotEmpty() }?.let { VpnBus.info(it) }
            form.error?.trim()?.takeIf { it.isNotEmpty() }?.let { VpnBus.error(it) }

            // Gateways that offer tunnel groups send a SELECT first. If the
            // profile names one, apply it and ask for the form again.
            val groupOpt = form.authgroupOpt
            if (groupOpt != null && profile.authGroup.isNotBlank() && !authGroupApplied) {
                val match = matchChoice(groupOpt.choices, profile.authGroup)
                if (match != null) {
                    authGroupApplied = true
                    if (groupOpt.value != match.name) {
                        groupOpt.value = match.name
                        VpnBus.info("Selected authentication group “${match.label ?: match.name}”")
                        return LibOpenConnect.OC_FORM_RESULT_NEWGROUP
                    }
                }
            }

            val unfilled = mutableListOf<LibOpenConnect.FormOpt>()

            for (opt in form.opts.orEmpty()) {
                if (opt.flags and LibOpenConnect.OC_FORM_OPT_IGNORE.toLong() != 0L) continue
                when (opt.type) {
                    LibOpenConnect.OC_FORM_OPT_TEXT ->
                        if (profile.username.isNotBlank() && looksLikeUsername(opt)) {
                            opt.value = profile.username
                        } else {
                            unfilled += opt
                        }

                    LibOpenConnect.OC_FORM_OPT_PASSWORD ->
                        if (password.isNotEmpty() && !passwordConsumed && !looksLikeToken(opt)) {
                            opt.value = password
                            passwordConsumed = true
                        } else {
                            unfilled += opt
                        }

                    LibOpenConnect.OC_FORM_OPT_SELECT -> {
                        val choices = opt.choices.orEmpty()
                        val match = profile.authGroup.takeIf { it.isNotBlank() }
                            ?.let { matchChoice(opt.choices, it) }
                        when {
                            match != null -> opt.value = match.name
                            choices.size == 1 -> opt.value = choices[0].name
                            else -> unfilled += opt
                        }
                    }
                    // HIDDEN options already carry their value; TOKEN options
                    // belong to software tokens this app does not configure.
                    else -> Unit
                }
            }

            if (unfilled.isEmpty()) return LibOpenConnect.OC_FORM_RESULT_OK
            if (disconnectRequested.get()) return LibOpenConnect.OC_FORM_RESULT_CANCELLED

            val prompt = UserPrompt.Auth(
                id = Interaction.nextId(),
                title = form.authID?.trim()?.takeIf { it.isNotEmpty() } ?: "Sign in",
                banner = form.banner?.trim()?.takeIf { it.isNotEmpty() },
                message = form.message?.trim()?.takeIf { it.isNotEmpty() },
                error = form.error?.trim()?.takeIf { it.isNotEmpty() },
                fields = unfilled.map { opt -> toPromptField(opt) },
            )

            return when (val result = Interaction.await(prompt)) {
                is PromptResult.Values -> {
                    for (opt in unfilled) {
                        val value = result.values[opt.name] ?: continue
                        opt.value = value
                        if (opt.type == LibOpenConnect.OC_FORM_OPT_PASSWORD) {
                            password = value
                            passwordConsumed = true
                        }
                    }
                    LibOpenConnect.OC_FORM_RESULT_OK
                }

                else -> {
                    userCancelled.set(true)
                    LibOpenConnect.OC_FORM_RESULT_CANCELLED
                }
            }
        }

        private fun toPromptField(opt: LibOpenConnect.FormOpt) = PromptField(
            name = opt.name.orEmpty(),
            label = opt.label?.trim()?.removeSuffix(":")?.takeIf { it.isNotEmpty() }
                ?: opt.name.orEmpty(),
            type = when (opt.type) {
                LibOpenConnect.OC_FORM_OPT_PASSWORD -> PromptFieldType.PASSWORD
                LibOpenConnect.OC_FORM_OPT_SELECT -> PromptFieldType.SELECT
                else -> PromptFieldType.TEXT
            },
            prefill = if (opt.type == LibOpenConnect.OC_FORM_OPT_TEXT && looksLikeUsername(opt)) {
                profile.username
            } else {
                ""
            },
            choices = opt.choices.orEmpty().map { PromptChoice(it.name.orEmpty(), it.label ?: it.name.orEmpty()) },
        )

        private fun matchChoice(
            choices: List<LibOpenConnect.FormChoice>?,
            wanted: String,
        ): LibOpenConnect.FormChoice? = choices.orEmpty().firstOrNull {
            it.name.equals(wanted, ignoreCase = true) || it.label.equals(wanted, ignoreCase = true)
        }

        private fun looksLikeUsername(opt: LibOpenConnect.FormOpt): Boolean {
            val name = opt.name.orEmpty().lowercase()
            val label = opt.label.orEmpty().lowercase()
            return name.contains("user") || name.contains("uname") || name.contains("login") ||
                name == "id" || label.contains("user") || label.contains("login") ||
                label.contains("email")
        }

        /**
         * A second PASSWORD field is usually a one-time code, and shovelling the
         * saved password into it would burn the login attempt.
         */
        private fun looksLikeToken(opt: LibOpenConnect.FormOpt): Boolean {
            val text = (opt.name.orEmpty() + " " + opt.label.orEmpty()).lowercase()
            return text.contains("token") || text.contains("otp") ||
                text.contains("one-time") || text.contains("secondary") ||
                text.contains("verification") || text.contains("passcode") ||
                text.contains("second password")
        }
    }

    private val defaultUserAgent: String
        get() = "AnyConnect Android 4.10.05065"

    private companion object {
        const val DTLS_ATTEMPT_SECONDS = 60
        const val RECONNECT_TIMEOUT_SECONDS = 300
        const val MIN_MTU = 1280
        const val DEFAULT_MTU = 1350

        /** Opaque, stable-per-build device id for the AnyConnect mobile header. */
        const val DEVICE_ID = "0123456789ABCDEF0123456789ABCDEF01234567"
    }
}
