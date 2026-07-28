# OpenTunnel v3.3.0 🚀

We are excited to announce **OpenTunnel v3.3.0**! This release introduces a comprehensive Haptic Feedback Subsystem, an expanded 160dp Speed Telemetry Chart with peak download rate visualization and customizable history range selection (1m, 10m, 1h, 2h, 5h), high-frequency 4-second ping latency updates, and fluid Material 3 UI motion physics.

---

## 🌟 Highlights

- **Configurable Haptic Feedback Subsystem:** Tailored tactile vibration feedback for list scrolling, connection status state changes (`CONNECTED` / `DISCONNECTED`), and main configuration switches. Easily toggled on/off in Settings.
- **Expanded Speed Telemetry & History Range:** Increased chart height to 160dp, introduced peak download rate indicator lines with real-time peak throughput labels, and added dynamic time range filters (`1m`, `10m`, `1h`, `2h`, `5h`).
- **High-Frequency Ping Monitoring:** Accelerated ping latency measurement polling to 4-second intervals across the application UI and home-screen widgets.
- **Fluid Material 3 Motion Physics:** Upgraded split-tunneling mode toggling with spring animation physics (`StiffnessMediumLow`) and smoothed `NavHost` section transitions (`FastOutSlowInEasing`).

---

## ✨ What's New

### 🎨 User Interface, Motion & Haptics
- **System-Wide Haptic Feedback Setting:** Added a new setting toggle under "System" in `SettingsScreen` to enable or disable tactile feedback.
- **List Scroll Haptic Ticks:** Triggers subtle vibration ticks (`EFFECT_TICK`) as items/rows pass during scrolling across `SplitTunnelScreen`, `ProfileManagementScreen`, `LogScreen`, `SettingsScreen`, `ProfileScreen`, and `HomeScreen`.
- **VPN Status Connection Haptics:** Distinct vibration feedback patterns on VPN connection success and disconnection/failure events.
- **Main Toggle Vibration:** Tactile click feedback when toggling main configuration switches in Settings, Profile, and Split Tunnel headers (excluding per-app split tunneling checkboxes).
- **Animated Split Tunnel Mode Switch:** Animated sliding pill indicator transition when switching between Exclude and Include split tunneling modes.
- **Seamless Navigation Transitions:** Eased horizontal slide and fade transitions between app screens.

### 📊 Telemetry & Network Performance
- **160dp Speed Chart Height:** Expanded chart canvas height from 90dp to 160dp for optimal curve legibility and telemetry rendering.
- **Peak Download Rate Line & Label:** Permanent horizontal dashed indicator line (`alpha = 0.35f`) with a top summary label showing peak download throughput achieved.
- **Time Range Selector:** Integrated top-corner dropdown to filter history telemetry by `1m`, `10m`, `1h`, `2h`, or `5h` history windows.
- **4-Second Ping Latency Polling:** Updated background ping latency measurement loop from 15 seconds to 4 seconds for both the main app and app widgets.

### 🛠️ Maintenance & Documentation
- **README Update:** Comprehensive documentation updates detailing Haptic Feedback, telemetry ranges, and ping updates.
- **Build System:** Bumped `versionCode` to 11 and `versionName` to 3.3.0.

---

## 📦 Compatibility

- **Android:** 7.0+ (API 24+) / Target API 35
- **Architectures:** `arm64-v8a`, `armeabi-v7a`, `x86_64`
