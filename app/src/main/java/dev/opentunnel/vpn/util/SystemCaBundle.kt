package dev.opentunnel.vpn.util

import android.content.Context
import android.util.Base64
import java.io.File
import java.security.KeyStore
import java.security.cert.X509Certificate
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

/**
 * OpenSSL on Android has no usable default CA path, and `/system/etc/security/
 * cacerts` is a directory of hashed files rather than the single bundle
 * `SSL_CTX_load_verify_locations()` wants.
 *
 * Rather than shipping a Mozilla bundle that slowly goes stale, this exports
 * the device's *live* trust anchors — including any CA the user or their
 * employer installed — into a PEM file for openconnect to point at.
 */
object SystemCaBundle {

    private const val FILE_NAME = "system-ca-bundle.pem"
    private const val MAX_AGE_MS = 24L * 60 * 60 * 1000

    /** Returns the bundle path, or null if the trust store could not be read. */
    fun ensure(context: Context): String? {
        val file = File(context.filesDir, FILE_NAME)
        val fresh = file.exists() &&
            file.length() > 0 &&
            System.currentTimeMillis() - file.lastModified() < MAX_AGE_MS
        if (fresh) return file.absolutePath

        val anchors = runCatching { trustAnchors() }.getOrNull()
        if (anchors.isNullOrEmpty()) return file.takeIf { it.exists() }?.absolutePath

        return runCatching {
            val tmp = File(context.filesDir, "$FILE_NAME.tmp")
            tmp.bufferedWriter().use { out ->
                anchors.forEach { cert ->
                    out.write("# ")
                    out.write(cert.subjectX500Principal.name)
                    out.write("\n-----BEGIN CERTIFICATE-----\n")
                    Base64.encodeToString(cert.encoded, Base64.NO_WRAP)
                        .chunked(64)
                        .forEach { line -> out.write(line); out.write("\n") }
                    out.write("-----END CERTIFICATE-----\n")
                }
            }
            if (!tmp.renameTo(file)) {
                tmp.copyTo(file, overwrite = true)
                tmp.delete()
            }
            file.absolutePath
        }.getOrNull()
    }

    private fun trustAnchors(): List<X509Certificate> {
        val factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        factory.init(null as KeyStore?)
        return factory.trustManagers
            .filterIsInstance<X509TrustManager>()
            .flatMap { it.acceptedIssuers.toList() }
            .distinctBy { it.subjectX500Principal.name + it.serialNumber }
    }
}
