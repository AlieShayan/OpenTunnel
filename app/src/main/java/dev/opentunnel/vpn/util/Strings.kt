package dev.opentunnel.vpn.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import dev.opentunnel.vpn.data.AppLanguage

/**
 * Localized string provider supporting English and Persian (Farsi) with RTL/LTR helpers.
 */
object Strings {

    /** Wraps a LTR technical term with Unicode LTR marks (\u200E) to prevent line flips in RTL text. */
    fun ltr(text: String): String = "\u200E$text\u200E"

    fun isRtl(language: AppLanguage): Boolean = language == AppLanguage.PERSIAN

    // ── Screen Titles & Navigation ──────────────────────────────────────────
    fun appName(): String = "OpenTunnel"
    fun homeTitle(lang: AppLanguage): String = "OpenTunnel"
    fun profileManagementTitle(lang: AppLanguage): String = if (isRtl(lang)) "مدیریت پروفایل‌ها" else "Profile Management"
    fun editProfileTitle(lang: AppLanguage, name: String): String =
        if (isRtl(lang)) {
            if (name.isNotBlank()) "ویرایش «$name»" else "پروفایل جدید"
        } else {
            if (name.isNotBlank()) "Editing \"$name\"" else "New profile"
        }
    fun splitTunnelTitle(lang: AppLanguage): String = if (isRtl(lang)) "اسپلیت تانلینگ برنامه" else "App Split Tunneling"
    fun logsTitle(lang: AppLanguage): String = if (isRtl(lang)) "لاگ‌های اتصال" else "Connection log"
    fun settingsTitle(lang: AppLanguage): String = if (isRtl(lang)) "تنظیمات" else "Settings"

    // ── HomeScreen ───────────────────────────────────────────────────────────
    fun notConnected(lang: AppLanguage): String = if (isRtl(lang)) "متصل نیست" else "Not connected"
    fun connecting(lang: AppLanguage): String = if (isRtl(lang)) "در حال اتصال…" else "Connecting…"
    fun authenticating(lang: AppLanguage): String = if (isRtl(lang)) "در حال احراز هویت…" else "Authenticating…"
    fun preparing(lang: AppLanguage): String = if (isRtl(lang)) "در حال آماده‌سازی…" else "Preparing…"
    fun disconnecting(lang: AppLanguage): String = if (isRtl(lang)) "در حال قطع اتصال…" else "Disconnecting…"
    fun reconnecting(lang: AppLanguage): String = if (isRtl(lang)) "در حال اتصال مجدد…" else "Reconnecting…"
    fun connected(lang: AppLanguage): String = if (isRtl(lang)) "اتصال امن برقرار است" else "Secure tunnel active"
    fun connectionFailed(lang: AppLanguage): String = if (isRtl(lang)) "خطا در اتصال" else "Connection failed"
    fun downloaded(lang: AppLanguage): String = if (isRtl(lang)) "دریافتی (دانلود)" else "Downloaded"
    fun uploaded(lang: AppLanguage): String = if (isRtl(lang)) "ارسالی (آپلود)" else "Uploaded"
    fun connectionDetails(lang: AppLanguage): String = if (isRtl(lang)) "جزئیات اتصال" else "Connection"
    fun ipv4Address(lang: AppLanguage): String = if (isRtl(lang)) "آدرس ${ltr("IPv4")}" else "IPv4 address"
    fun ipv6Address(lang: AppLanguage): String = if (isRtl(lang)) "آدرس ${ltr("IPv6")}" else "IPv6 address"
    fun dnsServers(lang: AppLanguage): String = if (isRtl(lang)) "سرورهای ${ltr("DNS")}" else "DNS"
    fun searchDomain(lang: AppLanguage): String = if (isRtl(lang)) "دامنه جستجو" else "Search domain"
    fun mtuLabel(lang: AppLanguage): String = if (isRtl(lang)) "مقدار ${ltr("MTU")}" else "MTU"
    fun tlsChannel(lang: AppLanguage): String = if (isRtl(lang)) "کانال ${ltr("TLS")}" else "TLS channel"
    fun dtlsChannel(lang: AppLanguage): String = if (isRtl(lang)) "کانال ${ltr("DTLS")}" else "DTLS channel"
    fun locationLabel(lang: AppLanguage): String = if (isRtl(lang)) "موقعیت خروجی" else "Location"
    fun pingLabel(lang: AppLanguage): String = if (isRtl(lang)) "پینگ" else "Ping"
    fun logsSubtitle(lang: AppLanguage): String = if (isRtl(lang)) "گزارش زنده تمام رویدادهای ${ltr("openconnect")}" else "Everything openconnect reports, live"
    fun settingsSubtitle(lang: AppLanguage): String = if (isRtl(lang)) "ظاهر، اتصال مجدد و عیب‌یابی" else "Appearance, reconnection, diagnostics"

