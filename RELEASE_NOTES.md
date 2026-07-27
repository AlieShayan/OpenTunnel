## 🇮🇷 فارسی (Persian)

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
