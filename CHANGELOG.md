# Changelog

All notable changes to the OpenTunnel project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [3.0.0] - 2026-07-27

### Added
- **Complete Split Tunneling Translations**: Added full Persian and English string entries in `Strings.kt` and applied them to `SplitTunnelScreen.kt` for headers, search placeholder, clear button, filters, and empty list hints.
- **Connect Orb & Handshake Stage Localizations**: Added localized button action strings (`CONNECT`, `CONNECTED`, `RETRY`, `STOPPING`, `CANCEL`) and stage status strings (`Authenticating...`, `Preparing...`, `Disconnecting...`, `Reconnecting...`) in English and Persian.
- **Settings Subtitle Localizations**: Added Persian translations for Wallpaper colours, Connect on boot, Always-on VPN, Notification stats, and Verbose logging subtitles.
- **Multi-Endpoint TCP Ping Latency**: Implemented multi-target TCP socket ping measuring (`1.1.1.1:443`, `1.1.1.1:53`, `8.8.8.8:53`) and displayed live Ping latency in `HomeScreen` and `TunnelWidget`.
- **HTTPS Geolocation Fallbacks**: Updated `LocationResolver` to use HTTPS endpoints (`https://ip-api.com/json/...`) with `ipapi.co` and `ipinfo.io` fallbacks to prevent cleartext HTTP policy blocking on Android.

### Fixed
- **Wi-Fi Handshake Hang ("در انتظار سرور")**: Pre-resolved server domain hostnames to IPv4 addresses in Kotlin prior to calling `lib.parseURL()` while maintaining TLS SNI with `lib.setHostname()`, preventing SLAAC IPv6 timeouts on dual-stack Wi-Fi routers.
- **App Language Button Sizing**: Assigned equal layout weight (`Modifier.weight(1f)`) to all three options in `SingleChoiceSegmentedButtonRow` on `SettingsScreen.kt` so "System Default", "English", and "Persian" buttons render with identical width.
- **2-Second Update Delay**: Changed `STATS_INTERVAL_MS` from 2,000ms to 1,000ms in `OpenTunnelVpnService` so second-counters and traffic metrics update every 1 second across both app and widget.

---

## [2.0.0] - 2026-07-27

### Added
- **Multi-Profile Management**: Multi-profile storage, active profile picker, dropdown menu, and JSON export/import backup functionality.
- **Extended OpenConnect Config**: CA certificate, user certificate, private key, software tokens (RSA SecurID/TOTP/HOTP), custom OS spoofing, CSD posture script, XML POST disable, PFS, and custom DPD timeout options.
- **Dynamic RTL & Dual Language**: System Default, English, and Persian language switcher with LTR mark formatting for technical terms.
- **2×2 Home-Screen Widget**: Live widget displaying active profile, public IP, flag/location, elapsed timer, traffic counters, and one-tap connect/disconnect.
- **Package Stability**: Keystore encryption, Android auto-backup, and harmonized signing configurations.
