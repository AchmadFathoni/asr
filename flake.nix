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
            git
            gh
            gcc
            zlib
            ncurses
            patchelf
          ];

          JAVA_HOME = "${pkgs.jdk21}";

          shellHook = ''
            export GRADLE_USER_HOME="$PWD/.gradle-home"

            if [ -z "$ANDROID_HOME" ]; then
              export ANDROID_HOME="$PWD/.android-sdk"
            fi

            mkdir -p "$ANDROID_HOME/licenses"
            echo "24333f8a63b6825ea9c5514f83c2829b004d1fee" > "$ANDROID_HOME/licenses/android-sdk-license"

            echo ""
            echo "ASR dev shell ready"
            echo "  JAVA_HOME=$JAVA_HOME"
            echo "  ANDROID_HOME=$ANDROID_HOME"
            echo ""
            echo "  First build: ./scripts/gradlew assembleDebug"
            echo "  Tests:       ./scripts/gradlew test"
            echo ""
          '';
        };
      }
    );
}
