#!/bin/sh
# Builds the Mac input method for release: a universal app signed with Developer ID, in an
# installer package that puts it in /Library/Input Methods and turns its input sources on for the
# user who installs it. Signs the package and notarizes it when the credentials are there.
#
#   macosApp/scripts/package.sh
#
# Environment (all optional):
#   SIGN_APP        app signing identity (default "Developer ID Application")
#   SIGN_INSTALLER  package signing identity (default "Developer ID Installer"; unsigned if missing)
#   NOTARY_PROFILE  notarytool keychain profile (`xcrun notarytool store-credentials`); skipped if unset
#
# Output: macosApp/build/release/NepaliUnicodeKeyboard-<version>.pkg
set -eu
cd "$(dirname "$0")/.."

VERSION=$(sed -n 's/^ *MARKETING_VERSION: "\(.*\)"/\1/p' project.yml)
SIGN_APP="${SIGN_APP:-Developer ID Application}"
SIGN_INSTALLER="${SIGN_INSTALLER:-Developer ID Installer}"
OUT=build/release
DERIVED=build/DerivedData
APP="$DERIVED/Build/Products/Release/NepaliKeyboard.app"
PKG="$OUT/NepaliUnicodeKeyboard-$VERSION.pkg"
LSREGISTER=/System/Library/Frameworks/CoreServices.framework/Frameworks/LaunchServices.framework/Support/lsregister

rm -rf "$OUT"
mkdir -p "$OUT"

echo "Building $VERSION (arm64 + x86_64)…"
xcodebuild -project NepaliKeyboard.xcodeproj -scheme NepaliKeyboard -configuration Release \
  -derivedDataPath "$DERIVED" -destination 'generic/platform=macOS' -quiet \
  ARCHS="arm64 x86_64" ONLY_ACTIVE_ARCH=NO \
  CODE_SIGN_STYLE=Manual CODE_SIGN_IDENTITY="$SIGN_APP" OTHER_CODE_SIGN_FLAGS=--timestamp \
  build
# Launch Services registers build products; an extra copy confuses System Settings
"$LSREGISTER" -u "$APP" 2>/dev/null || true

codesign --verify --strict --deep "$APP"
lipo -archs "$APP/Contents/MacOS/NepaliKeyboard"

# Component package. Not relocatable: the installer must not "upgrade" a copy it finds elsewhere
ROOT="$OUT/root"
mkdir -p "$ROOT"
ditto "$APP" "$ROOT/NepaliKeyboard.app"
pkgbuild --analyze --root "$ROOT" "$OUT/components.plist" >/dev/null
plutil -replace 0.BundleIsRelocatable -bool NO "$OUT/components.plist"
pkgbuild --root "$ROOT" --component-plist "$OUT/components.plist" \
  --identifier io.github.thegbguy.inputmethod.NepaliKeyboard.pkg --version "$VERSION" \
  --install-location "/Library/Input Methods" --scripts scripts/pkg \
  "$OUT/NepaliKeyboard-component.pkg" >/dev/null

# Product archive with a title and the "what next" page
sed "s/@VERSION@/$VERSION/" scripts/pkg-distribution.xml > "$OUT/distribution.xml"
if security find-identity -v | grep -q "$SIGN_INSTALLER"; then
  productbuild --distribution "$OUT/distribution.xml" --resources scripts/pkg-resources \
    --package-path "$OUT" --sign "$SIGN_INSTALLER" --timestamp "$PKG" >/dev/null
  SIGNED=yes
else
  echo "warning: no \"$SIGN_INSTALLER\" identity: the package is unsigned (local testing only)"
  productbuild --distribution "$OUT/distribution.xml" --resources scripts/pkg-resources \
    --package-path "$OUT" "$PKG" >/dev/null
  SIGNED=no
fi

if [ -n "${NOTARY_PROFILE:-}" ] && [ "$SIGNED" = yes ]; then
  echo "Notarizing (a few minutes)…"
  xcrun notarytool submit "$PKG" --keychain-profile "$NOTARY_PROFILE" --wait
  xcrun stapler staple "$PKG"
  spctl --assess --type install --verbose "$PKG"
else
  echo "Not notarized (set NOTARY_PROFILE and have a Developer ID Installer identity)"
fi

rm -rf "$ROOT" "$OUT/components.plist" "$OUT/distribution.xml" "$OUT/NepaliKeyboard-component.pkg"
shasum -a 256 "$PKG"
echo "Release: gh release create mac-v$VERSION \"$PWD/$PKG\" --title \"Mac $VERSION\" --latest=false"
