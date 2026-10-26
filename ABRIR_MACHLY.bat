@echo off
setlocal
title Iniciando Machly
cd /d "%~dp0"

echo.
echo ========================================
echo          ABRIENDO MACHLY
echo ========================================
echo.

if not exist node_modules\express (
  echo Instalando componentes locales...
  call npm.cmd install
  if errorlevel 1 goto error
)

powershell -NoProfile -Command "try { $r=Invoke-RestMethod -TimeoutSec 2 http://localhost:3000/api/ui-version; if($r.ui -eq 'machly-agro'){exit 0}; exit 1 } catch { exit 1 }"
if errorlevel 1 (
  echo Iniciando el servidor local...
  start "Machly servidor" /min node server\reliable-index.js
  for /L %%i in (1,1,20) do (
    powershell -NoProfile -Command "try { $r=Invoke-RestMethod -TimeoutSec 1 http://localhost:3000/api/ui-version; if($r.ui -eq 'machly-agro'){exit 0}; exit 1 } catch { exit 1 }"
    if not errorlevel 1 goto ready
    timeout /t 1 /nobreak >nul
  )
  goto error
)

:ready
echo Machly esta listo. Abriendo el navegador...
start "" "http://localhost:3000/"
exit /b 0

:error
echo.
echo No se pudo iniciar Machly.
echo Si ya existe otra ventana de Machly, cierrala y ejecuta este archivo nuevamente.
pause
exit /b 1
