#!/usr/bin/env bash
#
# Cross-compiles openconnect (+ OpenSSL, libxml2, lz4) for Android and drops
# libopenconnect.so — with the JNI bindings linked in — into app/src/main/jniLibs.
#
#   ./native/build-openconnect.sh                    # all default ABIs
#   ./native/build-openconnect.sh --abis arm64-v8a   # just one, much faster
#
# Requirements on the build host (Linux, macOS, or WSL2):
#   * Android NDK r23 or newer   -> ANDROID_NDK_HOME / ANDROID_NDK_ROOT
#   * cmake >= 3.18, make, pkg-config, perl, curl (or wget), tar, xz
#
# Nothing here needs root, and nothing is installed system-wide: every artifact
# lands under native/build/.
#
set -euo pipefail

# --------------------------------------------------------------------------
# Defaults (overridable via flags; Gradle passes the gradle.properties values)
# --------------------------------------------------------------------------
OPENCONNECT_VER="9.21"
OPENSSL_VER="3.5.7"
LIBXML2_VER="2.13.8"
LZ4_VER="1.10.0"

ABIS="arm64-v8a,armeabi-v7a,x86_64"
API="26"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
OUT_DIR="$ROOT_DIR/app/src/main/jniLibs"
JAVA_OUT_DIR="$ROOT_DIR/app/src/main/java"
BUILD_DIR="$SCRIPT_DIR/build"
DL_DIR="$BUILD_DIR/downloads"
JOBS="$(getconf _NPROCESSORS_ONLN 2>/dev/null || echo 4)"
FORCE=0

while [[ $# -gt 0 ]]; do
    case "$1" in
        --version)   OPENCONNECT_VER="$2"; shift 2 ;;
        --openssl)   OPENSSL_VER="$2";     shift 2 ;;
        --libxml2)   LIBXML2_VER="$2";     shift 2 ;;
        --lz4)       LZ4_VER="$2";         shift 2 ;;
        --abis)      ABIS="$2";            shift 2 ;;
        --api)       API="$2";             shift 2 ;;
        --out)       OUT_DIR="$2";         shift 2 ;;
        --java-out)  JAVA_OUT_DIR="$2";    shift 2 ;;
        --jobs|-j)   JOBS="$2";            shift 2 ;;
        --force)     FORCE=1;              shift ;;
        -h|--help)   sed -n '2,20p' "$0"; exit 0 ;;
        *) echo "unknown option: $1" >&2; exit 2 ;;
    esac
done

# --------------------------------------------------------------------------
# Pretty logging
# --------------------------------------------------------------------------
if [[ -t 1 ]]; then
    C_B=$'\033[1m'; C_G=$'\033[32m'; C_Y=$'\033[33m'; C_R=$'\033[31m'; C_0=$'\033[0m'
else
    C_B=""; C_G=""; C_Y=""; C_R=""; C_0=""
fi
say()  { printf '%s==>%s %s\n' "$C_G$C_B" "$C_0" "$*"; }
sub()  { printf '    %s\n' "$*"; }
warn() { printf '%s[warn]%s %s\n' "$C_Y" "$C_0" "$*" >&2; }
die()  { printf '%s[error]%s %s\n' "$C_R" "$C_0" "$*" >&2; exit 1; }

# --------------------------------------------------------------------------
# Locate the NDK
# --------------------------------------------------------------------------
find_ndk() {
    for candidate in "${ANDROID_NDK_HOME:-}" "${ANDROID_NDK_ROOT:-}" "${NDK_HOME:-}"; do
        [[ -n "$candidate" && -d "$candidate" ]] && { echo "$candidate"; return; }
    done
    for sdk in "${ANDROID_SDK_ROOT:-}" "${ANDROID_HOME:-}" "$HOME/Android/Sdk" \
               "$HOME/Library/Android/sdk" "/usr/local/lib/android/sdk"; do
        [[ -n "$sdk" && -d "$sdk/ndk" ]] || continue
        # highest-numbered NDK wins
        local newest
        newest="$(ls -1 "$sdk/ndk" 2>/dev/null | sort -V | tail -1)"
        [[ -n "$newest" ]] && { echo "$sdk/ndk/$newest"; return; }
    done
    return 1
}

NDK="$(find_ndk)" || die "Android NDK not found. Install it (Android Studio > SDK Manager > SDK Tools > NDK) and export ANDROID_NDK_HOME=/path/to/ndk/<version>"
[[ -d "$NDK/toolchains/llvm/prebuilt" ]] || die "'$NDK' does not look like an NDK (no toolchains/llvm/prebuilt)"

