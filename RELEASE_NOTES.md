# OpenTunnel v4.1.1 🛠️

A critical stability release resolving a crash on the Home screen during the connection phase.

---

## ⚡ Bug Fixes & Improvements

- **Fixed Crash on Connect:** Resolved an Android HWUI / Skia GPU crash caused by rendering stroked arcs with sweep gradients during the `CONNECTING` animation in `ConnectOrb`.
- **Alpha Clamping Protection:** Fixed potential `IllegalArgumentException` in pulse wave animations by ensuring all alpha values remain strictly within `[0.0, 1.0]`.
- **GPU Lighting Safeguards:** Clamped celestial gradient radii in `WorldLighting` to safe hardware boundaries to prevent GPU texture buffer overflows.
- **Sub-pixel Layout Stability:** Guarded anchor positioning in `OpenTunnelWorld` against sub-pixel float jitter.
- **Uncaught Exception & Crash Logger:** Integrated a global crash handler that persists uncaught exceptions to disk and surfaces previous crash details in the logs upon restart.

---

## 📦 Compatibility

- **Android:** 7.0+ (API 26+) / Target API 35
- **Architectures:** `arm64-v8a`, `armeabi-v7a`, `x86_64`
