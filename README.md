# OpenTunnel v3.1.2

[![Latest Release](https://img.shields.io/github/v/release/AlieShayan/OpenTunnel?color=0080FF&style=for-the-badge&logo=github)](https://github.com/AlieShayan/OpenTunnel/releases/latest)
[![License](https://img.shields.io/badge/License-LGPL%202.1-orange.svg?style=for-the-badge)](LICENSE)
[![Android](https://img.shields.io/badge/Android-7.0%2B%20%28API%2024%29-3DDC84?style=for-the-badge&logo=android&logoColor=white)](app/build.gradle.kts)
[![Language](https://img.shields.io/badge/Localization-English%20%7C%20Persian-blue?style=for-the-badge)](app/src/main/res/values-fa)

An open-source, high-performance Android client for **Cisco AnyConnect** and OpenConnect SSL VPNs. OpenTunnel is powered by the native `openconnect` C engine compiled for Android with OpenSSL 3.x, featuring a modern Material You interface, multi-profile management, granular split tunneling, 1-second real-time telemetry, 3 home-screen widget sizes (2x2, 3x2, 4x1), and complete English & Persian (Farsi) localization.

[📥 **Download Latest APK (v3.1.2)**](https://github.com/AlieShayan/OpenTunnel/releases/latest)

---


## 🚀 What's New in Version 3.1.2

- 🔒 **Enforced Release Build Signing**: Fixed APK update signature mismatch by requiring release signing keys and removing debug fallback in release builds.
- 🌐 **DNS-over-HTTPS (DoH) Fallback**: Added automatic DoH resolution (Cloudflare `1.1.1.1` & Google `8.8.8.8`) when system DNS fails on restricted Wi-Fi networks.
- 🧹 **Cleaned Codebase Documentation**: Sanitized all Persian inline comments from source files and build scripts into standard English documentation.
- 📐 **Full 4-Column Horizontal Widget (4×1)**: Expanded widget layout flexibility without text wrapping into control buttons.
- 📶 **Wi-Fi Hotspot & Tethering Connection Fix**: Auto-tuned MTU (1350) and official AnyConnect User-Agent for seamless tethering.

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
Push tags or commits to GitHub to automatically cross-compile `libopenconnect.so` and build signed APK artifacts (`opentunnel_3.1.2_release.apk`).

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
