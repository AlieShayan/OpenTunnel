# Changelog

All notable changes to the OpenTunnel project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [3.1.0] - 2026-07-27

### Added
- **Three Home Screen Widget Sizes (2x2, 3x2, 4x1)**: Added distinct 2x2 compact, 3x2 card, and 4x1 horizontal bar widget options (`widget_opentunnel_2x2`, `widget_opentunnel_3x2`, `widget_opentunnel_4x1`) registered as separate launcher widget choices.
- **Dedicated Widget Ping Display**: Added a dedicated `widget_ping` TextView view element to display live Ping (`⚡ ms`) on its own line across widget layouts.

### Fixed
- **Wi-Fi Connectivity & Physical Interface Binding**: Bound protected VPN sockets to active underlying networks via `setUnderlyingNetworks()` and preserved domain hostnames in `resolveServerUrl` for TLS SNI and HTTP `Host` header compliance on Wi-Fi.
- **Location Lookup Loop**: Prevented premature cancellation of active location resolution jobs on 1-second stats emissions, and integrated fast HTTPS GeoIP endpoints (`https://ipwho.is/`, `https://freeipapi.com/api/json`).
- **Notification Panel Stats Truncation**: Formatted Download (`↓`) and Upload (`↑`) counters and BigText view in `Notifications.kt` to ensure traffic numbers remain visible on MIUI / Android status bars.
- **Release Versioning & Debug Artifact Exclusion**: Dynamically resolved `versionName` from release tag (`v3.1.0`) in `build.gradle.kts` and filtered GitHub Release action assets to `app/build/outputs/apk/release/*.apk`.

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
