# Plan operativo de pruebas para Machly

**Estado:** Planificado - no ejecutado  
**Proyecto:** Machly Android App  
**Propósito:** indicar qué pruebas se realizarán, dónde se realizarán y qué evidencia deberá guardarse. Este documento complementa `PLAN_DE_TESTEO_SQA.md`.

## 1. Dónde se realizarán las pruebas

| Lugar o entorno | Uso previsto | Pruebas que se realizarán | Condición necesaria |
|---|---|---|---|
| Computadora de desarrollo del equipo | Preparar la versión de prueba y ejecutar pruebas unitarias posteriores. | Reglas de registro, validaciones y pruebas de integración locales. | Android Studio/JDK/SDK configurados y versión identificada. |
| Emulador Android API 24 | Verificar compatibilidad mínima configurada por la app. | Login, lista, alta, edición, SQLite y navegación básica. | Imagen de emulador y APK de prueba. |
| Emulador Android de versión intermedia | Detectar diferencias entre versiones Android. | Humo, sincronización, interfaz y modo offline. | Versión intermedia acordada por el equipo. |
| Emulador o dispositivo Android API 36 | Verificar el comportamiento en la versión objetivo del proyecto. | Todos los flujos P0, accesibilidad y compatibilidad. | Dispositivo/emulador disponible. |
| Ambiente sandbox de la API ASP.NET Core | Probar comunicación remota sin usar producción. | Login online, roles, usuarios, errores HTTP y sincronización. | URL real, contrato API y credenciales de prueba. |
| Base SQLite dentro del emulador/dispositivo | Revisar los datos locales que usa la app. | Usuarios, roles, correo único, relación usuario-rol, actualización y migración. | Datos de prueba; exportación controlada y sin secretos. |
| Herramienta de gestión (TestLink) | Registrar y controlar la ejecución. | Casos, resultados, defectos, trazabilidad y métricas. | Instancia disponible y cuentas del equipo. |
| Entorno de automatización posterior | Ejecutar pruebas repetibles en Android. | Unitarias con JUnit; instrumentadas/E2E con Espresso o Selenium mediante Appium. | Herramientas instaladas, emulador, APK y datos de prueba. |

**No se realizarán pruebas directamente en producción.** No se usarán datos personales ni contraseñas reales. Si no existe sandbox de la API, las pruebas de red quedan bloqueadas hasta que el equipo entregue un entorno autorizado.

## 2. Qué se va a probar

### 2.1 Inicio de sesión y sesión

**Dónde:** emuladores/dispositivo Android y API sandbox.

1. Abrir la aplicación y comprobar que aparece la pantalla de login.
2. Probar email o contraseña vacíos.
3. Probar credenciales válidas contra la API sandbox.
4. Probar credenciales inválidas y errores de red.
5. Confirmar que el usuario autenticado queda disponible localmente.
6. Probar acceso offline con un usuario que ya inició sesión en línea.
7. Probar acceso offline con contraseña incorrecta y con correo no guardado.
8. Validar el comportamiento de token vencido cuando se acuerde la regla con el equipo/API.

**Evidencia:** capturas, resultado en TestLink, respuesta HTTP sin secretos y exportación SQLite redactada cuando sea necesaria.

### 2.2 Sincronización de usuarios y roles

**Dónde:** API sandbox, emulador/dispositivo y SQLite local.

1. Sincronizar roles y luego usuarios tras el login.
2. Confirmar que cada usuario recibido queda asociado a un rol local válido.
3. Probar fallo al descargar roles.
4. Probar fallo al descargar usuarios después de obtener roles.
5. Desconectar y reconectar red mientras se abre la lista.
6. Ejecutar refresco manual desde la lista.
7. Confirmar que no se duplican usuarios o roles después de reconectar.
8. Medir el tiempo de sincronización solo cuando se definan volumen de datos y tiempo objetivo.

**Evidencia:** respuestas API redactadas, captura de lista, registro de eventos sin contraseñas/tokens y comparación de base de datos antes/después.

### 2.3 Base de datos local SQLite

**Dónde:** base SQLite del emulador/dispositivo de prueba.

1. Revisar que se creen las tablas de usuarios y roles.
2. Confirmar que un usuario guardado posee nombre, correo, hash de contraseña y rol local.
3. Confirmar que no existan dos usuarios con el mismo correo.
4. Comprobar que la relación usuario-rol se mantiene tras sincronizar y editar.
5. Probar el comportamiento ante actualización de la versión de la base de datos, después de acordar si los datos deben migrarse o avisarse antes de eliminarse.

**Evidencia:** copia de SQLite de prueba, consultas/capturas de inspección y comparación antes/después. No se adjuntarán claves, hashes ni tokens en texto visible.

### 2.4 Registro, lista, detalle y edición de usuarios

**Dónde:** emulador/dispositivo Android con SQLite y roles de prueba cargados.

1. Probar lista vacía y lista con usuarios.
2. Abrir el detalle de un usuario desde la lista.
3. Registrar un usuario con nombre, correo válido, contraseña de 8 caracteres o más y rol.
4. Intentar registro con nombre vacío o correo vacío.
5. Intentar registro con correo inválido.
6. Probar contraseña con 7 y con 8 caracteres.
7. Intentar registrar un correo existente y verificar que no cree ni modifique el usuario previo.
8. Intentar guardar sin rol disponible o seleccionado, una vez acordado el comportamiento esperado.
9. Editar nombre y rol dejando la contraseña vacía.
10. Editar la contraseña y comprobar el acceso offline con clave anterior y nueva, si el contrato del hash lo permite.
11. Volver al detalle y a la lista para confirmar que los datos editados se actualizan.

