# Machly para Android

Aplicación Android nativa en Java para explorar equipos publicados en Machly y solicitar reservas desde la API compartida con la web. Los datos se leen y escriben en el servidor; la aplicación no copia la base SQLite del contenedor al teléfono.

## Conexión

- Teléfono conectado a la misma Wi-Fi: `http://10.26.6.56:3000/api/`
- Emulador Android Studio: `http://10.0.2.2:3000/api/`
- Documentación: `http://10.26.6.56:3000/api/docs`
- Salud del servidor: `http://10.26.6.56:3000/api/health`

La app detecta el emulador automáticamente. Si cambia la IP de la computadora, actualiza `PHONE_BASE_URL` en `app/src/main/java/com/machly/app/AppConstants.java`. El teléfono debe permanecer en la misma red Wi-Fi que el servidor.

## Funciones

- Catálogo y búsqueda de equipos desde `GET /equipment`.
- Detalle y disponibilidad desde `GET /equipment/{id}`.
- Inicio de sesión con `POST /auth/login`.
- Solicitud de reservas desde `POST /bookings`.
- Guardado de favoritos desde `POST /favorites/{equipmentId}`.
- Token Bearer y vencimiento de sesión.

Para probar el inicio de sesión, la pantalla incluye las credenciales de demostración: `cliente@machly.local` / `machly123`. Las operaciones privadas envían `Authorization: Bearer TOKEN`.

## Ejecutar

Abre este proyecto en Android Studio y ejecuta la configuración `app` en un emulador o teléfono conectado a la misma Wi-Fi del servidor. El wrapper usa Gradle 8.13: configura **Gradle JDK** como Java 17 o Java 21; Java 25 no es compatible con esta versión de Gradle. La API utiliza HTTP en esta red local; para un despliegue público se debe configurar HTTPS.
