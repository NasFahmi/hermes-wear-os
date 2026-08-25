@echo off
setlocal

echo ========================================================
echo   Hermes Wear OS - Recompile dan Run
echo ========================================================
echo.

if exist "C:\Program Files\Eclipse Adoptium\jdk-17.0.16.8-hotspot" (
    set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.16.8-hotspot"
    set "PATH=C:\Program Files\Eclipse Adoptium\jdk-17.0.16.8-hotspot\bin;%PATH%"
) else if exist "C:\Program Files\Android\Android Studio\jbr" (
    set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
    set "PATH=C:\Program Files\Android\Android Studio\jbr\bin;%PATH%"
) else if exist "C:\Program Files\Eclipse Adoptium\jdk-21.0.8.9-hotspot" (
    set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.8.9-hotspot"
    set "PATH=C:\Program Files\Eclipse Adoptium\jdk-21.0.8.9-hotspot\bin;%PATH%"
)

set "ADB=%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe"
set "EMULATOR=%LOCALAPPDATA%\Android\Sdk\emulator\emulator.exe"
set "APK_PATH=%~dp0app\build\outputs\apk\debug\app-debug.apk"

echo [1/4] Mengompilasi kode terbaru (Gradle assembleDebug)...
call gradlew.bat assembleDebug
if errorlevel 1 goto :build_error

echo.
echo [2/4] Memeriksa status emulator...
"%ADB%" get-state >nul 2>&1
if %ERRORLEVEL% EQU 0 goto :emulator_ready

echo Menyalakan Emulator Wear OS...
start "" "%EMULATOR%" -avd Wear_OS_Large_Round
echo Menunggu emulator terkoneksi...
"%ADB%" wait-for-device
echo Menunggu sistem Android siap...
"%ADB%" wait-for-device shell "while [[ -z $(getprop sys.boot_completed) ]]; do sleep 1; done"

:emulator_ready
echo Emulator sudah siap!

echo.
echo [3/4] Menginstall APK terbaru ke jam tangan...
"%ADB%" install -r "%APK_PATH%"

echo.
echo [4/4] Membuka aplikasi Hermes di jam tangan...
"%ADB%" shell am start -n com.hermes.wearos/.presentation.MainActivity

echo.
echo ========================================================
echo   Sukses! Perubahan terbaru sudah berjalan di Emulator.
echo ========================================================
echo.
pause
exit /b 0

:build_error
echo.
echo [ERROR] Kompilasi gagal! Periksa error di atas.
pause
exit /b 1
