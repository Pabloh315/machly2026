# Capturas pendientes

| Código | Captura requerida | Programa | Pasos exactos | Nombre de archivo | Estado |
|---|---|---|---|---|---|
| CAP-PABLO-01 | Compilación Gradle | Android Studio o PowerShell | Abrir `mobile/`; ejecutar `.\gradlew.bat clean; .\gradlew.bat testDebugUnitTest; .\gradlew.bat assembleDebug`; capturar `BUILD SUCCESSFUL`. | `CAP-PABLO-01-build-gradle.png` | Pendiente manual |
| CAP-PABLO-02 | Prueba instrumentada | Android Studio | Iniciar AVD, confirmar con `adb devices`, ejecutar `.\gradlew.bat connectedDebugAndroidTest`; capturar resultado real. | `CAP-PABLO-02-prueba-instrumentada.png` | Bloqueado: no hay dispositivo |
| CAP-PABLO-03 | Resultado JUnit | Navegador/Android Studio | Abrir `mobile/app/build/reports/tests/testDebugUnitTest/index.html`; capturar total, clases y resultados. | `CAP-PABLO-03-junit.png` | Pendiente manual |
| CAP-PABLO-04 | Tablero SQA | Navegador | Abrir `https://github.com/users/Pabloh315/projects/4`; mostrar tarjetas MOB-01 a MOB-04 y estado. | `CAP-PABLO-04-planner-sqa.png` | Pendiente manual |
