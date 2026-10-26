# Machly 2.0 — guía local

Machly ahora es una plataforma completa de alquiler de maquinaria agrícola y vehículos. La computadora funciona como servidor, guarda la base de datos y atiende tanto a la web como a la aplicación Android.

## Iniciar la plataforma

1. Haz doble clic en `INICIAR_MACHLY.bat`.
2. Mantén abierta la ventana del servidor.
3. En la computadora abre `http://localhost:3000`.
4. En un teléfono conectado a la misma red Wi‑Fi abre `http://IP-DE-LA-PC:3000` o instala el APK y escribe esa dirección.

En esta computadora, la dirección Wi‑Fi detectada durante la instalación fue:

`http://192.168.100.25:3000`

La IP puede cambiar al reiniciar el router. Cada vez que Machly inicia, muestra las direcciones actuales en la ventana. Si Windows pregunta si Node.js puede comunicarse en la red, permite el acceso únicamente para **redes privadas**.

## Aplicación Android

El APK compilado está en:

`mobile/app/build/outputs/apk/debug/app-debug.apk`

Puedes copiarlo al teléfono e instalarlo, o conectar el teléfono por USB y ejecutar `INSTALAR_ANDROID.bat`. Android puede pedir permiso para instalar aplicaciones desde archivos.

Al abrir la app:

1. Escribe la dirección local mostrada por el servidor, por ejemplo `http://192.168.100.25:3000`.
2. Inicia sesión o crea una cuenta.
3. La app valida primero la sesión contra el servidor y luego abre la experiencia móvil completa.

Para el emulador de Android Studio usa `http://10.0.2.2:3000`.

## Cuentas iniciales

| Tipo | Correo | Contraseña |
|---|---|---|
| Administrador | `admin@machly.local` | `admin123` |
| Propietario | `propietario@machly.local` | `machly123` |
| Cliente | `cliente@machly.local` | `machly123` |

Cambia estas claves si usarás datos reales. Las contraseñas se guardan con hash `scrypt`; nunca se almacenan en texto plano.

## Funciones disponibles

- Registro e inicio de sesión para clientes y propietarios.
- Catálogo responsive con búsqueda, ubicación, fechas, categorías y precios.
- Disponibilidad en tiempo real y prevención de reservas solapadas.
- Cálculo de días, subtotal, tarifa de servicio, garantía y total.
- Favoritos y calificaciones.
- Panel del cliente con reservas y estados.
- Panel del propietario con publicaciones, solicitudes, aceptación, entrega y cierre.
- Alta y edición de maquinaria o vehículos.
- Base de datos SQLite persistente y API REST compartida.
- Aplicación web instalable (PWA) y APK Android autenticado.

## Datos y copias de seguridad

La base está en `data/machly.db`. Para una copia de seguridad:

1. Detén el servidor cerrando su ventana.
2. Copia `data/machly.db` a una ubicación segura.

Para empezar desde cero, con el servidor detenido, mueve ese archivo fuera de la carpeta. Al iniciar de nuevo se creará una base limpia con los datos de demostración.

## Comandos útiles

Desde esta carpeta:

```powershell
npm.cmd start
npm.cmd test
npm.cmd run check
.\gradlew.bat -p mobile :app:assembleDebug
```

## Arquitectura

- `server/`: API, seguridad y migraciones SQLite.
- `public/`: aplicación web responsive y PWA.
- `mobile/`: aplicación Android 2.0 con autenticación nativa.
- `tests/`: pruebas integrales de la API.
- `data/`: base SQLite local creada al iniciar.

La carpeta `app/` pertenece al prototipo Android anterior y se conserva como referencia. La versión móvil entregable es `mobile/`.

## API principal

- `POST /api/auth/register`, `POST /api/auth/login`, `GET /api/auth/me`
- `GET/POST/PUT/DELETE /api/equipment`
- `GET/POST /api/bookings`, `PATCH /api/bookings/:id/status`
- `GET/POST/DELETE /api/favorites`
- `POST /api/reviews`
- `GET /api/dashboard`, `GET /api/health`

Todas las operaciones privadas requieren un token firmado y además se vuelven a autorizar en el servidor según el propietario o arrendatario correspondiente.