    // ── Connect Orb Labels ──────────────────────────────────────────────────
    fun orbConnect(lang: AppLanguage): String = if (isRtl(lang)) "اتصال" else "CONNECT"
    fun orbConnected(lang: AppLanguage): String = if (isRtl(lang)) "متصل" else "CONNECTED"
    fun orbRetry(lang: AppLanguage): String = if (isRtl(lang)) "تلاش مجدد" else "RETRY"
    fun orbStopping(lang: AppLanguage): String = if (isRtl(lang)) "توقف" else "STOPPING"
    fun orbCancel(lang: AppLanguage): String = if (isRtl(lang)) "لغو" else "CANCEL"

    // ── Profile Management ──────────────────────────────────────────────────
    fun addProfile(lang: AppLanguage): String = if (isRtl(lang)) "افزودن پروفایل" else "Add profile"
    fun activeProfileBadge(lang: AppLanguage): String = if (isRtl(lang)) "فعال" else "ACTIVE"
    fun noProfilesYet(lang: AppLanguage): String = if (isRtl(lang)) "هیچ پروفایلی ثبت نشده است" else "No profiles yet"
    fun tapToAddProfile(lang: AppLanguage): String =
        if (isRtl(lang)) "برای افزودن پروفایل ${ltr("VPN")} دکمه + را لمس کنید" else "Tap + to add a VPN profile with any gateway"
    fun exportProfiles(lang: AppLanguage): String = if (isRtl(lang)) "پشتیبان‌گیری (خروجی JSON)" else "Export Profiles"
    fun importProfiles(lang: AppLanguage): String = if (isRtl(lang)) "بازیابی (ورودی JSON)" else "Import Profiles"
    fun deleteProfileConfirm(lang: AppLanguage, name: String): String =
        if (isRtl(lang)) "آیا از حذف پروفایل «$name» اطمینان دارید؟" else "Are you sure you want to delete profile \"$name\"?"
    fun delete(lang: AppLanguage): String = if (isRtl(lang)) "حذف" else "Delete"
    fun cancel(lang: AppLanguage): String = if (isRtl(lang)) "لغو" else "Cancel"
    fun save(lang: AppLanguage): String = if (isRtl(lang)) "ذخیره" else "Save"

