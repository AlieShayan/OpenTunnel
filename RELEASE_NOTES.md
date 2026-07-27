## 🇮🇷 فارسی (Persian)

### 🚀 تغییرات نسخه 3.1.2

- 🔒 **رفع خطای آپدیت (Signature Mismatch)**: الزام ساخت امضای Release در Gradle و جلوگیری از سوئیچ خودکار به کلید Debug برای یکسان ماندن امضای APK در به‌روزرسانی‌ها.
- 🌐 **حل مشکل عدم اتصال روی شبکه‌های Wi-Fi**: افزودن رزولوشن خودکار DNS-over-HTTPS (DoH از طریق Cloudflare `1.1.1.1` و Google `8.8.8.8`) هنگام شکست خوردن DNS شبکه وای‌فای.
- 🧹 **پاک‌سازی کامنت‌های پروژه**: ترجمه و یکدست‌سازی کامنت‌های خطی کاتلین و اسکریپت‌ها به زبان انگلیسی.

---

### 🚀 تغییرات نسخه 3.1.1

- 📐 **پشتیبانی کامل ویجت افقی (۴×۱) از ۴ ستون**:
  - **حذف محدودیت عرض:** حذف `maxResizeWidth` جهت کشیده شدن کامل ویجت ۴×۱ روی ۴ یا ۵ ستون در تمامی لانچرها.
  - **بازطراحی چیدمان:** تخصیص وزن انعطاف‌پذیر به عنوان سرور و کشور جهت جلوگیری از هم‌پوشانی متون روی دکمه قطع/وصل.
- 🔤 **ارتقای خوانایی و فونت‌های بزرگ‌تر در تمامی ویجت‌ها**:
  - افزایش اندازه فونت عناوین، وضعیت، آی‌پی و آمار ترافیک به ۱۱ تا ۱۴ اس‌پی و تایمر به ۱۶ اس‌پی در هر ۳ ویجت (۲×۲، ۳×۲ و ۴×۱).
- 📶 **رفع مشکل عدم اتصال روی هات‌اسپات (Wi-Fi Hotspot)**:
  - **برطرف شدن تایم‌اوت SSL Negotiation:** تنظیم خودکار MTU درخواستی به ۱۳۵۰ جهت جلوگیری از خرد شدن و افت بسته‌های حجیم TLS در tethering.
  - **تغییر User-Agent پیش‌فرض:** بهینه‌سازی به هدر رسمی AnyConnect جهت عبور از مسدودی DPI اپراتورها روی اتصالات هات‌اسپات.

---

### 🚀 تغییرات نسخه 3.1.0

- 📊 **افزوده شدن ۳ سایز مختلف برای ویجت‌ها (2x2, 3x2, 4x1)**:
  - **ویجت ۲×۲ (مربعی فشرده)**: مناسب پروژه‌های کوچک و صفحه‌اصلی فشرده.
  - **ویجت ۳×۲ (کارت کامل)**: نمایش کامل آی‌پای خروجی، پرچم و کشور، ترافیک زنده و خط اختصاصی پینگ (`⚡ ms`).
  - **ویجت ۴×۱ (نوار افقی Sleek Bar)**: چیدمان افقی شیک و کشیده جهت دسترسی سریع با یک کلیک.
- 🌐 **رفع کامل مشکل عدم اتصال روی شبکه‌های Wi-Fi**: اتصال سوکت‌های محافظت‌شده به اینترفیس فیزیکی فعال و حفظ SNI و هدر `Host` جهت جلوگیری از قطعی در شبکه وای‌فای.
- 🗺️ **رفع مشکل لوپ لوکیشن و نمایش سریع پرچم کشور**: استفاده از سرویس‌های امن HTTPS GeoIP (`ipwho.is` و `freeipapi.com`) و عدم لغو درخواست‌های در حال اجرا.
- 🔔 **ارتقای نمایش آمار ترافیک در نوار نوتیفیکیشن**: نمایش برجسته مقادیر دانلود (`↓`) و آپلود (`↑`) در سطر اول و بخش جزئیات نوتیفیکیشن بدون قطعی متون.
- 🏷️ **ارتقا به نسخه 3.1.0 و بهبود انتشار CI**: انتشار اختصاصی فایل‌های APK نهایی (Release) در صفحه GitHub Releases.

---

### 🚀 تغییرات نسخه 3.0.0

- 🇮🇷 **ترجمه کامل فارسی تمام بخش‌ها**: ترجمه کامل بخش «تونل‌سازی جداگانه»، وضعیت‌های اتصال ("در حال احراز هویت…"، "در حال آماده‌سازی…")، دکمه اتصال و تنظیمات به زبان فارسی با پشتیبانی راست‌چین (RTL).
- 📐 **هم‌اندازه‌سازی دکمه‌های زبان برنامه**: گزینه‌های «پیش‌فرض سیستم»، «English» و «فارسی» در تنظیمات اکنون عرض یکسان و چیدمان کاملاً متوازن دارند.
- 🌐 **رفع مشکل گیر کردن اتصال روی Wi-Fi**: پیش‌پردازش آدرس IPv4 سرور در لایه کاتلین همراه با حفظ SNI گواهی امنیتی، جهت برطرف کردن تعارضات DNS و هنگی در شبکه‌های وای‌فای dual-stack.
- ⏱️ **به‌روزرسانی ۱ ثانیه‌ای ثانیه‌شمار و ترافیک**: آپدیت زنده ثانیه‌شمار اتصال و میزان ترافیک ارسالی/دریافتی هر ۱ ثانیه در اپلیکیشن و ویجت.
- ⚡ **محاسبه و نمایش پینگ (Ping)**: محاسبه دقیق RTT روی چند اندپوینت معتبر و نمایش پینگ در جزئیات اتصال برنامه و خط آمار ویجت.
- 🗺️ **نمایش لوکیشن خروجی بدون اختلال**: استفاده از آدرس‌های امن HTTPS و سرویس‌های پشتیبان برای نمایش پرچم، نام کشور و شهر در برنامه و ویجت.

