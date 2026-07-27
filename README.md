# OpenTunnel v3.1.1

An open-source, high-performance Android client for **Cisco AnyConnect** and OpenConnect SSL VPNs. OpenTunnel is powered by the native `openconnect` C engine compiled for Android with OpenSSL 3.x, featuring a modern Material You interface, multi-profile management, granular split tunneling, 1-second real-time telemetry, 3 home-screen widget sizes (2x2, 3x2, 4x1), and complete English & Persian (Farsi) localization.

---

## 🚀 What's New in Version 3.1.1

- 📐 **Full 4-Column Horizontal Widget (4×1)**: Removed width caps so the horizontal widget stretches fully across 4 screen columns without text overflow or button clipping.
- 🔤 **Enhanced Readability Across All Widgets**: Increased text sizes for labels, status, IP, location, and timer across 2×2, 3×2, and 4×1 widgets for clear readability.
- 📶 **Wi-Fi Hotspot & Tethering Connection Fix**: Resolved SSL negotiation timeouts on Wi-Fi Hotspots with tuned MTU (1350) and standard AnyConnect User-Agent to prevent packet drops and DPI blocks.

- 🇮🇷 **Complete Persian (Farsi) & English Localization**: Full translation coverage for Split Tunneling, Connect Orb states, connection stages, and Settings options with RTL support.
- ⏱️ **Real-Time 1-Second Telemetry**: Live traffic counters, latency ping, and connection duration update every 1 second across the app UI and all widget sizes.

---

## ✨ Key Features

- 📂 **Multi-Profile Management**: Create, edit, and switch between multiple VPN profiles with JSON backup export/import.
- 🔀 **Per-App Split Tunneling**: Route specific apps inside or outside the VPN tunnel with search and category filtering.
- 🔐 **Advanced Authentication**: Custom CA/Client certificates, Private Keys, RSA SecurID (`stoken`), TOTP, HOTP, custom OS spoofing, and CSD posture wrapper support.
- 📱 **Interactive Home-Screen Widgets**: 2x2, 3x2, and 4x1 widgets showing connected profile, flag/location, public IP, 1s timer, traffic, ping, and one-tap connect/disconnect/cancel.
- 🛡️ **Security Controls**: Perfect Forward Secrecy (PFS), XML POST toggle, custom DPD timeouts, credential caching controls, and AES-256-GCM Keystore encryption.

---

## 🛠️ Build & Development

### Requirements
Android Studio Ladybug+, Android SDK 35, NDK r27+, JDK 17.

### Option A — GitHub Actions CI (Recommended)
Push tags or commits to GitHub to automatically cross-compile `libopenconnect.so` and build signed APK artifacts (`opentunnel_3.1.0_release.apk`).

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