    // ── ProfileScreen Fields ─────────────────────────────────────────────────
    fun serverSection(lang: AppLanguage): String = if (isRtl(lang)) "مشخصات سرور" else "Server"
    fun profileName(lang: AppLanguage): String = if (isRtl(lang)) "نام پروفایل" else "Profile name"
    fun serverAddress(lang: AppLanguage): String = if (isRtl(lang)) "آدرس سرور (گیت‌وی)" else "Server address"
    fun caCertificate(lang: AppLanguage): String = if (isRtl(lang)) "گواهی امنیتی ${ltr("CA")}" else "CA certificate"
    fun userCertificate(lang: AppLanguage): String = if (isRtl(lang)) "گواهی کاربر" else "User certificate"
    fun privateKey(lang: AppLanguage): String = if (isRtl(lang)) "کلید خصوصی (${ltr("Private key")})" else "Private key"
    fun softwareToken(lang: AppLanguage): String = if (isRtl(lang)) "توکن نرم‌افزاری" else "Software token"
    fun tokenString(lang: AppLanguage): String = if (isRtl(lang)) "رشته توکن (${ltr("Token string")})" else "Token string"
    fun username(lang: AppLanguage): String = if (isRtl(lang)) "نام کاربری" else "Username"
    fun password(lang: AppLanguage): String = if (isRtl(lang)) "رمز عبور" else "Password"
    fun authGroup(lang: AppLanguage): String = if (isRtl(lang)) "گروه احراز هویت (اختیاری)" else "Auth group (optional)"
    fun disableCredentialCaching(lang: AppLanguage): String = if (isRtl(lang)) "ذخیره نکردن مشخصات ورود" else "Disable credential caching"
    fun disableCredentialCachingSub(lang: AppLanguage): String =
        if (isRtl(lang)) "عدم ذخیره‌سازی نام کاربری، گروه یا رمز عبور" else "Never cache login names, user groups, or passwords"
    fun clearSavedPasswords(lang: AppLanguage): String = if (isRtl(lang)) "پاک کردن رمز عبور ذخیره‌شده" else "Clear saved passwords"
    fun advancedOptions(lang: AppLanguage): String = if (isRtl(lang)) "تنظیمات پیشرفته" else "Advanced options"
    fun hideAdvancedOptions(lang: AppLanguage): String = if (isRtl(lang)) "پنهان‌سازی تنظیمات پیشرفته" else "Hide advanced options"
    fun batchMode(lang: AppLanguage): String = if (isRtl(lang)) "حالت ${ltr("Batch mode")}" else "Batch mode"
    fun reportedOs(lang: AppLanguage): String = if (isRtl(lang)) "سیستم‌عامل گزارش‌شده (${ltr("Reported OS")})" else "Reported OS"
    fun csdWrapper(lang: AppLanguage): String = if (isRtl(lang)) "اسکریپت سفارشی ${ltr("CSD wrapper")}" else "Custom CSD wrapper"
    fun splitTunnelMode(lang: AppLanguage): String = if (isRtl(lang)) "حالت تونل‌سازی جداگانه" else "Split tunnel mode"
    fun splitTunnelNetworks(lang: AppLanguage): String = if (isRtl(lang)) "شبکه‌های تونل‌سازی جداگانه" else "Split tunnel networks"
    fun disableXmlPost(lang: AppLanguage): String = if (isRtl(lang)) "غیرفعال‌سازی ${ltr("XML POST")}" else "Disable XML POST"
    fun disableXmlPostSub(lang: AppLanguage): String =
        if (isRtl(lang)) "استفاده از پروتکل قدیمی احراز هویت" else "Use the old authentication handshake; may fail on newer servers"
    fun requirePfs(lang: AppLanguage): String = if (isRtl(lang)) "الزامی بودن ${ltr("PFS")}" else "Require PFS"
    fun requirePfsSub(lang: AppLanguage): String =
        if (isRtl(lang)) "استفاده انحصاری از الگوریتم‌های ${ltr("Perfect Forward Secrecy")}" else "Only negotiate cipher suites with Perfect Forward Secrecy"
    fun overrideDpdTimeout(lang: AppLanguage): String = if (isRtl(lang)) "تغییر مهلت ${ltr("DPD")}" else "Override DPD timeout"
    fun overrideDpdTimeoutSub(lang: AppLanguage): String =
        if (isRtl(lang)) "تعیین زمان اختصاصی تشخیص قطع ارتباط" else "Use a custom Dead Peer Detection timeout instead of the server default"
    fun dpdSeconds(lang: AppLanguage): String = if (isRtl(lang)) "مهلت ${ltr("DPD")} (ثانیه)" else "DPD timeout (seconds)"
    fun vpnProtocol(lang: AppLanguage): String = if (isRtl(lang)) "پروتکل ${ltr("VPN")}" else "VPN protocol"

