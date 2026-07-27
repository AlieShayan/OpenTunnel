package dev.opentunnel.vpn.core

import org.infradead.libopenconnect.LibOpenConnect

/**
 * Loads libopenconnect.so exactly once and remembers whether it worked.
 *
 * [LibOpenConnect][org.infradead.libopenconnect.LibOpenConnect] runs
 * `globalInit()` from a static initialiser, so the shared object has to be in
 * the process *before* that class is touched. Everything that references it
 * goes through [isAvailable] first.
 */
object NativeLibrary {

    private const val LIB_NAME = "openconnect"

    @Volatile
    private var loaded: Boolean? = null

    @Volatile
    var loadError: String? = null
        private set

    val isAvailable: Boolean
        get() = loaded ?: synchronized(this) {
            loaded ?: runCatching {
                System.loadLibrary(LIB_NAME)
                true
            }.getOrElse { throwable ->
                loadError = throwable.message ?: throwable.toString()
                false
            }.also { loaded = it }
        }

    /**
     * Version string reported by the bundled library, or null if unavailable.
     *
     * Touching [LibOpenConnect] runs its static initialiser (a native call), so
     * the [isAvailable] check has to come first — an import alone is harmless
     * because class loading is lazy.
     */
    fun version(): String? =
        if (!isAvailable) null
        else runCatching { LibOpenConnect.getVersion() }.getOrNull()

    const val MISSING_MESSAGE: String =
        "The openconnect native library is not bundled in this build. " +
            "Run ./gradlew :app:buildNativeLibs (see native/README.md) and reinstall."
}
