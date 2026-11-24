#!/bin/sh
# Builds the input method, installs it for this user and restarts it.
# First run: then open System Settings → Keyboard → Text Input → Input Sources → Edit → + →
# Nepali, and add "Nepali – Romanized" and/or "Nepali – Traditional" (the script tries to enable
# them itself). After that, rerun this script after each change; no log out needed.
set -e
cd "$(dirname "$0")/.."
DERIVED="${DERIVED:-$HOME/Library/Developer/Xcode/DerivedData/NepaliKeyboardMac}"
CONFIG="${CONFIG:-Debug}"

xcodebuild -project NepaliKeyboard.xcodeproj -scheme NepaliKeyboard -configuration "$CONFIG" \
  -derivedDataPath "$DERIVED" -destination 'platform=macOS' -quiet build

APP="$DERIVED/Build/Products/$CONFIG/NepaliKeyboard.app"
DEST="$HOME/Library/Input Methods"
mkdir -p "$DEST"
killall NepaliKeyboard 2>/dev/null || true
rm -rf "$DEST/NepaliKeyboard.app"
cp -R "$APP" "$DEST/"
"$DEST/NepaliKeyboard.app/Contents/MacOS/NepaliKeyboard" --install
echo "Installed $DEST/NepaliKeyboard.app"
