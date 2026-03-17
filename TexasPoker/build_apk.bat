@echo off
chcp 65001 >nul
echo ============================================
echo   Texas Poker APK 构建脚本
echo ============================================
echo.

:: 检查Java环境
echo [1/4] 检查Java环境...
java -version 2>&1
if %errorlevel% neq 0 (
    echo [错误] 未找到Java，请先安装JDK 17
    echo 下载地址: https://adoptium.net/
    pause
    exit /b 1
)

:: 检查Android SDK
echo [2/4] 检查Android SDK...
if "%ANDROID_HOME%"=="" (
    echo [警告] ANDROID_HOME 未设置
    echo 请安装Android Studio并配置SDK路径
)

:: 运行Gradle构建
echo [3/4] 开始Gradle构建...
call gradlew.bat assembleDebug --stacktrace

if %errorlevel% neq 0 (
    echo.
    echo [错误] 构建失败，请检查错误信息
    pause
    exit /b 1
)

:: 查找APK
echo [4/4] 查找生成的APK...
for /f "tokens=*" %%i in ('dir /s /b "app\build\outputs\apk\debug\*.apk" 2^>nul') do (
    echo [成功] APK位置: %%i
    copy "%%i" "TexasPoker-debug.apk"
    echo [成功] 已复制到: TexasPoker-debug.apk
)

echo.
echo ============================================
echo   构建完成！
echo   APK文件: TexasPoker-debug.apk
echo ============================================
pause
