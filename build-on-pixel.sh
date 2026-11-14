#!/usr/bin/env bash
# Сборка APK прямо в Linux-терминале Pixel (Debian, arm64). Запускай из папки проекта.
set -e
PROJ="$(cd "$(dirname "$0")" && pwd)"

echo "== 1/5 Пакеты =="
sudo apt update
sudo apt install -y wget unzip aapt
sudo apt install -y openjdk-17-jdk-headless || sudo apt install -y openjdk-21-jdk-headless

AAPT2="$(command -v aapt2 || true)"
if [ -z "$AAPT2" ]; then
  echo "Не нашёл aapt2 в Debian-репозитории. Напиши Claude, придумаем обход."; exit 1
fi
echo "aapt2: $AAPT2"

echo "== 2/5 Android SDK =="
export ANDROID_HOME="$HOME/android-sdk"
if [ ! -x "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" ]; then
  mkdir -p "$ANDROID_HOME/cmdline-tools"
  wget -q --show-progress -O /tmp/cmdtools.zip \
    https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
  unzip -q /tmp/cmdtools.zip -d "$ANDROID_HOME/cmdline-tools"
  mv "$ANDROID_HOME/cmdline-tools/cmdline-tools" "$ANDROID_HOME/cmdline-tools/latest"
fi
yes | "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" --licenses >/dev/null || true
"$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" "platforms;android-34" "build-tools;34.0.0"
echo "sdk.dir=$ANDROID_HOME" > "$PROJ/local.properties"

echo "== 3/5 Gradle 8.7 =="
if [ ! -x "$HOME/gradle-8.7/bin/gradle" ]; then
  wget -q --show-progress -O /tmp/gradle.zip https://services.gradle.org/distributions/gradle-8.7-bin.zip
  unzip -q /tmp/gradle.zip -d "$HOME"
fi

echo "== 4/5 Сборка (первый раз долго, 10-30 минут) =="
cd "$PROJ"
"$HOME/gradle-8.7/bin/gradle" assembleDebug --no-daemon \
  -Pandroid.aapt2FromMavenOverride="$AAPT2"

APK="$PROJ/app/build/outputs/apk/debug/app-debug.apk"
echo "== 5/5 Готово: $APK =="
if [ -d /mnt/shared ]; then
  cp "$APK" /mnt/shared/ascii-wallpaper.apk && echo "Скопировал в общую папку (Downloads): ascii-wallpaper.apk"
else
  echo "Общей папки /mnt/shared нет, забери APK вручную."
fi
