# Native engine build

`build-openconnect.sh` cross-compiles the VPN engine for Android and drops
`libopenconnect.so` into `app/src/main/jniLibs/<abi>/`.

```
openconnect 9.21   shared  ← the engine, with openconnect's own jni.c linked in
├── OpenSSL 3.5.7  static  (TLS + DTLS)
├── libxml2 2.13.8 static  (CSTP config parsing)
├── lz4 1.10.0     static  (payload compression)
└── zlib           from the NDK sysroot
```

## Usage

```bash
export ANDROID_NDK_HOME=$HOME/Android/Sdk/ndk/27.2.12479018

./native/build-openconnect.sh                        # all ABIs from gradle.properties
./native/build-openconnect.sh --abis arm64-v8a       # just one — much faster
./native/build-openconnect.sh --version 9.20         # pin a different openconnect
./native/build-openconnect.sh --force                # rebuild deps from scratch
./native/build-openconnect.sh -j 8                   # parallelism
```

Everything lands under `native/build/`:

```
native/build/downloads/       tarballs (cached between runs)
native/build/src/             unpacked sources
native/build/<abi>/sysroot/   static deps, headers, .pc files
native/build/<abi>/work/      configure/build trees + full logs
```

Deleting `native/build/<abi>/sysroot` forces the dependencies to rebuild;
openconnect itself is always rebuilt.

## Requirements

* Android NDK **r23 or newer** (`ANDROID_NDK_HOME`, `ANDROID_NDK_ROOT`, or an
  `ndk/<version>` directory inside your SDK — the script finds any of them)
* `cmake` ≥ 3.18 — the SDK's copy is used if it is installed
* `make`, `pkg-config`, `perl`, `tar`, and `curl` or `wget`
* A Linux or macOS shell. On Windows use WSL2.

Autotools (`autoconf`/`automake`) are **not** needed: the script uses release
tarballs, which ship a pre-generated `configure`.

## What the script does that matters

* **Builds against OpenSSL rather than GnuTLS.** OpenSSL's `android-*` targets
  are first-class, which removes the GMP + Nettle + GnuTLS chain entirely.
* **Passes `--enable-jni-standalone`**, so openconnect's `jni.c` is compiled
  into `libopenconnect.so`. The app needs exactly one `System.loadLibrary`.
* **Overrides libtool's `-version-number` with `-avoid-version`.** Upstream
  produces `libopenconnect.so.5.x.y` with a matching SONAME; Android's loader
  has no concept of versioned sonames and would fail at `dlopen`. The script
  verifies the resulting SONAME is exactly `libopenconnect.so` and fails loudly
  if the override did not take.
* **Checks the JNI symbols are exported** before accepting the output.
* **Copies `LibOpenConnect.java` out of the tarball** into
  `app/src/main/java/org/infradead/libopenconnect/`, so the Java binding always
  matches the `jni.c` that was actually compiled.

## Troubleshooting

**`compiler not found: …/aarch64-linux-android26-clang`**
The NDK does not have that API level. Lower `openconnect.minApi` in
`gradle.properties` (and `minSdk` in `app/build.gradle.kts` to match), or use a
newer NDK.

**`openconnect configure failed`**
Read `native/build/<abi>/work/openconnect-configure.log`. Nearly always a
missing dependency in the sysroot — check the OpenSSL/libxml2/lz4 logs next to
it, or re-run with `--force`.

**`SONAME is 'libopenconnect.so.5'`**
The libtool flag override was ignored, likely because a future openconnect
changed its `Makefile.am`. Add `-avoid-version` to `libopenconnect_la_LDFLAGS`
in the source tree, or run `patchelf --set-soname libopenconnect.so` on the
output.

**Downloads fail**
`--version` / `--openssl` / `--libxml2` / `--lz4` all accept explicit versions;
you can also drop tarballs into `native/build/downloads/` by hand and the script
will use them.
