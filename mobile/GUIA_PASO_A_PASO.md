# Guía para ejecutar Machly en Android

## 1. Comprueba el servidor

La computadora debe tener Machly encendido en el puerto `3000`. Desde el teléfono, conectado a la misma Wi-Fi, abre `http://10.26.6.56:3000/api/health`. Debe responder con estado `ok`.

- Teléfono: `http://10.26.6.56:3000/api/`
- Emulador Android Studio: `http://10.0.2.2:3000/api/`

La app selecciona automáticamente la dirección del emulador o la del teléfono. Si cambia la IP de la computadora, actualiza `PHONE_BASE_URL` en `app/src/main/java/com/machly/app/AppConstants.java`.

## 2. Ejecuta la aplicación

Abre el proyecto en Android Studio y ejecuta la configuración `app`. Para sincronizar Gradle 8.13, configura **Gradle JDK** como Java 17 o Java 21; Java 25 no es compatible. La pantalla inicial muestra los equipos de la API. Puedes buscar, abrir un equipo y consultar sus datos sin iniciar sesión.

Para solicitar una reserva o guardar un favorito, inicia sesión con:

- Correo: `cliente@machly.local`
- Contraseña: `machly123`

La API valida las fechas disponibles y calcula los importes de la reserva.

## 3. Si no carga el catálogo

1. Verifica que `http://10.26.6.56:3000/api/health` responda `ok` desde el mismo teléfono.
2. Confirma que el teléfono y la computadora estén conectados a la misma Wi-Fi.
3. Si cambió la dirección IP de la computadora, reemplázala en `PHONE_BASE_URL`.
4. Revisa que el firewall permita conexiones entrantes al puerto TCP `3000`.
