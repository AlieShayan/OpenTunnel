# OpenTunnel v2.0.0

An open-source Android client for **Cisco AnyConnect** and OpenConnect SSL VPNs, powered by the native [openconnect](https://www.infradead.org/openconnect/) engine compiled from source for Android — featuring **Multi-Profile Management**, **Extended OpenConnect Options**, **Per-App Split Tunnelling**, **Dynamic Persian (Farsi) & English Localization (with RTL support)**, and a **Live 5-Second Ping & Traffic Home-Screen Widget**.

---

## 🌟 Key Features in Version 2.0.0

### 📱 Multi-Profile Management & JSON Backup
- **Unlimited Profiles**: Add, edit, manage, and switch between any number of VPN profiles for any gateway.
- **Active Profile Selection**: Quickly select the default active profile via radio selection or dropdown menu.
- **JSON Backup Export & Import**: Easily backup and transfer all saved profiles across devices using structured JSON export/import.

### ⚙️ Extended OpenConnect Configuration
- **Certificates & Keys**: Configure custom CA certificates (`.pem`/`.crt`), user certificates, and private key files.
- **Software Tokens**: Built-in support for **RSA SecurID (`stoken`)**, **TOTP (Google Authenticator, etc.)**, and **HOTP** counter-based tokens.
- **Reported OS**: Custom OS spoofing (`Android`, `Linux 64-bit`, `Linux 32-bit`, `Windows`, `macOS`, `iOS`) to bypass gateway device restrictions.
- **Posture Checks & CSD**: Support for custom CSD wrapper scripts for gateways requiring posture verification.
- **Split Tunnel Networks**: Specify network subnets directly per profile or use automatic gateway routing.
- **Security & Timeout Control**: Disable XML POST handshake, enforce Perfect Forward Secrecy (**PFS**), and override Dead Peer Detection (**DPD**) timeouts.
- **Credential Caching Control**: Option to disable credential caching and clear saved passwords at any time.

### 🌐 Dual Language (English / Persian) & Dynamic RTL
- **In-App Language Switcher**: Toggle seamlessly between **System Default**, **English**, and **Persian (فارسی)** in Settings.
- **RTL (Right-to-Left) Engine**: Automatic layout direction switching (`LayoutDirection.Rtl`) for Farsi.
- **Clean Technical Word Formatting**: Technical terms (*VPN*, *OpenConnect*, *IP*, *DNS*, *MTU*, *DTLS*, *IPv6*, *PFS*, *DPD*, *RSA SecurID*, *TOTP*, *HOTP*, etc.) are wrapped with Unicode LTR marks (`\u200E`) to prevent text flow disruption.

### 📊 Enhanced 2×2 Home-Screen Widget
- **Live Connected Profile Name**: Displays active profile label.
- **Public Outbound IP & Location**: Displays external IP address and geolocation badge (flag + country/city).
- **Traffic Counters**: Real-time downloaded (`⬇`) and uploaded (`⬆`) bytes for active session.
- **5-Second Ping Indicator**: Live latency polling engine updating RTT every 5000ms (`⚡ 45 ms`).

### 🔒 Security, Backup & Package Stability
- **Package Conflict Fix**: Harmonized release and debug signing configurations to prevent installation conflict errors upon app updates.
- **Auto-Backup Enabled**: Configured Android Auto-Backup (`android:allowBackup="true"`) to protect saved profiles and settings across device upgrades.
- **Keystore Encryption**: Secrets encrypted at rest with AES-256-GCM via the Android Keystore.

---

## 🛠️ Building the Project

### Option A — GitHub Actions CI (Recommended)
Push this repository to GitHub. `.github/workflows/build.yml` cross-compiles openconnect for `arm64-v8a` + `armeabi-v7a` and attaches the generated APK `opentunnel_2.0.0.apk` as a workflow artifact.

### Option B — Local Build

**Requirements**: Linux, macOS, or WSL2 with Android SDK, NDK r23+, CMake 3.18+, JDK 17, `make`, `pkg-config`, `perl`, `curl`, and `tar`.

```bash
# 1. Set NDK location
export ANDROID_NDK_HOME=$HOME/Android/Sdk/ndk/27.2.12479018

# 2. Cross-compile native engine (once)
./native/build-openconnect.sh --abis arm64-v8a

# 3. Assemble APK
./gradlew assembleDebug
```
The compiled APK lands in `app/build/outputs/apk/debug/opentunnel_2.0.0.apk`.

---

## 📄 License & Credits

- **openconnect**: LGPL 2.1 (linked as a shared library `libopenconnect.so`).
- **LibOpenConnect.java**: LGPL 2.1 © 2013 Kevin Cernekee.
- **OpenSSL**: Apache 2.0 (OpenSSL 3.x).
- **libxml2**: MIT. **lz4**: BSD 2-clause.
- **OpenTunnel**: Developed & Maintained by **AlieShayan**.

*Disclaimer: OpenTunnel is not affiliated with or endorsed by Cisco Systems. "AnyConnect" is a trademark of Cisco Systems.*