case "$(uname -s)" in
    Linux)  HOST_TAG="linux-x86_64" ;;
    Darwin) HOST_TAG="darwin-x86_64" ;;
    *)      die "unsupported build host '$(uname -s)' — use Linux, macOS, or WSL2" ;;
esac
TOOLCHAIN="$NDK/toolchains/llvm/prebuilt/$HOST_TAG"
[[ -d "$TOOLCHAIN" ]] || die "no prebuilt toolchain at $TOOLCHAIN"

# cmake: prefer the SDK-managed one, fall back to whatever is on PATH
CMAKE_BIN="$(command -v cmake || true)"
for sdk in "${ANDROID_SDK_ROOT:-}" "${ANDROID_HOME:-}" "$HOME/Android/Sdk" "$HOME/Library/Android/sdk"; do
    [[ -n "$sdk" && -d "$sdk/cmake" ]] || continue
    c="$(ls -1d "$sdk"/cmake/*/bin/cmake 2>/dev/null | sort -V | tail -1)"
    [[ -n "$c" ]] && { CMAKE_BIN="$c"; break; }
done
[[ -n "$CMAKE_BIN" ]] || die "cmake not found (install it, or add it via the Android SDK Manager)"

for tool in make pkg-config perl tar; do
    command -v "$tool" >/dev/null || die "'$tool' is required but not installed"
done

if command -v curl >/dev/null; then
    fetch() { curl -fL --retry 3 --progress-bar -o "$1" "$2"; }
elif command -v wget >/dev/null; then
    fetch() { wget -q --show-progress -O "$1" "$2"; }
else
    die "need curl or wget to download sources"
fi

say "NDK        $NDK"
sub "toolchain  $TOOLCHAIN"
sub "cmake      $CMAKE_BIN"
sub "openconnect $OPENCONNECT_VER / openssl $OPENSSL_VER / libxml2 $LIBXML2_VER / lz4 $LZ4_VER"
sub "ABIs       $ABIS   (minSdk $API, -j$JOBS)"

# --------------------------------------------------------------------------
# Download + unpack sources once, shared across ABIs
# --------------------------------------------------------------------------
mkdir -p "$DL_DIR" "$BUILD_DIR/src"

grab() { # grab <filename> <url>
    local file="$DL_DIR/$1"
    if [[ -s "$file" ]]; then sub "cached  $1"; return; fi
    sub "fetch   $1"
    fetch "$file.part" "$2" || die "download failed: $2"
    mv "$file.part" "$file"
}

unpack() { # unpack <tarball> <expected-dir-name>
    local dest="$BUILD_DIR/src/$2"
    if [[ -d "$dest" ]]; then return; fi
    sub "unpack  $1"
    tar -xf "$DL_DIR/$1" -C "$BUILD_DIR/src"
    [[ -d "$dest" ]] || die "expected $dest after unpacking $1"
}

LIBXML2_SERIES="${LIBXML2_VER%.*}"

say "Fetching sources"
grab "openconnect-$OPENCONNECT_VER.tar.gz" \
     "https://www.infradead.org/openconnect/download/openconnect-$OPENCONNECT_VER.tar.gz"
grab "openssl-$OPENSSL_VER.tar.gz" \
     "https://github.com/openssl/openssl/releases/download/openssl-$OPENSSL_VER/openssl-$OPENSSL_VER.tar.gz"
grab "libxml2-$LIBXML2_VER.tar.xz" \
     "https://download.gnome.org/sources/libxml2/$LIBXML2_SERIES/libxml2-$LIBXML2_VER.tar.xz"
grab "lz4-$LZ4_VER.tar.gz" \
     "https://github.com/lz4/lz4/releases/download/v$LZ4_VER/lz4-$LZ4_VER.tar.gz"

unpack "openconnect-$OPENCONNECT_VER.tar.gz" "openconnect-$OPENCONNECT_VER"
unpack "openssl-$OPENSSL_VER.tar.gz"         "openssl-$OPENSSL_VER"
unpack "libxml2-$LIBXML2_VER.tar.xz"         "libxml2-$LIBXML2_VER"
unpack "lz4-$LZ4_VER.tar.gz"                 "lz4-$LZ4_VER"

OC_SRC="$BUILD_DIR/src/openconnect-$OPENCONNECT_VER"

