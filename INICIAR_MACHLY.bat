@echo off
title Machly - Servidor local
cd /d "%~dp0"
echo.
echo ========================================
echo       MACHLY - SERVIDOR LOCAL
echo ========================================
echo.
if not exist node_modules (
  echo Instalando componentes por primera vez...
  call npm.cmd install
  if errorlevel 1 goto error
)
echo Iniciando plataforma...
echo No cierres esta ventana mientras uses Machly.
echo.
call npm.cmd start
if errorlevel 1 goto error
goto end
:error
echo.
echo No se pudo iniciar Machly. Revisa el mensaje anterior.
pause
:end