    // ── Split Tunneling ─────────────────────────────────────────────────────
    fun splitTunnelHeaderTitle(lang: AppLanguage): String = if (isRtl(lang)) "مسیریابی مستقیم برنامه‌ها" else "Route apps around the VPN"
    fun splitTunnelDisabledSub(lang: AppLanguage): String = if (isRtl(lang)) "در حال حاضر تمام برنامه‌ها از تونل عبور می‌کنند" else "Every app currently goes through the tunnel"
    fun splitTunnelNoAppsSub(lang: AppLanguage): String = if (isRtl(lang)) "برنامه‌هایی که نباید از تونل عبور کنند را انتخاب کنید" else "Pick the apps to leave outside the tunnel"
    fun splitTunnelExcludeCountSub(lang: AppLanguage, count: Int): String =
        if (isRtl(lang)) "$count برنامه مستقیم به اینترنت متصل می‌شوند" else "$count app(s) will use your normal connection"
    fun splitTunnelIncludeCountSub(lang: AppLanguage, count: Int): String =
        if (isRtl(lang)) "تنها $count برنامه از تونل استفاده خواهند کرد" else "Only $count app(s) will use the tunnel"
    fun splitTunnelNotice(lang: AppLanguage): String = if (isRtl(lang)) "تغییرات در اتصال بعدی تونل اعمال می‌شوند." else "Changes apply the next time the tunnel connects."
    fun splitTunnelBypassMode(lang: AppLanguage): String = if (isRtl(lang)) "عدم عبور برنامه‌های انتخاب‌شده" else "Selected apps bypass VPN"
    fun splitTunnelOnlyMode(lang: AppLanguage): String = if (isRtl(lang)) "فقط برنامه‌های انتخاب‌شده" else "Only selected use VPN"
    fun splitTunnelSearchPlaceholder(lang: AppLanguage): String = if (isRtl(lang)) "جستجوی برنامه‌ها…" else "Search apps"
    fun splitTunnelClear(lang: AppLanguage): String = if (isRtl(lang)) "پاک‌سازی" else "Clear"
    fun splitTunnelFilterAll(lang: AppLanguage): String = if (isRtl(lang)) "همه" else "All"
    fun splitTunnelFilterSelected(lang: AppLanguage): String = if (isRtl(lang)) "انتخاب‌شده" else "Selected"
    fun splitTunnelFilterInstalled(lang: AppLanguage): String = if (isRtl(lang)) "نصب‌شده" else "Installed"
    fun splitTunnelFilterSystem(lang: AppLanguage): String = if (isRtl(lang)) "سیستم" else "System"
    fun splitTunnelEmptyDisabled(lang: AppLanguage): String =
        if (isRtl(lang)) "برای تعیین برنامه‌های خارج از ${ltr("VPN")}، تونل‌سازی جداگانه را روشن کنید." else "Turn split tunnelling on to choose which apps skip the VPN."
    fun splitTunnelNoMatches(lang: AppLanguage, query: String): String =
        if (isRtl(lang)) "هیچ برنامه‌ای با «$query» مطابقت نداشت." else "No apps match \"$query\"."

