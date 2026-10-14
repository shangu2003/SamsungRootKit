#!/bin/bash
# Samsung Root Kit App —— macOS 一键构建脚本
# 用法: ./build.sh
# 输出: app/build/outputs/apk/debug/app-debug.apk

set -e

# 自动定位 JDK (Android Studio 自带的 JBR) 和 Android SDK
export JAVA_HOME="${JAVA_HOME:-/Applications/Android Studio.app/Contents/jbr/Contents/Home}"
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"

echo "JAVA_HOME = $JAVA_HOME"
echo "ANDROID_HOME = $ANDROID_HOME"
echo ""

cd "$(dirname "$0")"

# 赋予 gradlew 执行权限
chmod +x ./gradlew 2>/dev/null || true

# 构建 Debug APK (完全离线资源，联网仅用于下载一次 Gradle 依赖)
echo ">>> 开始构建 Debug APK ..."
./gradlew :app:assembleDebug --no-daemon "$@"

APK="app/build/outputs/apk/debug/app-debug.apk"
if [ -f "$APK" ]; then
    echo ""
    echo "✅ 构建成功!"
    echo "APK 路径: $(pwd)/$APK"
    echo "大小: $(du -h "$APK" | cut -f1)"
    echo ""
    echo "安装到手机: adb install -r \"$APK\""
else
    echo "❌ 未找到 APK，构建可能失败"
    exit 1
fi
