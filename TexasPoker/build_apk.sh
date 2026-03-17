#!/bin/bash
echo "============================================"
echo "  Texas Poker APK 构建脚本"
echo "============================================"

# 检查Java
echo "[1/4] 检查Java环境..."
java -version 2>&1
if [ $? -ne 0 ]; then
    echo "[错误] 未找到Java，请安装JDK 17"
    exit 1
fi

# 添加执行权限
chmod +x gradlew

# 构建Debug APK
echo "[2/4] 构建Debug APK..."
./gradlew assembleDebug --stacktrace

if [ $? -ne 0 ]; then
    echo "[错误] 构建失败"
    exit 1
fi

# 查找APK
APK=$(find app/build/outputs/apk/debug -name "*.apk" 2>/dev/null | head -1)
if [ -n "$APK" ]; then
    cp "$APK" "TexasPoker-debug.apk"
    echo "[成功] APK: TexasPoker-debug.apk"
fi

echo "============================================"
echo "  构建完成！"
echo "============================================"