    // ── SettingsScreen ───────────────────────────────────────────────────────
    fun appearance(lang: AppLanguage): String = if (isRtl(lang)) "ظاهر و پوسته" else "Appearance"
    fun theme(lang: AppLanguage): String = if (isRtl(lang)) "پوسته برنامه" else "Theme"
    fun themeSystem(lang: AppLanguage): String = if (isRtl(lang)) "سیستم" else "System"
    fun themeDark(lang: AppLanguage): String = if (isRtl(lang)) "تاریک" else "Dark"
    fun themeLight(lang: AppLanguage): String = if (isRtl(lang)) "روشن" else "Light"
    fun appLanguageLabel(lang: AppLanguage): String = if (isRtl(lang)) "زبان برنامه" else "App Language"
    fun langSystem(lang: AppLanguage): String = if (isRtl(lang)) "پیش‌فرض سیستم" else "System Default"
    fun langEnglish(lang: AppLanguage): String = if (isRtl(lang)) "English (انگلیسی)" else "English"
    fun langPersian(lang: AppLanguage): String = if (isRtl(lang)) "فارسی (Persian)" else "فارسی (Persian)"
    fun dynamicColorTitle(lang: AppLanguage): String = if (isRtl(lang)) "رنگ‌بندی پوسته سیستم" else "Wallpaper colours"
    fun dynamicColorSub(lang: AppLanguage): String =
        if (isRtl(lang)) "هماهنگ‌سازی رنگ‌های برنامه با تصویر پس‌زمینه (${ltr("Material You")})" else "Tint the app with your Material You palette"
    fun tunnelBehaviour(lang: AppLanguage): String = if (isRtl(lang)) "رفتار تونل" else "Tunnel behaviour"
    fun bypassLocal(lang: AppLanguage): String = if (isRtl(lang)) "عدم عبور ترافیک شبکه محلی از ${ltr("VPN")}" else "Keep local network off the VPN"
    fun bypassLocalSub(lang: AppLanguage): String =
        if (isRtl(lang)) "پرینترها و دستگاه‌های شبکه محلی متصل می‌مانند" else "Printers, NAS and casting keep working while connected"
    fun reconnectNetwork(lang: AppLanguage): String = if (isRtl(lang)) "اتصال مجدد هنگام تغییر شبکه" else "Reconnect on network change"
    fun reconnectNetworkSub(lang: AppLanguage): String =
        if (isRtl(lang)) "اتصال مجدد هنگام جابجایی بین ${ltr("Wi-Fi")} و دیتای موبایل" else "Re-establish the tunnel when moving between Wi-Fi and mobile data"
    fun connectOnBoot(lang: AppLanguage): String = if (isRtl(lang)) "اتصال خودکار پس از روشن شدن دستگاه" else "Connect after restart"
    fun connectOnBootSub(lang: AppLanguage): String =
        if (isRtl(lang)) "نیازمند اعطای حداقل یک‌بار مجوز ${ltr("VPN")} است" else "Needs VPN permission to have been granted at least once"
    fun systemSection(lang: AppLanguage): String = if (isRtl(lang)) "سیستم" else "System"
    fun alwaysOnVpn(lang: AppLanguage): String = if (isRtl(lang)) "${ltr("VPN")} همیشه روشن" else "Always-on VPN"
    fun alwaysOnVpnSub(lang: AppLanguage): String =
        if (isRtl(lang)) "باز کردن تنظیمات ${ltr("VPN")} اندروید برای فعال‌سازی اتصال همیشگی" else "Open Android's VPN settings to make this the always-on VPN and block traffic when it drops"
    fun statsNotification(lang: AppLanguage): String = if (isRtl(lang)) "نمایش میزان ترافیک در اعلان" else "Traffic counters in the notification"
    fun statsNotificationSub(lang: AppLanguage): String =
        if (isRtl(lang)) "نمایش مجموع ترافیک ارسالی و دریافتی در اعلان" else "Show total up/down in the ongoing notification"
    fun hapticFeedbackTitle(lang: AppLanguage): String = if (isRtl(lang)) "بازخورد لمسی (ویبره)" else "Haptic feedback"
    fun hapticFeedbackSub(lang: AppLanguage): String =
        if (isRtl(lang)) "ارائه ویبره هنگام اسکرول لیست‌ها، تغییر وضعیت اتصال و تاگل‌های اصلی" else "Vibrate on list scrolling, connection status change, and main toggles"
    fun diagnostics(lang: AppLanguage): String = if (isRtl(lang)) "عیب‌یابی و لاگ" else "Diagnostics"
    fun verboseLogging(lang: AppLanguage): String = if (isRtl(lang)) "ثبت لاگ‌های تفصیلی (${ltr("Verbose")})" else "Verbose logging"
    fun verboseLoggingSub(lang: AppLanguage): String =
        if (isRtl(lang)) "شامل کردن لاگ‌های دیباگ ${ltr("openconnect")} در لاگ اتصال" else "Include openconnect debug output in the connection log"
    fun about(lang: AppLanguage): String = if (isRtl(lang)) "درباره برنامه" else "About"

