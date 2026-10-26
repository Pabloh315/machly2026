@echo off
title Machly - Diseno agronomico final
cd /d "%~dp0"
echo.
echo ========================================
echo       MACHLY - VERSION FINAL
echo ========================================
echo.
if not exist node_modules (
  echo Instalando componentes por primera vez...
  call npm.cmd install
  if errorlevel 1 goto error
)
echo Iniciando en http://localhost:3000
echo No cierres esta ventana mientras uses Machly.
echo.
node server\polished-index.js
if errorlevel 1 goto error
goto end
:error
echo.
echo No se pudo iniciar. Cierra cualquier otra ventana de Machly
echo que esté usando el puerto 3000 y vuelve a intentarlo.
pause
:end
