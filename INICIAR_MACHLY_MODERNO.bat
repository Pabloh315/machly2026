@echo off
title Machly - Plataforma moderna
cd /d "%~dp0"
echo.
echo ========================================
echo       MACHLY - DISENO AGRICOLA
echo ========================================
echo.
if not exist node_modules (
  echo Instalando componentes por primera vez...
  call npm.cmd install
  if errorlevel 1 goto error
)
echo Abriendo servidor local en el puerto 3000...
echo No cierres esta ventana mientras uses Machly.
echo.
node server\modern-index.js
if errorlevel 1 goto error
goto end
:error
echo.
echo No se pudo iniciar. Si el puerto 3000 esta ocupado,
echo cierra la otra ventana de Machly e intenta nuevamente.
pause
:end
