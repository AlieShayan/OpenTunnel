# OpenTunnel

[![Latest Release](https://img.shields.io/github/v/release/AlieShayan/OpenTunnel?color=0080FF&style=flat-square&logo=github)](https://github.com/AlieShayan/OpenTunnel/releases/latest)
[![License: LGPL 2.1](https://img.shields.io/badge/License-LGPL%202.1-orange.svg?style=flat-square)](LICENSE)
[![Android Support](https://img.shields.io/badge/Android-7.0%2B%20%28API%2024%29-3DDC84?style=flat-square&logo=android&logoColor=white)](app/build.gradle.kts)
[![Languages](https://img.shields.io/badge/Localization-English%20%7C%20Persian-blue?style=flat-square)](app/src/main/res)

OpenTunnel is an open-source, enterprise-grade Android client for **Cisco AnyConnect** and OpenConnect SSL VPN protocols. Built upon a native C engine (`openconnect`) linked with OpenSSL 3.x, OpenTunnel provides secure, high-throughput network tunneling with a modern Jetpack Compose Material 3 interface, granular per-app split tunneling, real-time connection telemetry, and multi-layout home-screen widgets.

---

## Technical Highlights & Features

### Core Engine & Security
* **Native C Performance:** Powered by `libopenconnect` compiled for `arm64-v8a`, `armeabi-v7a`, and `x86_64` architectures with OpenSSL 3.x, `libxml2`, `lz4`, and `stoken`.
* **Hardware-Backed Encryption:** Credential storage secured via Android Keystore with AES-256-GCM encryption.
* **Authentication Protocols:** Supports password-based auth, client certificates/PKCS#12, private keys, RSA SecurID (`stoken`), TOTP/HOTP 2FA, custom OS spoofing, and CSD (Cisco Secure Desktop) posture script wrappers.
* **Perfect Forward Secrecy (PFS):** Complete session confidentiality with custom DPD (Dead Peer Detection) timers and SSL/DTLS cipher selection.

### Network & Reliability
* **DNS-over-HTTPS (DoH) Fallback:** Automatic DNS resolution via Cloudflare (`1.1.1.1`) and Google (`8.8.8.8`) DoH endpoints when standard DNS queries fail on restricted Wi-Fi networks.
* **Hotspot & Tethering Optimization:** Automated MTU auto-tuning (1350 bytes default) and official Cisco AnyConnect User-Agent header spoofing for seamless device tethering.
* **Per-App Split Tunneling:** Selectively route application traffic inside or outside the encrypted VPN tunnel with search and instant application category filters.

### UI & Telemetry
* **Modern Material 3 Interface:** Built entirely with Jetpack Compose, offering full dynamic color theme support (Material You), dark/light modes, and intuitive profile workflows.
* **Real-Time Telemetry:** Live sub-second updates for download/upload throughput, packet statistics, connection uptime, ping latency, and public IP resolution.
* **Interactive App Widgets:** Home-screen widgets available in 2x2, 3x2, and 4x1 layout configurations with live status indicators and one-tap connection controls.
* **Localization:** Native support for English and Persian (Farsi).

---

## Latest Release (v3.1.2)

* **Enforced Release Build Signing:** Strict production release signing requirements to guarantee APK signature integrity.
* **DoH Fallback Subsystem:** Integrated DNS-over-HTTPS fallback for restricted network environments.
* **Expanded Widget System:** Full 4-column horizontal widget (4x1) for compact home-screen status monitoring.
* **Tethering Enhancements:** Optimized MTU framing and User-Agent parameters for tethered mobile hotspots.

---

## Project Architecture

```
OpenTunnel/
├── app/                        # Android application source code (Kotlin + Jetpack Compose)
│   ├── src/main/java/dev/opentunnel/vpn/
│   │   ├── core/               # Engine lifecycle, VPN service, and JNI bindings
│   │   ├── data/               # Data repositories, Room DB, and encrypted storage
│   │   ├── service/            # OpenTunnelVpnService & network interface handlers
│   │   ├── ui/                 # Jetpack Compose screens, components, and viewmodels
│   │   └── widget/             # AppWidgetProvider implementations (2x2, 3x2, 4x1)
│   └── src/main/jni/           # JNI interface wrappers for libopenconnect
├── native/                     # Native engine cross-compilation toolchain
│   ├── build-openconnect.sh    # NDK cross-compilation shell script
│   └── patches/                # Dependency patches for OpenSSL, libxml2, lz4
└── .github/workflows/          # CI/CD workflows for multi-ABI native compilation & release packaging
```

---

## Build & Development

### Prerequisites
* **Android Studio:** Ladybug (2024.2.1) or newer
* **Android SDK:** API 35 (Compile target) / API 24 (Minimum support)
* **Android NDK:** r27+ (`27.2.12479018` recommended)
* **JDK:** Java 17

### Local Compilation

1. **Set Environment Variables:**
   ```bash
   export ANDROID_NDK_HOME=$HOME/Android/Sdk/ndk/27.2.12479018
   ```

2. **Cross-Compile Native Engine:**
   Run the native build script to generate `libopenconnect.so` and dependencies for target ABIs:
   ```bash
   ./native/build-openconnect.sh --abis arm64-v8a,armeabi-v7a,x86_64
   ```

3. **Assemble APK:**
   ```bash
   ./gradlew assembleDebug
   ```

### CI/CD Pipeline
Builds are automated via GitHub Actions. Pushing release tags (`v*`) triggers automated cross-compilation of native binaries across all supported ABIs and packages optimized release APKs.

---

## License & Credits

* **Core Engine:** `openconnect` (LGPL-2.1) compiled with OpenSSL 3.x (Apache-2.0), `libxml2` (MIT), and `lz4` (BSD).
* **JNI Interface:** `LibOpenConnect.java` (LGPL-2.1 © 2013 Kevin Cernekee).
* **Application & Design:** OpenTunnel developed by **AlieShayan**.

*Disclaimer: OpenTunnel is an independent open-source project and is not affiliated with, sponsored by, or endorsed by Cisco Systems.*