**Evidencia:** capturas de pantalla, video corto para recorridos, comparación de SQLite y resultado de cada caso en TestLink.

### 2.5 Interfaz, usabilidad, accesibilidad y compatibilidad

**Dónde:** emuladores Android API 24, intermedio y API 36; al menos pantalla pequeña y normal.

1. Verificar que login, lista, alta, edición y detalle no se superpongan ni oculten campos.
2. Usar teclado y lector de pantalla para revisar orden de foco, etiquetas y mensajes de error.
3. Revisar que los botones se puedan pulsar y los mensajes sean comprensibles.
4. Rotar el dispositivo durante alta/edición y registrar el comportamiento.
5. Volver atrás sin guardar, guardar dos veces y abrir estados de lista vacía.

**Evidencia:** matriz con dispositivo/versión/orientación, checklist de accesibilidad y capturas de cada hallazgo.

### 2.6 Seguridad básica

**Dónde:** código revisado, emulador/dispositivo de prueba y sandbox de API.

1. Revisar que contraseñas, hashes y token no se muestren en pantallas ni evidencias.
2. Confirmar con el responsable de API que el ambiente productivo usará HTTPS.
3. Revisar que los registros de red no guarden secretos en entregables de QA.
4. Revisar el comportamiento con token vencido y acceso no autorizado cuando el contrato esté disponible.
5. Registrar como riesgos de análisis estático: tráfico cleartext habilitado, `SharedPreferences` estándar y utilidad de hash basada en SHA-256.

**Evidencia:** checklist de seguridad, hallazgos con archivo/símbolo y capturas redactadas. No se realizará pentesting ni pruebas de ataque sin autorización explícita.

## 3. Orden de ejecución

| Etapa | Dónde se realiza | Actividades | Resultado esperado de la etapa |
|---|---|---|---|
| 1. Preparación | Computadora de desarrollo y TestLink | Confirmar copia del proyecto, build, dispositivos, sandbox, datos y criterios. | Ambiente listo o bloqueos registrados. |
| 2. Revisión | Reunión de equipo y documentos del repositorio | Acordar reglas de registro, token, sincronización, rol y migración. | Decisiones registradas. |
| 3. Humo | Emulador API 36 + sandbox | Abrir app, login válido, lista de usuarios y datos locales. | Decidir si la build es testeable. |
| 4. Integración | Sandbox + emulador + SQLite | Login, API, sincronización, modo offline e integridad local. | Casos P0 ejecutados y defectos registrados. |
| 5. Funcional/UI | Emuladores API 24/intermedio/36 | Alta, edición, lista, detalle, aceptación y accesibilidad. | Casos P0/P1 ejecutados. |
| 6. Corrección y regresión | Mismo ambiente de la falla | Repetir el caso corregido y los casos afectados. | Corrección verificada o defecto reabierto. |
| 7. Cierre | TestLink y documentos QA | Métricas, auditoría cruzada y riesgos pendientes. | Informe final basado en resultados reales. |

## 4. Distribución del trabajo

| Responsable | Dónde trabajará principalmente | Qué realizará |
|---|---|---|
| Integrante 1 | Sandbox API, emulador/dispositivo y SQLite | Login, sesión offline, sincronización, API, SQLite y seguridad básica. |
| Integrante 2 | Emuladores/dispositivos y SQLite | Registro, edición, lista, detalle, interfaz, accesibilidad y compatibilidad. |
| Ambos | TestLink, reunión de revisión y mismos emuladores | Humo, criterios pendientes, defectos P0, regresión, revisión cruzada y cierre. |

Alternativa para tres personas: Integrante 1 toma login/API/seguridad, Integrante 2 toma sincronización/SQLite/rendimiento e Integrante 3 toma usuarios/UI/accesibilidad. Cada integrante revisa una muestra de las evidencias de otro.

## 5. Precondiciones antes de comenzar

1. Confirmar que `Machly/app/` es la copia definitiva del proyecto a probar, pues existe una copia anidada adicional.
2. Definir una URL de API sandbox y no incluir contraseñas o tokens reales en el proyecto ni en evidencias.
3. Entregar usuarios y roles de prueba, por ejemplo Administrador y Consulta.
4. Acordar el criterio de token vencido, envío de autorización a API, sincronización parcial, rol obligatorio y migración de SQLite.
5. Disponer de emuladores API 24, una versión intermedia y API 36; o acordar una matriz menor si faltan recursos.
6. Crear el proyecto de TestLink e ingresar los casos del documento principal.
7. Identificar cada build y cada evidencia con fecha, versión, dispositivo, API Android y tipo de red.

## 6. Entregables de la ejecución posterior

- Casos y resultados registrados en TestLink.
- Capturas, videos cortos, reportes automáticos y logs redactados.
- Exportaciones controladas de SQLite para casos de persistencia.
- Registro de defectos con pasos, esperado, obtenido, entorno, severidad, prioridad y responsable.
- Informe de auditoría cruzada y métricas reales de avance, aprobados/fallidos y defectos.
- Actualización de `PLAN_DE_TESTEO_SQA.md` solo después de confirmar decisiones o resultados reales.

## 7. Restricciones

- Este plan no autoriza cambios de código, ejecución sobre producción, migraciones productivas ni pruebas ofensivas de seguridad.
- Las pruebas automatizadas se implementarán solamente en una instrucción posterior.
- Si no existe sandbox, no se debe reemplazar por un servicio público sin autorización del responsable de la API.
