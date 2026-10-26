# API móvil vigente de Machly

Esta es la referencia de conexión actual para que la aplicación móvil use la misma base de datos que la web.

## URL base

- Teléfono físico conectado a la misma Wi-Fi: `http://10.26.6.56:3000/api/`
- Emulador de Android Studio: `http://10.0.2.2:3000/api/`
- Verificación: `GET http://10.26.6.56:3000/api/health`
- Documentación navegable: `http://10.26.6.56:3000/api/docs`
- Especificación OpenAPI: `http://10.26.6.56:3000/api/openapi.yaml`

La IP de la computadora puede cambiar al conectarse a otra red. En ese caso, reemplaza `10.26.6.56` por la dirección IPv4 actual del adaptador Wi-Fi. El puerto y el sufijo `/api/` no cambian.

## Autenticación

Inicia sesión con `POST /api/auth/login` y envía el token en las operaciones privadas:

```http
Authorization: Bearer TOKEN_RECIBIDO
Content-Type: application/json
```

Ejemplo de acceso:

```json
{
  "email": "cliente@machly.local",
  "password": "machly123"
}
```

## Retrofit

```kotlin
val retrofit = Retrofit.Builder()
    .baseUrl("http://10.26.6.56:3000/api/")
    .addConverterFactory(GsonConverterFactory.create())
    .build()
```

La URL de Retrofit debe terminar en `/`. Para desarrollo local por HTTP, Android también necesita:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<application android:usesCleartextTraffic="true" ... />
```

## Regla de integración

La aplicación móvil nunca debe abrir o copiar SQLite. Debe consumir esta API. El contenedor guarda los datos en el volumen persistente `machly_data`, por lo que la web y todos los teléfonos comparten la misma información.

Consulta las operaciones y modelos completos en [`api/openapi.yaml`](api/openapi.yaml) y la guía ampliada en [`API_MOVIL.md`](API_MOVIL.md).
