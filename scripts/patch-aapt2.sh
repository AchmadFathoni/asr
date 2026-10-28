#!/usr/bin/env bash
# Patch aapt2 binaries in gradle caches and android SDK with the given ELF interpreter.
# Usage: patch-aapt2.sh [interpreter]
# Defaults to nix-ld, then NIX_LD, then system default.

set -euo pipefail

INTERP="${1:-}"
if [ -z "$INTERP" ]; then
  INTERP="$(command -v nix-ld || true)"
fi
if [ -z "$INTERP" ]; then
  INTERP="${NIX_LD:-"$(patchelf --print-interpreter "$(which bash)")"}"
fi

GRADLE_USER_HOME="${GRADLE_USER_HOME:-$PWD/../.gradle-home}"
ANDROID_HOME="${ANDROID_HOME:-$PWD/../.android-sdk}"

find "$GRADLE_USER_HOME/caches" "$ANDROID_HOME/build-tools" \
  -name aapt2 -type f -print0 2>/dev/null \
  | while IFS= read -r -d '' bin; do
      current="$(patchelf --print-interpreter "$bin" 2>/dev/null || true)"
      if [ "$current" != "$INTERP" ]; then
        patchelf --set-interpreter "$INTERP" "$bin" 2>/dev/null || true
      fi
    done