    // ── Additional UI & File Picker Strings ───────────────────────────────────
    fun selectFile(lang: AppLanguage): String = if (isRtl(lang)) "انتخاب فایل" else "Select file"
    fun selectProfileTitle(lang: AppLanguage): String = if (isRtl(lang)) "انتخاب پروفایل اتصال" else "Select VPN Profile"
    fun manageProfilesAction(lang: AppLanguage): String = if (isRtl(lang)) "مدیریت پروفایل‌ها…" else "Manage profiles…"
    fun selectAllApps(lang: AppLanguage): String = if (isRtl(lang)) "انتخاب همه" else "Select all"
    fun deselectAllApps(lang: AppLanguage): String = if (isRtl(lang)) "لغو همه" else "Deselect all"
    fun logLevelAll(lang: AppLanguage): String = if (isRtl(lang)) "همه" else "All"
    fun logLevelError(lang: AppLanguage): String = if (isRtl(lang)) "خطاها" else "Errors"
    fun logLevelInfo(lang: AppLanguage): String = if (isRtl(lang)) "اطلاعات" else "Info"
    fun logLevelApp(lang: AppLanguage): String = if (isRtl(lang)) "برنامه" else "App"
    fun autoScrollLabel(lang: AppLanguage): String = if (isRtl(lang)) "پیمایش خودکار" else "Auto-scroll"

    // ── HomeScreen — previously hardcoded strings ────────────────────────────
    /** Shown in the profile row when the profile has no server/username yet. */
    fun tapToSetupProfile(lang: AppLanguage): String =
        if (isRtl(lang)) "برای افزودن سرور، نام کاربری و رمز عبور لمس کنید" else "Tap to add your server, username and password"

    /** Shown in ConnectionDetails when DTLS is not active. */
    fun dtlsNotEstablished(lang: AppLanguage): String =
        if (isRtl(lang)) "برقرار نشده (فقط ${ltr("TLS")})" else "not established (TLS only)"

    /** Label for the gateway-pushed routes row in ConnectionDetails. */
    fun gatewayRoutes(lang: AppLanguage): String =
        if (isRtl(lang)) "مسیرهای گیت‌وی" else "Gateway routes"

    /** Label for the apps-outside-tunnel count in ConnectionDetails. */
    fun appsOutsideTunnel(lang: AppLanguage): String =
        if (isRtl(lang)) "برنامه‌های خارج از تونل" else "Apps outside the tunnel"

    /** Summary shown on HomeScreen when app split-tunnel is disabled. */
    fun splitTunnelOffSummary(lang: AppLanguage): String =
        if (isRtl(lang)) "خاموش — تمام برنامه‌ها از ${ltr("VPN")} استفاده می‌کنند" else "Off — every app uses the VPN"

    /** Summary shown on HomeScreen when app split-tunnel is on but no apps selected. */
    fun splitTunnelNoAppsSelectedSummary(lang: AppLanguage): String =
        if (isRtl(lang)) "روشن، اما هیچ برنامه‌ای انتخاب نشده" else "On, but no apps selected yet"

    /** Summary shown on HomeScreen when network split-tunnel is disabled. */
    fun splitTunnelNetworksOffSummary(lang: AppLanguage): String =
        if (isRtl(lang)) "خاموش — همه ترافیک از ${ltr("VPN")} عبور می‌کند" else "Off — all networks route through VPN"

    /** Summary shown on HomeScreen when network split-tunnel is on but no networks added. */
    fun splitTunnelNetworksNoEntriesSummary(lang: AppLanguage): String =
        if (isRtl(lang)) "روشن، اما هیچ شبکه یا سایتی اضافه نشده" else "On, but no networks or sites added"

    // ── Service Notification Prompts ─────────────────────────────────────────
    /** Notification body when the gateway asks for extra credentials at runtime. */
    fun promptAuthNotification(lang: AppLanguage): String =
        if (isRtl(lang)) "درگاه ${ltr("VPN")} اطلاعات ورود بیشتری می‌خواهد." else "The VPN gateway is asking for more sign-in details."

    /** Notification body when the gateway certificate needs user review. */
    fun promptCertTrustNotification(lang: AppLanguage): String =
        if (isRtl(lang)) "گواهی درگاه باید قبل از اتصال بررسی شود." else "The gateway's certificate needs to be reviewed before connecting."
}
