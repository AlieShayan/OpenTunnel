# OpenTunnel v4.1.0 🚀

We are pleased to announce **OpenTunnel v4.1.0**! This release brings targeted performance optimizations, critical core network & DNS reliability fixes, full Network & Domain Split Tunneling navigation integration, and refined application traffic monitoring restricted strictly to user-installed applications.

---

## 🌟 What's New in v4.1.0

### 📊 Clean User-Only App Traffic Monitoring
- **Restricted to User Applications:** App Traffic Collector and Telemetry now strictly filter out internal system apps and OEM services, monitoring only real, user-installed applications for reduced overhead and focused bandwidth insights.
- **Accurate App Accounting:** Refined `isSystem` classification ensuring non-user components do not clutter the traffic monitor list.

### 🛡️ Core Network & DNS Hardening
- **Offline Numeric IP Validation:** Fixed DNS blocking bug in `Net.isValidIp`, replacing socket lookups with instant numeric IP parsing to guarantee that Custom DoH and DNS pre-resolution are properly utilized.
- **TLS SNI & Hostname Integrity:** Fixed OpenConnect hostname overwrite issue during IP pre-resolution, ensuring the real server domain name is always presented in the TLS SNI extension.
- **Robust DoH Record Extraction:** Upgraded DoH JSON parsing to filter type-1 A records accurately, preventing CNAME/digit matching issues.

### 🌐 Network & Domain Split Tunneling Integration
- **Full Navigation Wiring:** Added dedicated routes and direct shortcuts on both the **Home Screen** and **Settings Screen** for Network & Domain Split Tunneling (`SplitTunnelNetworksScreen`).
- **Flexible Routing Rules:** Seamlessly include or exclude IP CIDR subnets (e.g. `192.168.1.0/24`) and wildcard domain suffixes (e.g. `*.ir`).

### ⚡ Stability & Widget Improvements
- **Safe Widget Consent Flow:** App widget now safely checks `VpnService.prepare` and launches the main interface if system VPN consent has not yet been granted.
- **Safe Emoji Unicode Rendering:** Hardened geographic location flag derivation against invalid ISO code formats.

---

## 📦 Compatibility

- **Android:** 7.0+ (API 26+) / Target API 35
- **Architectures:** `arm64-v8a`, `armeabi-v7a`, `x86_64`

