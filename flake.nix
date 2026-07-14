{
  description = "ASR - Todo & Habit App - Development environment";

  inputs = {
    nixpkgs.url = "https://flakehub.com/f/NixOS/nixpkgs/0.1";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs =
    { self, nixpkgs, flake-utils }:
    flake-utils.lib.eachDefaultSystem (
      system:
      let
        pkgs = import nixpkgs {
          inherit system;
          config.allowUnfree = true;
        };
      in
      {
        devShells.default = pkgs.mkShellNoCC {
          name = "asr-dev";

          packages = with pkgs; [
            jdk21
            gh
            zlib
            ncurses
            gcc
            patchelf
          ] ++ [ pkgs."nix-ld" ];

          JAVA_HOME = "${pkgs.jdk21}";

          shellHook = ''
            export GRADLE_USER_HOME="$PWD/.gradle-home"

            if [ -z "$ANDROID_HOME" ]; then
              export ANDROID_HOME="$PWD/.android-sdk"
            fi

            mkdir -p "$ANDROID_HOME/licenses"
            echo "24333f8a63b6825ea9c5514f83c2829b004d1fee" > "$ANDROID_HOME/licenses/android-sdk-license"

            # nix-ld: run prebuilt Linux binaries on NixOS
            export NIX_LD="${pkgs.glibc}/lib/ld-linux-x86-64.so.2"
            export NIX_LD_LIBRARY_PATH="${pkgs.zlib}/lib:${pkgs.ncurses}/lib:${pkgs.gcc.lib}/lib"

            patch_aapt2() {
              while IFS= read -r -d '' bin; do
                patchelf --set-interpreter "${pkgs."nix-ld"}/bin/nix-ld" "$bin" 2>/dev/null || true
              done < <(find "$PWD/.gradle-home/caches" "$PWD/.android-sdk/build-tools" -name aapt2 -type f -print0 2>/dev/null)
            }
            patch_aapt2

            gradlew() {
              patch_aapt2
              "$PWD/gradlew" "$@"
            }

            echo ""
            echo "ASR dev shell ready"
            echo "  JAVA_HOME=$JAVA_HOME"
            echo "  ANDROID_HOME=$ANDROID_HOME"
            echo ""
            echo "  Build: gradlew assembleDebug"
            echo "  Tests: gradlew test"
            echo ""
          '';
        };
      }
    );
}
