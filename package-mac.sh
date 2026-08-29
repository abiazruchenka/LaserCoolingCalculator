#!/usr/bin/env bash
# Builds a double-clickable macOS app with a bundled Java runtime.
# Must be run on the Mac that will use it (Apple Silicon vs Intel).
set -euo pipefail

cd "$(dirname "$0")"

if [[ "$(uname -s)" != Darwin ]]; then
  echo "Сборку .app нужно делать на этом Mac, не на Windows/Linux."
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
  echo "Нужен JDK 21+ с jpackage. Установите Temurin и задайте JAVA_HOME."
  exit 1
fi

export JAVA_HOME
export PATH="${JAVA_HOME}/bin:${PATH}"

APP_NAME="LaserCooling"
VERSION="1.0"
RUNTIME_IMAGE="target/app"
DEST="target/dist"

echo "JDK: ${JAVA_HOME}"
java -version

./mvnw -DskipTests javafx:jlink

if [[ ! -d "${RUNTIME_IMAGE}" ]]; then
  echo "jlink не создал ${RUNTIME_IMAGE}"
  exit 1
fi

rm -rf "${DEST}"
mkdir -p "${DEST}"

jpackage \
  --type app-image \
  --name "${APP_NAME}" \
  --app-version "${VERSION}" \
  --vendor IPG \
  --dest "${DEST}" \
  --runtime-image "${RUNTIME_IMAGE}" \
  --module ipg.cooling/ipg.cooling.Launcher \
  --mac-package-identifier ipg.cooling \
  --mac-package-name "${APP_NAME}"

jpackage \
  --type dmg \
  --name "${APP_NAME}" \
  --app-version "${VERSION}" \
  --vendor IPG \
  --dest "${DEST}" \
  --runtime-image "${RUNTIME_IMAGE}" \
  --module ipg.cooling/ipg.cooling.Launcher \
  --mac-package-identifier ipg.cooling \
  --mac-package-name "${APP_NAME}"

echo
echo "Готово:"
echo "  ${DEST}/${APP_NAME}.app"
echo "  ${DEST}/${APP_NAME}-${VERSION}.dmg"
echo
echo "Первый запуск: правый клик по .app → Открыть (Gatekeeper)."
echo "Или: xattr -cr \"${DEST}/${APP_NAME}.app\" && open \"${DEST}/${APP_NAME}.app\""
