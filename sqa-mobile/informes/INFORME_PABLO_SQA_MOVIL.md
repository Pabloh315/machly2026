# Informe SQA móvil — Pablo Hugo Rocabado

## Alcance y resultados

| Tarea | Estado | Resultado real |
|---|---|---|
| MOB-01 | Verificado | `assembleDebug` exitoso; APK de depuración generado. |
| MOB-02 | Condicionado | Pruebas instrumentadas compiladas; no ejecutadas: no había dispositivo/emulador conectado. |
| MOB-03 | Verificado | `testDebugUnitTest`: 9 pruebas, 9 aprobadas, 0 fallidas. |
| MOB-04 | Verificado | Auditoría, métricas y evidencias consolidadas. |

## Auditoría

| Hallazgo | Estado |
|---|---|
| AI-01 CI/CD ausente | Abierto; fuera del alcance de Pablo. |
| AI-02 pruebas de negocio ausentes | Corregido parcialmente: 9 pruebas unitarias de reservas y sesión. |
| AI-03 paquete instrumentado inconsistente | Corregido: `com.machly.app`. |
| AI-04 IP fija/documentación inconsistente | Abierto: IP privada en `AppConstants`; requiere configuración por entorno. |
| AI-05 HTTP sin cifrar | Abierto: permitido para red local de desarrollo. |
| AI-06 credenciales de demostración | Abierto; presentes en recursos de la app. |
| AI-07 historial de defectos ausente | Abierto; este informe inicia el registro SQA móvil. |
| AI-08 Newman/Playwright pendiente | Abierto; fuera del alcance móvil de Pablo. |
| AI-09 API/Java deprecados | Abierto: compilación advierte Java 8 y API deprecada. |

## Recomendación

Liberación **condicionada**: la compilación y pruebas unitarias pasan; antes de producción se requiere instrumentación en emulador/dispositivo, HTTPS, configuración de URL por entorno y actualización de compatibilidad Java/API.

Las evidencias están bajo `sqa-mobile/evidencias/pablo/`.
