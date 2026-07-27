# OpenTunnel v3.0.0

An open-source, high-performance Android client for **Cisco AnyConnect** and OpenConnect SSL VPNs. OpenTunnel is powered by the native `openconnect` C engine compiled for Android with OpenSSL 3.x, featuring a modern Material You interface, multi-profile management, granular split tunneling, 1-second real-time telemetry, home-screen widget, and complete English & Persian (Farsi) localization.

---

## 🚀 What's New in Version 3.0.0

- 🇮🇷 **Complete Persian (Farsi) & English Localization**: Full translation coverage for Split Tunneling, Connect Orb states, connection stages ("Authenticating...", "Preparing...", etc.), and Settings options with RTL support.
- 📐 **Harmonized Language Selector**: Balanced, equal-width segmented controls for System Default, English, and Persian options.
- 🌐 **Wi-Fi Handshake & DNS Pre-Resolution**: Pre-resolves gateway hostnames to IPv4 addresses in Kotlin with SNI preservation, resolving Wi-Fi handshake hangs ("در انتظار سرور") on dual-stack IPv6/SLAAC networks.
- ⏱️ **Real-Time 1-Second Telemetry**: Live traffic counters and connection duration update every 1 second across both the app UI and the home-screen widget.
- ⚡ **Multi-Target Latency Measurement**: Robust TCP RTT latency (Ping) measurement displayed live in both the app and the 2×2 widget.
- 🗺️ **Secure Exit Location Resolver**: Uses HTTPS endpoints with fallbacks to display public IP, country flag, and city/country info reliably.

---

## ✨ Key Features

- 📂 **Multi-Profile Management**: Create, edit, and switch between multiple VPN profiles with JSON backup export/import.
- 🔀 **Per-App Split Tunneling**: Route specific apps inside or outside the VPN tunnel with search and category filtering.
- 🔐 **Advanced Authentication**: Custom CA/Client certificates, Private Keys, RSA SecurID (`stoken`), TOTP, HOTP, custom OS spoofing, and CSD posture wrapper support.
- 📱 **Interactive Home-Screen Widget**: 2×2 widget showing connected profile, flag/location, public IP, 1s timer, traffic, ping, and one-tap connect/disconnect/cancel.
- 🛡️ **Security Controls**: Perfect Forward Secrecy (PFS), XML POST toggle, custom DPD timeouts, credential caching controls, and AES-256-GCM Keystore encryption.

---

## 🛠️ Build & Development

### Requirements
Android Studio Ladybug+, Android SDK 35, NDK r27+, JDK 17.

### Option A — GitHub Actions CI (Recommended)
Push tags or commits to GitHub to automatically cross-compile `libopenconnect.so` and build signed APK artifacts (`opentunnel_3.0.0_release.apk`).

### Option B — Local Build

```bash
# 1. Set Android NDK path
export ANDROID_NDK_HOME=$HOME/Android/Sdk/ndk/27.2.12479018

# 2. Build native dependencies (openconnect + OpenSSL + libxml2 + lz4)
./native/build-openconnect.sh --abis arm64-v8a,armeabi-v7a,x86_64

# 3. Build APK
./gradlew assembleDebug
```

---

## 📄 License & Credits

- **Engine**: `openconnect` (LGPL 2.1) compiled with OpenSSL 3.x (Apache 2.0), `libxml2` (MIT), and `lz4` (BSD).
- **JNI Binding**: `LibOpenConnect.java` (LGPL 2.1 © 2013 Kevin Cernekee).
- **App & Design**: OpenTunnel developed by **AlieShayan**.

*Disclaimer: OpenTunnel is an independent open-source project and is not affiliated with or endorsed by Cisco Systems.*
