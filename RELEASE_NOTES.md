# OpenTunnel v4.0.0 🚀

We are thrilled to announce **OpenTunnel v4.0.0**! This milestone major release introduces the **Continuous World / One Unified Environment** architecture, transforming the application into a single physical spatial environment observed by a 2D viewport camera across all four primary destinations (**Home**, **Traffic Monitor**, **Logs**, and **Settings**).

---

## 🌟 Highlights

- **One Continuous World & 2D Spatial Camera:** The entire application now operates as a unified physical universe. Horizontal pager gestures and vertical home scrolling translate the viewport camera smoothly over the stationary world at a locked 120 FPS.
- **3-Layer Global Light Field Model:** The ConnectOrb serves as the primary celestial luminary in world space, projecting light through a 3-layer spatial model:
  - *Layer 1 (Local Aura):* Focused, intense radiance centered on the Orb on the Home viewport.
  - *Layer 2 (Regional Field):* Medium-span ambient glow reaching adjacent viewports (Traffic Monitor ~60%, Logs ~40%).
  - *Layer 3 (Global World Illumination):* Expansive 6-stop celestial field providing subtle, deep illumination across all viewports (Settings ~20%).
- **Real-Time Per-App Traffic Monitoring:** Comprehensive telemetry suite tracking per-application data usage, real-time download/upload transfer rates, network traffic share percentages, and sorting/filtering controls.
- **Translucent Frosted Glass Hierarchy:** Soft depth and cohesive frosted translucency across all surfaces, cards, and floating navigation, allowing ambient world light to organically shine through while maintaining crystal-clear typography.
- **Decoupled Ambient Temporal Breathing:** Majestic 6.5-second celestial breathing cycle for the connected world, decoupled from the interactive ConnectOrb animations.

---

## ✨ What's New

### 🌌 Spatial Universe & UI/UX Architecture
- **2D Camera Coordinate System:** Evaluated entirely in the Compose Draw phase (`Modifier.drawBehind`), guaranteeing zero recomposition during horizontal swiping or vertical scrolling.
- **Graceful Off-Screen Physics:** Natural distance-based light falloff as the Orb leaves the viewport vertically or horizontally.
- **Continuous 4-Viewport World Background:** Spatial celestial landmarks (Traffic Nebula, Settings Crystalline Aura) and vertical parallax depth ($0.18\times \Delta Y$).
- **Floating Island Navigation:** Frosted glass bar with connection-state aware indicator glow and seamless RTL/LTR geometry.
- **Actionable Error Bottom Sheet:** Contextual troubleshooting actions and deep error diagnostics.

### 📊 Network Monitoring & Engine
- **UID Traffic Stats Engine:** Real-time per-app bandwidth accounting with counter-wrap protection and background usage detection.
- **Traffic Direction & Dimension Controls:** Instant sorting by total traffic, download speed, upload speed, or app name, with reversible sort direction.
- **Optimized Handshake & Telemetry:** Multi-endpoint ping latency, fast DNS pre-resolution, and battery optimization guides.

---

## 📦 Compatibility

- **Android:** 7.0+ (API 26+) / Target API 35
- **Architectures:** `arm64-v8a`, `armeabi-v7a`, `x86_64`

