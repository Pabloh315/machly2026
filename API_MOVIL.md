# API Machly para la aplicación móvil

La aplicación móvil debe consumir la misma API que la web. No debe conectarse directamente al archivo SQLite: el contenedor es el único responsable de leer y escribir la base, aplicar permisos y evitar reservas solapadas.

## Direcciones

- Teléfono físico en la misma Wi‑Fi: `http://IP-DE-LA-COMPUTADORA:3000/api/`
- Dirección detectada actualmente: `http://192.168.100.25:3000/api/`
- Emulador Android Studio: `http://10.0.2.2:3000/api/`
- Web y documentación: `http://IP-DE-LA-COMPUTADORA:3000/api/docs`
- OpenAPI importable: `http://IP-DE-LA-COMPUTADORA:3000/api/openapi.yaml`
- Comprobación: `GET /api/health`

La IP puede cambiar. El teléfono y la computadora deben estar en la misma red y el firewall debe permitir el puerto TCP 3000 en redes privadas.

## Autenticación

1. Ejecutar `POST /api/auth/login`.
2. Guardar el valor `token` de la respuesta usando almacenamiento seguro del teléfono.
3. Enviar en cada ruta privada:

```http
Authorization: Bearer TOKEN_RECIBIDO
Content-Type: application/json
```

El token dura siete días. Si una solicitud devuelve `401`, la app debe limpiar la sesión y mostrar el inicio de sesión.

### Inicio de sesión

```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "cliente@machly.local",
  "password": "machly123"
}
```

Respuesta:

```json
{
  "token": "eyJ...",
  "expiresIn": 604800,
  "user": {
    "id": 3,
    "fullName": "Cliente de prueba",
    "email": "cliente@machly.local",
    "phone": "70000003",
    "role": "renter"
  }
}
```

## Ejemplo Retrofit para Android

```kotlin
data class LoginBody(val email: String, val password: String)
data class AuthResponse(val token: String, val expiresIn: Long, val user: UserDto)

interface MachlyApi {
    @POST("auth/login")
    suspend fun login(@Body body: LoginBody): AuthResponse

    @GET("equipment")
    suspend fun equipment(
        @Query("search") search: String? = null,
        @Query("type") type: String? = null,
        @Query("city") city: String? = null,
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null
    ): EquipmentPageDto

    @POST("bookings")
    suspend fun createBooking(
        @Header("Authorization") bearer: String,
        @Body body: BookingBody
    ): BookingDto

    @GET("bookings")
    suspend fun bookings(@Header("Authorization") bearer: String): List<BookingDto>
}

val retrofit = Retrofit.Builder()
    .baseUrl("http://192.168.100.25:3000/api/")
    .addConverterFactory(GsonConverterFactory.create())
    .build()
```

En Android, la URL base de Retrofit debe terminar en `/`.

Para desarrollo HTTP local, el manifiesto debe incluir permiso de Internet y tráfico local permitido:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<application android:usesCleartextTraffic="true" ... />
```

En producción debe usarse HTTPS y `usesCleartextTraffic="false"`.

## Operaciones principales

| Método | Ruta | Autenticación | Uso |
|---|---|---:|---|
| POST | `/api/auth/register` | No | Crear cuenta |
| POST | `/api/auth/login` | No | Iniciar sesión |
| GET | `/api/auth/me` | Sí | Validar sesión |
| GET | `/api/equipment` | Opcional | Buscar catálogo |
| GET | `/api/equipment/:id` | Opcional | Ver detalle y disponibilidad |
| POST | `/api/equipment` | Propietario | Publicar equipo |
| GET | `/api/my/equipment` | Propietario | Publicaciones propias |
| GET | `/api/bookings` | Sí | Reservas del cliente |
| GET | `/api/bookings?scope=owner` | Propietario | Solicitudes recibidas |
| POST | `/api/bookings` | Sí | Solicitar reserva |
| PATCH | `/api/bookings/:id/status` | Sí | Cambiar estado |
| GET | `/api/favorites` | Sí | Listar favoritos |
| POST | `/api/favorites/:equipmentId` | Sí | Añadir favorito |
| DELETE | `/api/favorites/:equipmentId` | Sí | Quitar favorito |
| POST | `/api/reviews` | Sí | Crear reseña |

## Reserva

```http
POST /api/bookings
Authorization: Bearer TOKEN
Content-Type: application/json

{
  "equipmentId": 1,
  "startDate": "2027-02-10",
  "endDate": "2027-02-12",
  "message": "Preparación de terreno para siembra"
}
```

El servidor calcula días, subtotal, tarifa de servicio, garantía y total. La app no debe confiar en importes calculados localmente. Si las fechas se solapan devuelve `409`.

## Base de datos compartida

Docker guarda SQLite en el volumen `machly_data`, montado en `/app/data`. Todos los clientes consumen la misma API y, por lo tanto, ven la misma información.

Nunca se debe copiar la base al teléfono ni exponer el volumen por red.
