# Guía Paso a Paso para Machly

Esta guía te ayudará a poner en marcha la aplicación y solucionar errores comunes.

## 1. Solución al Error "Cannot resolve symbol 'main'"
El error que ves en la imagen ocurre porque **Android Studio generó un código por defecto** en `MainActivity.java` que busca una vista con id `main` (`R.id.main`), pero el archivo XML que generé (`activity_main.xml`) no tenía ese ID.

**Para arreglarlo, tienes dos opciones (haz ambas para asegurar):**

1.  **Reemplaza todo el código de `MainActivity.java`**: Copia el código que generé para `MainActivity.java` y pégalo sobre el archivo existente. Mi código **no** usa `R.id.main`, sino que configura la lista de usuarios y la sincronización. Si dejas el código por defecto de Android Studio, la app no hará nada.
2.  **Actualiza el XML (ya lo he hecho por ti)**: He actualizado `activity_main.xml` para incluir `android:id="@+id/main"` en la vista raíz. Esto hará que el error desaparezca incluso si usas el código por defecto.

## 2. Configuración Obligatoria

### URL del Servidor
1.  Abre el archivo: `app/src/main/java/com/machly/app/AppConstants.java`
2.  Busca: `public static final String BASE_URL = "https://YOUR_SOMEE_URL/";`
3.  Cambia `"https://YOUR_SOMEE_URL/"` por la URL real de tu API en Somee.
    *   Ejemplo: `public static final String BASE_URL = "http://machly-api.somee.com/";`
    *   **Nota**: Asegúrate de que termine en `/`.

### Sincronización de Gradle
1.  En Android Studio, verás una barra amarilla arriba o un elefante en la derecha.
2.  Haz clic en **"Sync Now"** o el botón de **"Sync Project with Gradle Files"**.
3.  Esto descargará las librerías necesarias (Retrofit, Gson, etc.).

## 3. Ejecución y Pruebas

### Primer Inicio (Online)
1.  Ejecuta la app en el emulador.
2.  Ingresa un email y contraseña.
    *   Si tu API ya tiene usuarios, usa uno válido.
    *   Si no, la app intentará loguearse contra el endpoint `/api/auth/login`.
3.  Al entrar, verás la lista de usuarios.

### Modo Offline
1.  Cierra la app.
2.  Pon el emulador en **Modo Avión**.
3.  Abre la app e intenta loguearte con las mismas credenciales.
4.  Deberías entrar y ver los datos guardados localmente.

### Exportar Base de Datos (Evidencia)
Para ver la base de datos SQLite:
1.  Abre la terminal en Android Studio (pestaña "Terminal" abajo).
2.  Ejecuta:
    ```bash
    adb exec-out run-as com.machly.app cat /databases/machly.db > machly.db
    ```
3.  Abre el archivo `machly.db` generado con el programa "DB Browser for SQLite".

## Resumen de Archivos Clave
Asegúrate de haber copiado el contenido de estos archivos correctamente:
-   `AppConstants.java` (Configuración)
-   `ApiClient.java` (Conexión)
-   `DbHelper.java` (Base de datos local)
-   `SyncManager.java` (Sincronización)
-   `MainActivity.java` (Pantalla principal)
-   `LoginActivity.java` (Pantalla de login)
