# OpenTunnel

An Android client for **Cisco AnyConnect** SSL VPNs, built on the real
[openconnect](https://www.infradead.org/openconnect/) engine compiled natively
for Android — plus **per-app split tunnelling** and a Material 3 interface.

You only need what your provider gave you: a **server address**, a **username**
and a **password**.

---

## What's in the box

| | |
|---|---|
| **Protocol** | Cisco AnyConnect (CSTP + DTLS). Juniper/Pulse, GlobalProtect, F5, FortiGate and Array are also selectable, since openconnect speaks them. |
| **Engine** | openconnect 9.21 built from source against OpenSSL 3.5, libxml2 and lz4, with openconnect's own JNI bindings linked in. No shelling out to a binary, no root. |
| **Split tunnelling** | Pick apps that bypass the VPN (default), or invert it so only the picked apps are tunnelled. Uses Android's `addDisallowedApplication` / `addAllowedApplication`. |
| **Local network** | Optional bypass for RFC1918 / link-local ranges so printers, NAS boxes and casting keep working while connected. |
| **Auth** | Saved username/password, plus live prompts for anything else the gateway asks (tunnel group, OTP, second factor, password change). |
| **Certificates** | Validated against the device's live trust store. Anything unknown raises a fingerprint dialog and is pinned on approval. |
| **UI** | Compose + Material 3, dark-first, optional wallpaper colour, animated connect orb, live traffic counters, full connection log. |
| **Extras** | Quick Settings tile, connect-on-boot, reconnect on network change, always-on VPN support. |

Secrets are encrypted at rest with an AES-256-GCM key that lives in the Android
Keystore and cannot be exported.

---

## Building

Two things have to happen: the native library gets cross-compiled once, then
Gradle builds the APK. **The APK build itself only needs the normal Android
SDK** — the NDK is used by step 1 only.

### Option A — let GitHub build it (no local toolchain)

Push this repo to GitHub. `.github/workflows/build.yml` installs the NDK,
compiles openconnect for `arm64-v8a` + `armeabi-v7a`, builds the APK and
attaches it to the run as an artifact. Works on the free tier; the first run
takes ~20 minutes, cached runs a few minutes.

### Option B — build locally

**Requirements:** Linux, macOS, or Windows + WSL2, with:

* Android Studio (or just the SDK) — **NDK r23+** and **CMake 3.18+** installed
  via *SDK Manager → SDK Tools*
* `make`, `pkg-config`, `perl`, `curl`, `tar` (all standard; `sudo apt install
  build-essential pkg-config` covers Debian/Ubuntu)
* JDK 17

```bash
export ANDROID_NDK_HOME=$HOME/Android/Sdk/ndk/27.2.12479018   # adjust

# 1. Cross-compile openconnect + OpenSSL + libxml2 + lz4  (~10-25 min, once)
./native/build-openconnect.sh --abis arm64-v8a      # add more ABIs if you like

# 2. Build the app
./gradlew assembleDebug
```

The APK lands in `app/build/outputs/apk/debug/`. Install with
`adb install -r app/build/outputs/apk/debug/app-debug.apk`, or open the project
in Android Studio and press Run.

Step 1 is also wired into Gradle if you prefer: `./gradlew :app:buildNativeLibs`.

> Building the UI without step 1 works fine — the app installs and runs, and
> tells you the engine is missing when you tap Connect. Useful for iterating on
> the interface.

### Which ABIs?

`gradle.properties` controls this in one place:

```properties
openconnect.abis=arm64-v8a,armeabi-v7a,x86_64
```

Almost every phone made since ~2019 is `arm64-v8a`. Building only that one
roughly thirds the build time and the APK size. `x86_64` is for emulators.

### Signing a release build

Create `keystore.properties` in the project root:

```properties
storeFile=/absolute/path/to/release.jks
storePassword=…
keyAlias=…
keyPassword=…
```

Then `./gradlew assembleRelease`. Without that file you still get an unsigned
release APK.

---

## Using it

1. Open the app → tap the server card → enter **server address**, **username**
   and **password**. The address can be `vpn.company.com`, `vpn.company.com:4443`,
   or a full `https://vpn.company.com/group` URL.
2. Back on the home screen, tap the orb. Android asks for VPN permission once.
3. If the gateway wants anything else — a tunnel group, an OTP — a dialog
   appears at that moment.

### Split tunnelling

*Home → Split tunnelling*, turn it on, pick apps.

* **Selected apps bypass VPN** (default) — everything is tunnelled except what
  you tick. This is what you want for a banking app that blocks VPNs, or to keep
  streaming off a slow corporate link.
* **Only selected use VPN** — the inverse. Everything bypasses the tunnel except
  the apps you tick, e.g. only your work mail client goes through the VPN.

Changes apply on the next connect — Android fixes the app list when the tunnel
interface is created and it cannot be changed while the tunnel is up.

### Advanced settings worth knowing

| Setting | When you need it |
|---|---|
| **Report this OS** | Some gateways refuse mobile clients. Reporting `linux-64` is the usual workaround. |
| **Use DTLS** | On by default and much faster. Turn off if the network blocks UDP and you see repeated DTLS failures in the log. |
| **Allow legacy ciphers** | Only for old gateways that still require 3DES-era suites. It weakens the connection. |
| **MTU override** | Fixes stalls on links that fragment badly. 1300 is a good first try. |
| **Disable XML POST** | Compatibility shim for a few non-Cisco gateways. |

If a connection fails, the **Connection log** has openconnect's own output —
turn on *Verbose logging* in Settings first and it usually names the problem
directly.

---

## Layout

```
app/src/main/
  java/dev/opentunnel/vpn/
    core/      TunnelRunner (drives libopenconnect), state bus, prompt bridge
    data/      profile + settings storage, installed-app listing
    service/   VpnService, notifications, QS tile, boot receiver
    ui/        Compose screens, theme, view model
  java/org/infradead/libopenconnect/
    LibOpenConnect.java   ← upstream JNI binding, refreshed by the build script
native/
  build-openconnect.sh    ← cross-compiles the engine
```

The connect path, end to end:

```
parseURL → obtainCookie (auth callbacks) → makeCSTPConnection → getIPInfo
        → VpnService.Builder.establish() → setupTunFD → setupDTLS → mainloop
```

`VpnService.protect()` is wired to openconnect's `onProtectSocket` callback so
the tunnel's own socket never loops back through the tunnel.

---

## Renaming the app

The package is `dev.opentunnel.vpn`. To change it, edit `applicationId` and
`namespace` in `app/build.gradle.kts`, `app_name` in
`app/src/main/res/values/strings.xml`, and the `ACTION_*` constants in
`OpenTunnelVpnService.kt` (they are namespaced strings, not just labels).

---

## Licensing

* **openconnect** — LGPL 2.1. It is linked as a shared library and its source is
  fetched unmodified by `native/build-openconnect.sh`, which keeps LGPL
  relinking rights intact.
* **`LibOpenConnect.java`** — LGPL 2.1, © 2013 Kevin Cernekee, vendored as-is
  from the openconnect tree.
* **OpenSSL** — Apache 2.0 (3.x).
* **libxml2** — MIT. **lz4** — BSD 2-clause.
* The app code here is yours to license as you like, subject to the LGPL
  obligations above.

Not affiliated with or endorsed by Cisco. "AnyConnect" is Cisco's trademark and
is used here only to describe the protocol.