---

### 🚀 تغییرات نسخه 2.0.0

- 📂 **مدیریت چند پروفایل و پشتیبان‌گیری JSON**: امکان افزودن، ویرایش، پشتیبان‌گیری و خروجی/ورودی گرفتن از پروفایل‌های VPN.
- ⚙️ **تنظیمات پیشرفته OpenConnect**: پشتیبانی از گواهی‌های اختصاصی، توکن‌های نرم‌افزاری (stoken، TOTP، HOTP)، CSD wrapper، غیرفعال‌سازی XML POST و PFS.
- 🌐 **پشتیبانی دو زبانه و RTL**: سوییچ بین انگلیسی و فارسی همراه با فرمت‌بندی کلمات فنی LTR.
- 📊 **ویجت ۲×۲ با نمایش مشخصات لوکیشن و ترافیک**: نمایش IP خروجی، پرچم کشور و کنترلهای اتصال از روی صفحه اصلی.

---

## 🇬🇧 English

### 🚀 What's New in v3.1.2

- 🔒 **Enforced Release Build Signing**: Fixed APK update signature mismatch by requiring release signing keys and removing debug fallback.
- 🌐 **DNS-over-HTTPS (DoH) Fallback**: Added automatic DoH resolution (Cloudflare `1.1.1.1` & Google `8.8.8.8`) for hostnames when Wi-Fi DNS fails.
- 🧹 **Cleaned Codebase Documentation**: Replaced all Persian inline comments across Kotlin code and build scripts with standard English documentation.

---

### 🚀 What's New in v3.1.1

- 📊 **4x1 Widget Grid Expansion**: Removed `maxResizeWidth` restriction allowing full 4-column stretching across all Android launchers.
- 🔤 **Enhanced Typography**: Scaled font sizes for server names, IP addresses, ping, and status indicators across 2x2, 3x2, and 4x1 widgets.
- 📶 **Wi-Fi Hotspot & Tethering Fix**: Set requested MTU to 1350 and default User-Agent to official AnyConnect header to prevent TLS fragmentation drops.

---

### 🚀 What's New in v3.1.0

- 📊 **3 Home-Screen Widget Sizes (2x2, 3x2, 4x1)**:
  - **2×2 Compact Square Widget**: Small vertical widget for dense home screen layouts.
  - **3×2 Detailed Card Widget**: Comprehensive widget showing public IP, location badge, 1s traffic, and dedicated live Ping (`⚡ ms`) line.
  - **4×1 Horizontal Bar Widget**: Sleek horizontal widget bar for one-tap toggle and quick status monitoring.
- 🌐 **Wi-Fi Connectivity & Socket Protection**: Explicitly binds protected VPN sockets to active physical interfaces (`setUnderlyingNetworks`) and preserves original SNI & HTTP `Host` headers on Wi-Fi connections.
- 🗺️ **Reliable GeoIP Resolution & Location Fix**: Upgraded `LocationResolver` to fast HTTPS GeoIP endpoints (`https://ipwho.is/` and `https://freeipapi.com/api/json`) without in-flight job cancellations.
- 🔔 **Enhanced Notification Panel Telemetry**: Download (`↓`) and Upload (`↑`) rates are prominently formatted in the notification title, text, and BigText view without truncation on MIUI / Android status bars.
- 🏷️ **GitHub Release CI Filtering**: Automatically tags generated release artifacts (`v3.1.0`) and excludes debug APKs from final GitHub release pages.

---

### 🚀 What's New in v3.0.0

- 🇮🇷 **Complete Persian & English Localization**: 100% translation coverage for Split Tunneling, Connect Orb states, connection stages ("Authenticating...", "Preparing...", etc.), and Settings menus.
- 📐 **Harmonized Language Switcher**: Equal-width SegmentedButtons for System Default, English, and Persian options in Settings.
- 🌐 **Wi-Fi Handshake & DNS Resolution Fix**: Resolves gateway hostnames to IPv4 in Kotlin with SNI header preservation, eliminating Wi-Fi handshake hangs ("در انتظار سرور") on dual-stack IPv6/SLAAC networks.
- ⏱️ **1-Second Real-Time Updates**: Connection duration timer and traffic counters refresh every 1 second in both the app and the home-screen widget.
- ⚡ **Live Ping (RTT) Latency**: Multi-endpoint TCP ping polling displayed live in connection details and inside the 2×2 widget.
- 🗺️ **Reliable Exit Geolocation**: Secure HTTPS lookup endpoints with fallbacks to display public IP, flag emoji, and city/country info.

---

### 🚀 What's New in v2.0.0

- 📂 **Multi-Profile Management & JSON Backup**: Save, edit, switch, export, and import multiple VPN profiles.
- ⚙️ **Extended OpenConnect Options**: Custom CA/Client certs, RSA SecurID/TOTP tokens, OS spoofing, CSD posture checks, PFS enforcement, and DPD timeouts.
- 🌐 **Dual Language & Dynamic RTL**: Seamless toggle between English and Persian with Unicode LTR mark formatting for technical terms.
- 📊 **Enhanced 2×2 Home-Screen Widget**: Displays connected profile, public IP, country flag, 1s timer, traffic, and ping with one-tap toggle.
