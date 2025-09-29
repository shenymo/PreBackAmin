#!/bin/sh
set -euo pipefail

if [ -z "${ANDROID_NDK_HOME:-}" ] && [ -z "${ANDROID_NDK:-}" ]; then
  echo "ANDROID_NDK_HOME or ANDROID_NDK must be set" >&2
  exit 1
fi

NDK="${ANDROID_NDK_HOME:-${ANDROID_NDK}}"
PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
BUILD_DIR="$PROJECT_DIR/build"
MODULE_DIR="$PROJECT_DIR/module/zygisk"
ABIS="arm64-v8a armeabi-v7a"
API_LEVEL=30

rm -rf "$BUILD_DIR"

for ABI in $ABIS; do
  ABI_BUILD_DIR="$BUILD_DIR/$ABI"
  cmake -G Ninja \
    -S "$PROJECT_DIR/native" \
    -B "$ABI_BUILD_DIR" \
    -DANDROID_ABI="$ABI" \
    -DANDROID_PLATFORM=android-$API_LEVEL \
    -DANDROID_STL=c++_shared \
    -DCMAKE_TOOLCHAIN_FILE="$NDK/build/cmake/android.toolchain.cmake"
  cmake --build "$ABI_BUILD_DIR"
  mkdir -p "$MODULE_DIR/$ABI"
  cp "$ABI_BUILD_DIR/libzygisk-preback.so" "$MODULE_DIR/$ABI/"
done

printf 'Build complete. Libraries copied to %s\n' "$MODULE_DIR"