# --------------------------------------------------------------------------
# Per-ABI build
# --------------------------------------------------------------------------
abi_triple() {
    case "$1" in
        armeabi-v7a) echo "arm-linux-androideabi" ;;
        arm64-v8a)   echo "aarch64-linux-android" ;;
        x86)         echo "i686-linux-android" ;;
        x86_64)      echo "x86_64-linux-android" ;;
        *) die "unsupported ABI '$1'" ;;
    esac
}
abi_cc_prefix() { # clang wrappers use a different prefix for 32-bit arm
    case "$1" in
        armeabi-v7a) echo "armv7a-linux-androideabi" ;;
        *) abi_triple "$1" ;;
    esac
}
abi_openssl_target() {
    case "$1" in
        armeabi-v7a) echo "android-arm" ;;
        arm64-v8a)   echo "android-arm64" ;;
        x86)         echo "android-x86" ;;
        x86_64)      echo "android-x86_64" ;;
    esac
}

build_abi() {
    local ABI="$1"
    local TRIPLE CC_PREFIX PREFIX WORK
    TRIPLE="$(abi_triple "$ABI")"
    CC_PREFIX="$(abi_cc_prefix "$ABI")"
    PREFIX="$BUILD_DIR/$ABI/sysroot"
    WORK="$BUILD_DIR/$ABI/work"

    printf '\n%s────────────────────────────────────────────────────────%s\n' "$C_B" "$C_0"
    say "Building $ABI ($TRIPLE, API $API)"

    if [[ $FORCE -eq 1 ]]; then rm -rf "${BUILD_DIR:?}/${ABI:?}"; fi
    mkdir -p "$PREFIX" "$WORK"

    export CC="$TOOLCHAIN/bin/${CC_PREFIX}${API}-clang"
    export CXX="$TOOLCHAIN/bin/${CC_PREFIX}${API}-clang++"
    export AR="$TOOLCHAIN/bin/llvm-ar"
    export RANLIB="$TOOLCHAIN/bin/llvm-ranlib"
    export STRIP="$TOOLCHAIN/bin/llvm-strip"
    export NM="$TOOLCHAIN/bin/llvm-nm"
    export LD="$TOOLCHAIN/bin/ld"
    [[ -x "$CC" ]] || die "compiler not found: $CC (is API level $API supported by this NDK?)"

    export PKG_CONFIG_LIBDIR="$PREFIX/lib/pkgconfig"
    export PKG_CONFIG_PATH="$PREFIX/lib/pkgconfig"
    export PKG_CONFIG_SYSROOT_DIR=""

    local COMMON_CFLAGS="-fPIC -O2 -ffunction-sections -fdata-sections"
    [[ "$ABI" == "armeabi-v7a" ]] && COMMON_CFLAGS="$COMMON_CFLAGS -march=armv7-a -mfpu=neon -mfloat-abi=softfp"

    # ---------------- OpenSSL ----------------
    if [[ -f "$PREFIX/lib/libssl.a" ]]; then
        sub "openssl  already built"
    else
        say "  openssl $OPENSSL_VER"
        rm -rf "$WORK/openssl"; mkdir -p "$WORK/openssl"
        (
            cd "$WORK/openssl"
            export ANDROID_NDK_ROOT="$NDK"
            export PATH="$TOOLCHAIN/bin:$PATH"
            # OpenSSL's android-* targets derive the compiler from PATH + API level.
            "$BUILD_DIR/src/openssl-$OPENSSL_VER/Configure" \
                "$(abi_openssl_target "$ABI")" \
                -D__ANDROID_API__="$API" \
                --prefix="$PREFIX" \
                --openssldir="$PREFIX/ssl" \
                --libdir=lib \
                no-shared no-tests no-docs no-comp \
                >"$WORK/openssl-configure.log" 2>&1 || {
                    tail -30 "$WORK/openssl-configure.log" >&2; die "openssl configure failed ($ABI)"; }
            make -j"$JOBS" build_libs >"$WORK/openssl-build.log" 2>&1 || {
                tail -40 "$WORK/openssl-build.log" >&2; die "openssl build failed ($ABI)"; }
            make install_dev >>"$WORK/openssl-build.log" 2>&1 || {
                tail -40 "$WORK/openssl-build.log" >&2; die "openssl install failed ($ABI)"; }
        )
    fi

    # ---------------- libxml2 (CMake) ----------------
    if [[ -f "$PREFIX/lib/libxml2.a" ]]; then
        sub "libxml2  already built"
    else
        say "  libxml2 $LIBXML2_VER"
        rm -rf "$WORK/libxml2"
        "$CMAKE_BIN" -S "$BUILD_DIR/src/libxml2-$LIBXML2_VER" -B "$WORK/libxml2" \
            -DCMAKE_TOOLCHAIN_FILE="$NDK/build/cmake/android.toolchain.cmake" \
            -DANDROID_ABI="$ABI" -DANDROID_PLATFORM="android-$API" \
            -DCMAKE_BUILD_TYPE=Release \
            -DCMAKE_INSTALL_PREFIX="$PREFIX" \
            -DCMAKE_INSTALL_LIBDIR=lib \
            -DCMAKE_POSITION_INDEPENDENT_CODE=ON \
            -DBUILD_SHARED_LIBS=OFF \
            -DLIBXML2_WITH_PROGRAMS=OFF -DLIBXML2_WITH_TESTS=OFF \
            -DLIBXML2_WITH_PYTHON=OFF -DLIBXML2_WITH_ICONV=OFF \
            -DLIBXML2_WITH_LZMA=OFF -DLIBXML2_WITH_ZLIB=OFF \
            -DLIBXML2_WITH_HTTP=OFF -DLIBXML2_WITH_FTP=OFF \
            -DLIBXML2_WITH_THREADS=ON -DLIBXML2_WITH_MODULES=OFF \
            >"$WORK/libxml2-configure.log" 2>&1 || {
                tail -30 "$WORK/libxml2-configure.log" >&2; die "libxml2 configure failed ($ABI)"; }
        "$CMAKE_BIN" --build "$WORK/libxml2" -j"$JOBS" --target install \
            >"$WORK/libxml2-build.log" 2>&1 || {
                tail -40 "$WORK/libxml2-build.log" >&2; die "libxml2 build failed ($ABI)"; }
    fi

    # ---------------- lz4 (CMake) ----------------
    if [[ -f "$PREFIX/lib/liblz4.a" ]]; then
        sub "lz4      already built"
    else
        say "  lz4 $LZ4_VER"
        rm -rf "$WORK/lz4"
        "$CMAKE_BIN" -S "$BUILD_DIR/src/lz4-$LZ4_VER/build/cmake" -B "$WORK/lz4" \
            -DCMAKE_TOOLCHAIN_FILE="$NDK/build/cmake/android.toolchain.cmake" \
            -DANDROID_ABI="$ABI" -DANDROID_PLATFORM="android-$API" \
            -DCMAKE_BUILD_TYPE=Release \
            -DCMAKE_INSTALL_PREFIX="$PREFIX" \
            -DCMAKE_INSTALL_LIBDIR=lib \
            -DCMAKE_POSITION_INDEPENDENT_CODE=ON \
            -DBUILD_SHARED_LIBS=OFF -DBUILD_STATIC_LIBS=ON \
            -DLZ4_BUILD_CLI=OFF -DLZ4_BUILD_LEGACY_LZ4C=OFF \
            >"$WORK/lz4-configure.log" 2>&1 || {
                tail -30 "$WORK/lz4-configure.log" >&2; die "lz4 configure failed ($ABI)"; }
        "$CMAKE_BIN" --build "$WORK/lz4" -j"$JOBS" --target install \
            >"$WORK/lz4-build.log" 2>&1 || {
                tail -40 "$WORK/lz4-build.log" >&2; die "lz4 build failed ($ABI)"; }
    fi

    # ---------------- openconnect ----------------
    say "  openconnect $OPENCONNECT_VER"

    # jni.h ships inside the NDK sysroot.
    local JNI_INC="$TOOLCHAIN/sysroot/usr/include"
    [[ -f "$JNI_INC/jni.h" ]] || die "jni.h missing from the NDK sysroot ($JNI_INC)"

    rm -rf "$WORK/openconnect"; mkdir -p "$WORK/openconnect"
    (
        cd "$WORK/openconnect"

        # --enable-jni-standalone links jni.c straight into libopenconnect.so, so
        # System.loadLibrary("openconnect") is all the app needs.
        "$OC_SRC/configure" \
            --host="$TRIPLE" \
            --prefix="$PREFIX" \
            --with-openssl="$PREFIX" \
            --without-gnutls \
            --with-java="$JNI_INC" \
            --enable-jni-standalone \
            --enable-shared --disable-static \
            --with-vpnc-script=/system/bin/false \
            --disable-nls \
            --without-stoken --without-libpcsclite \
            --without-gssapi --without-libproxy --without-libpskc \
            --without-openssl-version-check \
            --disable-dsa-tests \
            CFLAGS="$COMMON_CFLAGS" \
            LDFLAGS="-L$PREFIX/lib -Wl,--gc-sections" \
            LIBS="-lz" \
            >"$WORK/openconnect-configure.log" 2>&1 || {
                tail -40 "$WORK/openconnect-configure.log" >&2
                die "openconnect configure failed ($ABI) — full log: $WORK/openconnect-configure.log"; }

        # Android's loader has no concept of versioned sonames: the file must be
        # called libopenconnect.so *and* carry that exact SONAME. Upstream links
        # with -version-number, so override the libtool flags for this target.
        make libopenconnect.map >/dev/null 2>&1 || true
        VSCRIPT=""
        [[ -f libopenconnect.map ]] && VSCRIPT="-Wl,--version-script,libopenconnect.map"

        make -j"$JOBS" libopenconnect.la \
            libopenconnect_la_LDFLAGS="-avoid-version -no-undefined $VSCRIPT" \
            >"$WORK/openconnect-build.log" 2>&1 || {
                tail -60 "$WORK/openconnect-build.log" >&2
                die "openconnect build failed ($ABI) — full log: $WORK/openconnect-build.log"; }
    )

    local SO
    SO="$(find "$WORK/openconnect/.libs" -maxdepth 1 -name 'libopenconnect.so' -type f | head -1)"
    [[ -n "$SO" ]] || SO="$(find "$WORK/openconnect/.libs" -maxdepth 1 -name 'libopenconnect.so*' -type f | head -1)"
    [[ -n "$SO" ]] || die "libopenconnect.so not produced for $ABI"

    mkdir -p "$OUT_DIR/$ABI"
    cp -f "$SO" "$OUT_DIR/$ABI/libopenconnect.so"
    "$STRIP" --strip-unneeded "$OUT_DIR/$ABI/libopenconnect.so"

    # --- post-build sanity checks -------------------------------------------
    local soname
    soname="$("$TOOLCHAIN/bin/llvm-readelf" -d "$OUT_DIR/$ABI/libopenconnect.so" 2>/dev/null \
              | sed -n 's/.*SONAME.*\[\(.*\)\].*/\1/p' | head -1)"
    if [[ -n "$soname" && "$soname" != "libopenconnect.so" ]]; then
        die "$ABI: SONAME is '$soname' but Android requires 'libopenconnect.so' — the -avoid-version override did not take effect"
    fi

    if ! "$NM" -D --defined-only "$OUT_DIR/$ABI/libopenconnect.so" 2>/dev/null \
        | grep -q "Java_org_infradead_libopenconnect_LibOpenConnect_mainloop"; then
        die "$ABI: libopenconnect.so is missing the JNI exports — was --enable-jni-standalone honoured?"
    fi

    # Nothing outside the NDK sysroot may be needed at runtime.
    local badneed
    badneed="$("$TOOLCHAIN/bin/llvm-readelf" -d "$OUT_DIR/$ABI/libopenconnect.so" 2>/dev/null \
               | sed -n 's/.*NEEDED.*\[\(.*\)\].*/\1/p' \
               | grep -vE '^(libc\.so|libm\.so|libdl\.so|libz\.so|liblog\.so|libstdc\+\+\.so|libc\+\+_shared\.so)$' || true)"
    [[ -z "$badneed" ]] || warn "$ABI: unexpected shared dependencies: $(echo "$badneed" | tr '\n' ' ')"

    sub "$(printf '%-12s %s (%s)' "$ABI" "ok" "$(du -h "$OUT_DIR/$ABI/libopenconnect.so" | cut -f1)")"
}

IFS=',' read -r -a ABI_ARRAY <<< "$ABIS"
for abi in "${ABI_ARRAY[@]}"; do
    build_abi "$(echo "$abi" | xargs)"
done

# --------------------------------------------------------------------------
# Keep the Java binding in lockstep with the JNI we just compiled
# --------------------------------------------------------------------------
BINDING_SRC="$OC_SRC/java/src/org/infradead/libopenconnect/LibOpenConnect.java"
if [[ -f "$BINDING_SRC" ]]; then
    mkdir -p "$JAVA_OUT_DIR/org/infradead/libopenconnect"
    cp -f "$BINDING_SRC" "$JAVA_OUT_DIR/org/infradead/libopenconnect/LibOpenConnect.java"
    say "Synced LibOpenConnect.java from openconnect $OPENCONNECT_VER"
else
    warn "LibOpenConnect.java not found in the tarball; keeping the bundled copy"
fi

printf '\n'
say "Done. Native libraries:"
find "$OUT_DIR" -name 'libopenconnect.so' | sort | while read -r f; do
    sub "$(printf '%-14s %s' "$(basename "$(dirname "$f")")" "$(du -h "$f" | cut -f1)")"
done
printf '\nNow build the APK:  ./gradlew assembleDebug\n\n'
