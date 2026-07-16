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

find "$PWD/.gradle-home/caches" "$PWD/.android-sdk/build-tools" \
  -name aapt2 -type f -print0 2>/dev/null \
  | while IFS= read -r -d '' bin; do
      current="$(patchelf --print-interpreter "$bin" 2>/dev/null || true)"
      if [ "$current" != "$INTERP" ]; then
        patchelf --set-interpreter "$INTERP" "$bin" 2>/dev/null || true
      fi
    done
