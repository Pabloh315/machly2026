# MOB-02 — Corrección de prueba instrumentada

## Inconsistencia corregida

La prueba entregada verificaba `com.machly`, mientras que `mobile/app/build.gradle` declara `applicationId "com.machly.app"`. Se actualizó la aserción al identificador configurado.

## Comprobación adicional

Se añadió una segunda prueba que obtiene recursos del contexto destino y verifica que el nombre de aplicación es `Machly`. Ambas pruebas usan `AndroidJUnitRunner`, configurado en `defaultConfig`, y las dependencias de JUnit/Espresso ya declaradas en el módulo.

La ejecución en dispositivo se registra en `connectedDebugAndroidTest.log`.
