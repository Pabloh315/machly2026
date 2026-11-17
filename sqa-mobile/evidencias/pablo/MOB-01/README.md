# MOB-01 — Registro de estructura y compilación

- Proyecto incorporado: `mobile/` (Gradle, módulo `app`).
- Identificador de aplicación: `com.machly.app`.
- SDK de compilación: Android API 33; mínimo API 21; destino API 33.
- Gradle Wrapper: 8.13; Android Gradle Plugin: 8.1.0.
- Java detectado: OpenJDK 21.0.12 LTS.
- Dependencias verificadas: Retrofit 2.9.0, OkHttp logging-interceptor 4.11.0 y Gson 2.10.1.

## Resultado real

`assembleDebug` se intentó inicialmente sin una ruta de SDK configurada y falló. Tras configurar `ANDROID_HOME` de forma temporal para la ejecución, finalizó correctamente el 2026-11-17. El resultado completo y la huella SHA-256 del APK están en los archivos de esta carpeta.

La compilación emite advertencias por compatibilidad de código Java 8 y uso de API deprecada; se registran como hallazgos pendientes de auditoría, sin alterar la configuración funcional entregada.
