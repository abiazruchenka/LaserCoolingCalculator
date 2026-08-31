#!/usr/bin/env bash
# Builds a double-clickable macOS app with a bundled Java runtime.
# Must be run on the Mac that will use it (Apple Silicon vs Intel).
set -euo pipefail

cd "$(dirname "$0")"

if [[ "$(uname -s)" != Darwin ]]; then
  echo "Build the .app on this Mac, not on Windows or Linux."
  exit 1
fi

if [[ -z "${JAVA_HOME:-}" ]]; then
  if [[ -x /usr/libexec/java_home ]]; then
    JAVA_HOME="$(/usr/libexec/java_home 2>/dev/null || true)"
  fi
  if [[ -z "${JAVA_HOME}" && -d /Library/Java/JavaVirtualMachines/temurin-25.jdk/Contents/Home ]]; then
    JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-25.jdk/Contents/Home
  fi
fi

if [[ -z "${JAVA_HOME:-}" || ! -x "${JAVA_HOME}/bin/jpackage" ]]; then
  echo "Need JDK 21+ with jpackage. Install Temurin and set JAVA_HOME."
  exit 1
fi

export JAVA_HOME
export PATH="${JAVA_HOME}/bin:${PATH}"

APP_NAME="LaserCooling"
VERSION="$(grep -m1 '<version>' pom.xml | sed -E 's/.*<version>([^<]+)<\/version>.*/\1/')"
APP_VERSION="${VERSION%-SNAPSHOT}"
RUNTIME_IMAGE="target/app"
DEST="target/dist"

echo "JDK: ${JAVA_HOME}"
echo "Version: ${VERSION} (package ${APP_VERSION})"
java -version

./mvnw -DskipTests javafx:jlink

if [[ ! -d "${RUNTIME_IMAGE}" ]]; then
  echo "jlink did not create ${RUNTIME_IMAGE}"
  exit 1
fi

rm -rf "${DEST}"
mkdir -p "${DEST}"

jpackage \
  --type app-image \
  --name "${APP_NAME}" \
  --app-version "${APP_VERSION}" \
  --vendor IPG \
  --dest "${DEST}" \
  --runtime-image "${RUNTIME_IMAGE}" \
  --module ipg.cooling/ipg.cooling.Launcher \
  --mac-package-identifier ipg.cooling \
  --mac-package-name "${APP_NAME}"

jpackage \
  --type dmg \
  --name "${APP_NAME}" \
  --app-version "${APP_VERSION}" \
  --vendor IPG \
  --dest "${DEST}" \
  --runtime-image "${RUNTIME_IMAGE}" \
  --module ipg.cooling/ipg.cooling.Launcher \
  --mac-package-identifier ipg.cooling \
  --mac-package-name "${APP_NAME}"

echo
echo "Done:"
echo "  ${DEST}/${APP_NAME}.app"
echo "  ${DEST}/${APP_NAME}-${APP_VERSION}.dmg"
echo
echo "First launch: right-click the .app → Open (Gatekeeper)."
echo "Or: xattr -cr \"${DEST}/${APP_NAME}.app\" && open \"${DEST}/${APP_NAME}.app\""
