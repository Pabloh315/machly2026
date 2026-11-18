# Compilación de Machly Mobile

## Requisitos

- Android Studio Hedgehog o posterior.
- JDK 17 o JDK 21 (la validación se realizó con OpenJDK 21.0.12).
- Android SDK Platform 33 y Build Tools compatibles.
- Conexión a internet para resolver dependencias si no están en caché.

## Abrir y compilar

1. Abra la carpeta `mobile/` como proyecto Gradle en Android Studio.
2. Seleccione un JDK 17 o 21 para Gradle.
3. Configure la ubicación local del Android SDK desde Android Studio, o mediante `ANDROID_HOME`; no agregue `local.properties` al repositorio.
4. En Windows ejecute:

```powershell
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
cd mobile
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest
```

El APK de depuración se genera en `app/build/outputs/apk/debug/` y no se versiona.

## Configuración de API

La aplicación utiliza `AppConstants`: `10.0.2.2` para el emulador y una IP privada para un teléfono en la misma red. La dirección del teléfono debe actualizarse localmente antes de usar una API distinta. La conexión actual usa HTTP solo para la red de desarrollo; no debe considerarse adecuada para producción.
