@echo off
title Machly - Instalar Android
cd /d "%~dp0"
set "APK=mobile\app\build\outputs\apk\debug\app-debug.apk"
if not exist "%APK%" (
  echo El APK no existe. Ejecuta: gradlew.bat -p mobile :app:assembleDebug
  pause
  exit /b 1
)
where adb >nul 2>nul
if errorlevel 1 (
  echo ADB no esta disponible en PATH.
  echo Instala manualmente este archivo en el telefono:
  echo %CD%\%APK%
  pause
  exit /b 1
)
echo Conecta el telefono por USB y acepta la depuracion USB.
adb install -r "%APK%"
pause
