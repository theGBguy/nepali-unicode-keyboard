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
# Only the installed copy may be registered: System Settings reads input modes from whichever
# copy Launch Services knows, including an outdated build left in DerivedData
LSREGISTER=/System/Library/Frameworks/CoreServices.framework/Frameworks/LaunchServices.framework/Support/lsregister
"$LSREGISTER" -u "$APP" 2>/dev/null || true
"$LSREGISTER" -f -R "$DEST/NepaliKeyboard.app"
"$DEST/NepaliKeyboard.app/Contents/MacOS/NepaliKeyboard" --install
echo "Installed $DEST/NepaliKeyboard.app"
