package dev.opentunnel.vpn.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.Collator

@Immutable
data class InstalledApp(
    val packageName: String,
    val label: String,
    val isSystem: Boolean,
)

/**
 * Lists the apps that can actually use the network — those are the only ones
 * worth showing in a split-tunnel picker.
 */
object InstalledApps {

    private const val ICON_PX = 128
    private val iconCache = LruCache<String, ImageBitmap>(220)

    private var cachedList: List<InstalledApp>? = null
    private var lastLoadTime = 0L
    private const val CACHE_TTL_MS = 5 * 60 * 1000L

    suspend fun load(context: Context, forceRefresh: Boolean = false): List<InstalledApp> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (!forceRefresh && cachedList != null && (now - lastLoadTime < CACHE_TTL_MS)) {
            return@withContext cachedList!!
        }

        val pm = context.packageManager
        val self = context.packageName
        val collator = Collator.getInstance()

        val packages = runCatching {
            pm.getInstalledApplications(PackageManager.GET_META_DATA)
        }.getOrElse { emptyList() }

        val result = packages.asSequence()
            .filter { it.packageName != self }
            .filter { hasInternet(pm, it.packageName) }
            .map { info ->
                InstalledApp(
                    packageName = info.packageName,
                    label = runCatching { pm.getApplicationLabel(info).toString() }
                        .getOrDefault(info.packageName),
                    isSystem = (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0 &&
                        (info.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0,
                )
            }
            .sortedWith(compareBy(collator) { it.label })
            .toList()

        cachedList = result
        lastLoadTime = now
        result
    }

    private fun hasInternet(pm: PackageManager, packageName: String): Boolean =
        runCatching {
            pm.checkPermission(android.Manifest.permission.INTERNET, packageName) ==
                PackageManager.PERMISSION_GRANTED
        }.getOrDefault(true)

    suspend fun icon(context: Context, packageName: String): ImageBitmap? {
        iconCache.get(packageName)?.let { return it }
        return withContext(Dispatchers.IO) {
            runCatching {
                val drawable = context.packageManager.getApplicationIcon(packageName)
                val bitmap = drawable.toBitmap(ICON_PX, ICON_PX, Bitmap.Config.ARGB_8888)
                bitmap.asImageBitmap().also { iconCache.put(packageName, it) }
            }.getOrNull()
        }
    }

    /** Best-effort label for a package that may since have been uninstalled. */
    fun labelFor(context: Context, packageName: String): String = runCatching {
        val pm = context.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
    }.getOrDefault(packageName)
}
