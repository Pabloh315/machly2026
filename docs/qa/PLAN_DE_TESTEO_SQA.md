# Plan de Testeo SQA - Machly Android App

**Estado del documento:** Plan inicial diseñado - no ejecutado  
**Fecha de análisis:** 23 de octubre de 2026  
**Alcance de esta etapa:** inspección estática de código y documentación. No se compiló, instaló, ejecutó ni modificó la aplicación.

## 0. Convenciones, fuentes y límites

| Marca | Significado |
|---|---|
| **Hecho observado** | Evidencia directa en código o documentación del repositorio. |
| **Inferencia/riesgo** | Posible problema o comportamiento deducido estáticamente; requiere ejecución o revisión para confirmarse. |
| **Propuesta** | Decisión de pruebas o criterio sugerido que debe acordarse antes de usarlo como requisito. |

### Fuentes examinadas

- `README.md`, `GUIA_PASO_A_PASO.md` y `docs/ATDD_Registro_Usuario.md`.
- `docs/ATDD_Parte_Individual.docx` e `docs/Informe_Del_comportamiento_a_la_implementacion_corregido.docx`.
- Módulo Android `app`: manifiesto, Gradle, actividades, adaptador, modelos, API Retrofit, SQLite, sincronización, preferencias y pruebas existentes.

No se encontraron archivos de integración continua mediante la búsqueda estática. El directorio `Machly/` anidado contiene una segunda copia parcial del proyecto; este plan toma como línea base `app/` del proyecto que contiene `docs/`. Antes de ejecutar, el equipo debe confirmar cuál copia es entregable para evitar registrar evidencias sobre una versión distinta.

---

# Parte 1 - Bloque de ejecución para Integrante 1

## A. Resumen del análisis

### A.1 Qué hace el sistema

Machly es una aplicación Android nativa en Java para consultar y mantener localmente usuarios y roles. Inicia con autenticación contra una API ASP.NET Core, conserva un token en preferencias, sincroniza roles y usuarios con Retrofit, y permite continuar el acceso fuera de línea con datos SQLite previamente guardados. La interfaz muestra lista, detalle, alta y edición local de usuarios.

### A.2 Arquitectura y módulos identificados

| Módulo | Responsabilidad observada | Referencias principales |
|---|---|---|
| Inicio de sesión | Validación de campos no vacíos, llamada `POST`, persistencia del usuario autenticado y acceso offline. | `app/src/main/java/com/machly/app/ui/LoginActivity.java`, `api/ApiService.java` |
| API | Cliente Retrofit y contratos de login, usuarios y roles. | `api/ApiClient.java`, `api/ApiService.java`, `models/*Response.java` |
| Sincronización | Obtiene primero roles y luego usuarios; guarda resultados y marca última sincronización. | `sync/SyncManager.java`, `receivers/ConnectivityReceiver.java` |
| Persistencia local | SQLite con tablas `users` y `roles`, consultas, inserción/actualización y relación por rol local. | `db/DbHelper.java`, `models/User.java`, `models/Role.java` |
| Gestión de usuarios | Lista, detalle, alta y edición local. | `ui/MainActivity.java`, `ui/UserDetailActivity.java`, `ui/AddEditUserActivity.java`, `ui/adapters/UserAdapter.java` |
| Reglas de registro | Servicio de dominio que valida nombre, formato de correo, longitud mínima y correo duplicado. | `domain/registration/UserRegistrationService.java` |
| Seguridad y sesión | Red, hashing, token y preferencias. | `utils/NetworkUtils.java`, `utils/BCryptUtils.java`, `storage/PrefsManager.java` |

**Tecnologías observadas:** Android SDK (minSdk 24, target/compile SDK 36), Java 11, Gradle 8.13, Retrofit 2.9, Gson, OkHttp, SQLiteOpenHelper, Material Components, JUnit 4, AndroidX Test y Espresso. La configuración se encuentra en `app/build.gradle.kts` y `gradle/libs.versions.toml`.

### A.3 Documentación y pruebas existentes

- **Hecho observado:** hay checklist manual de primer inicio, modo offline, reconexión y CRUD en `README.md`.
- **Hecho observado:** hay criterios ATDD CA-01 y CA-02 para registro local, escenarios BDD y un documento académico de TDD en `docs/ATDD_Registro_Usuario.md` y los dos `.docx`.
- **Hecho observado:** existen `UserRegistrationServiceTest` (dos pruebas), `ExampleUnitTest` y `ExampleInstrumentedTest`.
- **Límite:** no se ejecutó ningún test. Los documentos que declaran resultados anteriores y la presencia de reportes/builds no demuestran que sean repetibles sobre esta línea base.
- **Hecho observado:** no se identificó pipeline CI ni plan de pruebas previo bajo `docs/qa/`.

### A.4 Información no confirmada

No están documentados ni fueron verificados: URL/ambiente real de API, contrato completo y códigos de error del servidor, semántica de `expiresIn`, usuarios/roles autorizados, SLA de sincronización, volumen esperado, navegadores/dispositivos objetivo, política de borrado y migración de datos, ni política de respaldo. Tampoco se confirma que la API entregue un `passwordHash` compatible con la comprobación offline.

### A.5 Áreas de mayor riesgo (análisis estático)

| Riesgo | Evidencia | Prioridad de validación |
|---|---|---|
| Criterios de registro no conectados a la pantalla | `AddEditUserActivity.saveUser()` solo exige nombre/correo y no invoca `UserRegistrationService`; `DbHelper.insertOrUpdateUser()` actualiza por correo existente. | P0 |
| Sesión y credenciales | `LoginActivity.onStart()` comprueba solo token no nulo; `PrefsManager` usa `SharedPreferences`; `BCryptUtils` implementa SHA-256 sin sal. | P0 |
| Exposición en red/registros | `usesCleartextTraffic="true"`, configuración de cleartext y `HttpLoggingInterceptor.Level.BODY`. | P0 |
| Integridad de sincronización | `SyncManager` anuncia transacción en comentario, pero persiste por operación; no borra registros remotos ausentes ni muestra autorización en peticiones. | P0 |
| Integridad en actualización | `DbHelper.onUpgrade()` elimina ambas tablas antes de recrearlas. | P1 |
| Consistencia de roles | `roleIdServer` no es único y el alta puede continuar sin una selección de rol efectiva si la lista está vacía. | P1 |
| Doble disparo de reconexión | Hay receptor declarado en manifiesto y también registrado en `MainActivity`. | P1 |

### A.6 Cobertura de este plan

Este plan cubre la aplicación Android incluida, sus contratos HTTP observables, persistencia local y criterios ATDD disponibles. No cubre internamente la API ASP.NET Core, su base de datos, servicios de terceros, infraestructura Somee ni seguridad ofensiva; solo define verificaciones de integración desde la app. La cobertura planificada no equivale a cobertura ejecutada, a ausencia de defectos ni a cumplimiento productivo.

## B. Alcance y prioridades

| ID | Módulo / funcionalidad | Rol | Fuente | Riesgo e impacto | Prioridad | Tipos aplicables | Alcance |
|---|---|---|---|---|---|---|---|
| F-AUTH-01 | Inicio de sesión online y sesión recordada | Usuario autenticado | `LoginActivity`, `ApiService.login` | Bloquea acceso y puede exponer credenciales/sesión. | P0 | Unit., integración, funcional, API, seguridad, E2E | Incluido |
| F-AUTH-02 | Inicio de sesión offline | Usuario previamente autenticado | `LoginActivity.performOfflineLogin`, README | Acceso incorrecto o imposibilidad de trabajar sin red. | P0 | Unit., integración, funcional, regresión | Incluido |
| F-SYNC-01 | Sincronizar roles y usuarios | Usuario autenticado | `SyncManager`, `ApiService` | Datos incompletos/inconsistentes afectan todas las pantallas. | P0 | Integración, API, BD, humo, rendimiento básico | Incluido |
| F-SYNC-02 | Actualizar por arranque, refresco y reconexión | Usuario autenticado | `MainActivity`, `ConnectivityReceiver` | Duplicados, carrera o datos obsoletos. | P0 | Funcional, E2E, exploratoria, regresión | Incluido |
| F-USER-01 | Listar y ver detalle de usuarios | Usuario autenticado | `MainActivity`, `UserAdapter`, `UserDetailActivity` | Información incorrecta o navegación fallida. | P1 | Funcional, UI, accesibilidad, compatibilidad | Incluido |
| F-USER-02 | Alta local de usuario | Administrador (según ATDD) | `AddEditUserActivity`, CA-01/CA-02 | Rompe criterios académicos e integridad de identidad. | P0 | Unit., integración, funcional, aceptación, BD, E2E | Incluido |
| F-USER-03 | Edición local de usuario | Administrador | `AddEditUserActivity` | Puede alterar contraseña, rol o identidad. | P1 | Integración, funcional, BD, regresión | Incluido |
| F-DATA-01 | Esquema y migración SQLite | Sistema | `DbHelper` | Pérdida de datos o referencias inválidas. | P0 | BD, integración, regresión | Incluido |
| F-SEC-01 | Token, contraseña, transporte y logs | Sistema | `PrefsManager`, `BCryptUtils`, manifiesto, `ApiClient` | Compromiso de cuentas y datos. | P0 | Seguridad básica, inspección | Incluido |
| F-UI-01 | Formularios, mensajes y accesibilidad | Usuario/Administrador | layouts XML y actividades UI | Errores de uso y baja accesibilidad. | P1 | UI, usabilidad, accesibilidad, compatibilidad | Incluido |
| F-PERF-01 | Tiempo de login/sincronización con datos representativos | Sistema | `SyncManager`, `ApiClient` | Degradación percibida; umbrales no definidos. | P2 | Rendimiento básico | Incluido condicionalmente |
| X-API-01 | Lógica interna, permisos y BD del servidor | N/D | API externa no presente | No hay código ni especificación suficiente. | P0 | Fuera de alcance de este repositorio | Excluido; coordinar con dueño API |
| X-PROD-01 | Pruebas de carga productivas, pentest y despliegue | N/D | Sin SLA ni entorno autorizado | Riesgo de impacto sobre terceros/producción. | P2 | No planificado en esta etapa | Excluido |

### Criterios de aceptación propuestos que requieren validación

Los siguientes son propuestas, no requisitos oficiales: **CP-01** expiración de token debe impedir acceso automático; **CP-02** toda petición protegida debe llevar autorización conforme a contrato API; **CP-03** alta y edición requieren rol válido; **CP-04** una sincronización fallida no debe dejar datos parcialmente actualizados; **CP-05** el tránsito productivo debe ser HTTPS y no registrar secretos; **CP-06** actualización de versión no debe borrar datos sin aceptación/migración. CA-01 y CA-02 son los únicos criterios de aceptación explícitamente documentados.

## C. Estrategia de pruebas de Parte 1

| Categoría | Objetivo, componentes y escenarios | Técnica | Modo / entorno / herramienta | Evidencia, prioridad y dependencias |
|---|---|---|---|---|
| Unitarias | Validar `UserRegistrationService`, hash/comparación y mapeos puros. Escenarios: nombre vacío, correo inválido, contraseña 7/8, correo repetido. | Particiones de equivalencia y valores límite. | Automatizada; JVM; JUnit. | Reporte JUnit y casos fuente. P0. Depende de datos/controladores falsos. |
| Integración | Verificar login→SQLite, API→mapeos→SQLite, roles→usuarios, edición→consulta. | Tablas de decisión y flujo de datos. | Automatizada/manual posterior; emulador y API mock/aislada; JUnit/Espresso/MockWebServer a evaluar. | Logs sanitizados, export de BD, reporte. P0. Depende de contrato API. |
| Funcionales | Confirmar recorridos observados: login, lista, detalle, alta/edición, offline y refresco. | Casos de uso y transiciones de estado conectado/desconectado. | Manual y automatización selectiva; APK de prueba, emulador/dispositivo. | Capturas/video, datos semilla y resultado en TestLink. P0/P1. |
| Aceptación | Comprobar CA-01 y CA-02: nombre, correo válido y único, contraseña >=8 y rol. | BDD Dado-Cuando-Entonces; decisiones de validación. | Manual primero; automatizable tras integrar la regla con formulario. | Aprobación de representante, escenarios y capturas. P0. Depende de confirmar rol administrador. |
| Regresión | Repetir humo, login offline y CRUD local después de cada corrección en autenticación, SQLite o sync. | Selección por riesgo y análisis de impacto. | Mixta; conjunto estable de casos. | Corrida vinculada al defecto y comparación. P0. |
| Exploratorias | Buscar estados no previstos: alternar red durante sync, volver atrás, reabrir app, rotar, listas vacías y respuestas incompletas. | Charter de exploración con timebox. | Manual; dispositivo físico y emulador. | Charter, notas, pasos reproducibles. P1. |
| Humo | Determinar si una build permite iniciar, presentar login, autenticar con ambiente válido y mostrar datos locales. | Checklist mínimo. | Manual/automatizada posterior. | Checklist y capturas. P0. Depende de build instalable/API configurada. |
| UI y E2E | Probar pantalla de login→lista→detalle→editar y alta→lista; comprobar estados vacío/carga/error. | Recorridos y transiciones. | Manual y automatización con Selenium vía Appium (viabilidad por validar) o Espresso. | Video/capturas y resultado de suite. P0/P1. |
| API | Validar formato, estado y mapeo de `POST /api/auth/login`, `GET /api/roles`, `GET /api/users`, `GET /api/users/{id}`. | Contrato, particiones HTTP y datos nulos/faltantes. | API aislada/sandbox; Postman u otro cliente acordado. | Colección, respuestas redactadas, contrato versionado. P0. Depende del dueño API. |
| Base de datos | Comprobar `NOT NULL`, unicidad de correo, relación usuario-rol, actualización, datos persistentes y upgrade. | Integridad referencial, transición de estado y límites. | Automatizada/manual; SQLite en emulador y export controlado. | Consultas, export sin secretos, capturas. P0. |
| Seguridad básica | Revisar transporte, secretos en logs, token, sesión expirada, permisos visibles y archivos de respaldo. | Checklist OWASP MASVS básico / revisión estática y pruebas negativas autorizadas. | Manual/revisión; dispositivo de prueba. | Checklist, evidencia redactada y hallazgos. P0. No incluye pentest. |
| Usabilidad y accesibilidad | Comprobar texto de errores, foco, teclado, contraste, etiquetas y tamaño táctil en login y alta. | Inspección heurística y navegación con lector de pantalla. | Manual; TalkBack y Android Accessibility Scanner si está disponible. | Checklist y capturas. P1. |
| Compatibilidad | Validar en Android API 24, una API intermedia y API 36; retrato, red wifi/datos y pantalla pequeña/normal. | Matriz de configuración. | Manual/automatizada selectiva; emuladores/dispositivo. | Matriz con versión, modelo y resultado. P1. |
| Rendimiento básico | Medir login y sync con conjunto pequeño, medio y máximo acordado; no afirmar umbrales aún. | Línea base y percentiles. | Manual/instrumentada posterior; red controlada. | Tiempos, tamaño de datos y ambiente. P2. Requiere volumen/SLO acordado. |

### Orden y datos de prueba de Parte 1

1. Confirmar URL de sandbox, contrato, credenciales de prueba y conjunto de roles/usuarios no productivos.
2. Ejecutar humo y contratos de login/roles/usuarios.
3. Ejecutar sincronización e integridad SQLite, incluidos fallos de roles, usuarios y red.
4. Ejecutar login offline con usuario previamente cacheado.
5. Ejecutar la validación de registro y los recorridos UI; abrir defectos y repetir regresión dirigida.

Datos mínimos: rol remoto `roleId=10, name=Administrador`; rol remoto `roleId=20, name=Consulta`; usuario `ana.perez@example.com`; usuario sin rol o con rol inexistente para prueba negativa de contrato; contraseña de prueba `ClaveSegura8`; y correo único `qa.nuevo+001@example.com`. No deben usarse credenciales reales ni datos personales reales.

## D. Catálogo de casos - Parte 1

Todos los casos de esta sección tienen estado inicial **Diseñado - no ejecutado**. La columna “Requisito” usa CA para criterios documentados, F para funcionalidad observada y CP para propuesta pendiente.

| ID | Módulo / requisito / objetivo | Tipo / técnica / prioridad | Precondición y rol | Datos y pasos | Resultado esperado verificable | Modo, evidencia y bloque |
|---|---|---|---|---|---|---|
| AUTH-01 | Login; F-AUTH-01. Rechazar campos vacíos. | Funcional; particiones; P0. | App en login; usuario. | A) email vacío, B) contraseña vacía. Pulsar LOGIN. | Se muestra `Please enter email and password`; no se solicita la API y no se navega. | Manual/UI. Captura y log de red. Parte 1. |
| AUTH-02 | Login online; F-AUTH-01. Persistir sesión y usuario autenticado. | Integración/API; flujo; P0. | Sandbox y credencial válida; usuario. | `ana.perez@example.com` / `ClaveSegura8`; iniciar sesión. | La respuesta exitosa permite abrir lista; se guarda token y existe el usuario local con rol asociado. | Manual/auto posterior. Captura, respuesta redactada, export BD. Parte 1. |
| AUTH-03 | Login online; F-AUTH-01. Manejar credenciales rechazadas. | API/funcional; particiones HTTP; P0. | API devuelve error de autenticación. | Correo válido + `ClaveInvalida8`; iniciar sesión. | No se abre lista; botón se rehabilita y se muestra `Login failed: <código>`. | Manual. Captura y código HTTP. Parte 1. |
| AUTH-04 | Login online; F-AUTH-01. Recuperar de fallo de transporte. | Integración; inyección de fallo; P0. | Red disponible sin respuesta del sandbox o mock timeout. | Iniciar con credenciales válidas. | Indicador de carga se oculta, botón queda habilitado y se muestra error; no se registra token nuevo. | Manual/auto posterior. Captura/log sanitizado. Parte 1. |
| AUTH-05 | Sesión; CP-01. Impedir acceso automático con token expirado. | Seguridad/funcional; transición de estado; P0. | Preferencias con token y expiración pasada; criterio CP-01 aprobado. | Abrir la app. | Se permanece en login, se invalida sesión y no se muestra lista. Si la semántica de expiración no se acuerda, registrar como bloqueado. | Manual/auto posterior. Captura y valor redactado. Parte 1. |
| AUTH-06 | Login offline; F-AUTH-02. Acceso cacheado válido. | Integración; transición conectado→offline; P0. | Usuario autenticado y guardado; modo avión; usuario. | `ana.perez@example.com` / contraseña compatible; iniciar. | Muestra `Offline mode` y abre lista con datos SQLite sin petición HTTP. | Manual. Video/captura y log red. Parte 1. |
| AUTH-07 | Login offline; F-AUTH-02. Rechazar usuario no cacheado y contraseña errónea. | Funcional; particiones; P0. | Modo avión; usuario. | A) `nuevo@example.com`; B) usuario cacheado + `MalaClave8`. | A muestra `You must login online the first time.`; B muestra `Invalid credentials`; no se abre lista. | Manual. Capturas. Parte 1. |
| SYNC-01 | Sincronización; F-SYNC-01. Mapear roles antes de usuarios. | Integración/API/BD; tabla de decisión; P0. | API devuelve 2 roles y 2 usuarios con roles válidos. | Login o refrescar; esperar callback. | `roles` contiene dos roles y cada usuario guardado tiene `roleIdLocal` que referencia un rol; lista muestra nombre de rol. | Manual/auto posterior. Respuestas redactadas, export BD, captura. Parte 1. |
| SYNC-02 | Sincronización; F-SYNC-01. Gestionar fallo al obtener roles. | Integración/API; transición de fallo; P0. | Mock/API retorna error en `GET /api/roles`. | Disparar sincronización. | Callback informa error; UI finaliza refresco con `Sync failed`; no se marca nueva última sincronización. | Manual/auto posterior. Código HTTP/log/captura. Parte 1. |
| SYNC-03 | Sincronización; CP-04. No dejar actualización parcial ante fallo de usuarios. | Integración/BD; atomicidad; P0. | Estado local conocido; roles OK y usuarios fallan; CP-04 aprobado. | Ejecutar sync. | Datos locales permanecen en estado previo completo y no se actualiza última sincronización. Si la política no se acuerda, registrar diferencia observada. | Auto posterior. Snapshot BD antes/después. Parte 1. |
| SYNC-04 | Reconexión; F-SYNC-02. Sincronizar al recuperar red sin duplicar. | E2E/exploratoria; transición de estado; P0. | Datos locales; red inicialmente desconectada. | Abrir lista, reconectar una vez y esperar. | Se inicia como máximo una sincronización lógica; no se duplican roles/usuarios y la lista se refresca. | Manual. Video, log y export BD. Parte 1. |
| SYNC-05 | Refresco manual; F-SYNC-02. Cerrar estado visual. | Funcional/UI; transición; P1. | Lista abierta; API exitosa y luego fallida. | Deslizar para refrescar en ambos escenarios. | Indicador se activa y se oculta en éxito/error; éxito muestra `Sync complete`, error `Sync failed`. | Manual. Capturas. Parte 1. |
| DB-01 | SQLite; F-DATA-01. Verificar estructura e integridad. | BD; restricciones; P0. | Instalación limpia y datos de prueba. | Guardar usuario con rol válido; inspeccionar BD. | `users` guarda campos no nulos previstos, correo único y `roleIdLocal` correspondiente; no hay fila sin rol válido si CP-03 se aprueba. | Manual/auto posterior. Export BD y consultas. Parte 1. |
| DB-02 | SQLite/registro; CA-02. Rechazar correo duplicado sin modificar usuario. | Integración/aceptación; tabla de decisión; P0. | Existe `ana.perez@example.com`; administrador. | Intentar alta con mismo correo y datos distintos. | No se crea ni modifica la fila existente y se informa `El correo electrónico ya está registrado.` | Manual/auto posterior. Captura y comparación BD. Parte 1. |
| DB-03 | Upgrade; CP-06. Preservar o migrar datos según decisión. | BD/regresión; transición de versión; P0. | Copia de BD v1 con usuarios/roles; política CP-06 aprobada. | Instalar build con incremento de versión. | Se cumple la política acordada (migración verificable o aviso/consentimiento explícito); nunca pérdida silenciosa. | Auto posterior. BD antes/después y versión. Parte 1. |
| SEC-01 | Seguridad; F-SEC-01. Ausencia de secretos en transporte/logs y almacenamiento. | Seguridad básica; checklist; P0. | Build de prueba, login controlado. | Inspeccionar logs de cliente, tráfico y preferencias; revisar cleartext. | No se exponen contraseña, token ni hash en evidencia; producción usa HTTPS y almacenamiento protegido conforme a CP-05. Hallazgo estático abierto hasta verificarse. | Revisión/manual. Checklist redactado. Parte 1. |
| API-01 | Contratos; F-AUTH-01/F-SYNC-01. Validar payloads y errores. | API; particiones HTTP/contrato; P0. | Especificación o contrato acordado; sandbox. | Ejecutar login, roles, usuarios y usuario por id con éxito, 401/403/404/5xx definidos. | Cada respuesta cumple esquema/código acordado y la app da la transición/mensaje acordado; discrepancias se registran. | Manual/auto posterior. Colección y respuestas redactadas. Parte 1. |

## E. ATDD, BDD y TDD - Parte 1

### ATDD

Se conservan CA-01 y CA-02 de `docs/ATDD_Registro_Usuario.md`. Antes de probar la pantalla, representante funcional, desarrollo y QA deben confirmar que “registro” significa alta local desde `AddEditUserActivity`, que el rol Administrador puede realizarla y que el mensaje exacto sigue vigente. Esto es importante porque el documento de análisis indica que la pantalla debe invocar la validación de dominio, mientras el código inspeccionado no muestra esa invocación.

### BDD propuesto

```gherkin
Característica: Alta local de usuario

Escenario: Alta local válida
  Dado que existe el rol "Administrador" y no existe "qa.nuevo+001@example.com"
  Cuando un administrador registra "Ana Pérez" con ese correo y "ClaveSegura8"
  Entonces se guarda exactamente un usuario local asociado al rol seleccionado
  Y se muestra "User saved locally"

Escenario: Correo repetido
  Dado que existe el usuario "ana.perez@example.com"
  Cuando un administrador intenta crear otro usuario con ese correo
  Entonces no se crea ni modifica ningún usuario
  Y se muestra "El correo electrónico ya está registrado."
```

### TDD futuro, sin atribución al código actual

Para corregir la integración de la alta, aplicar TDD sobre un adaptador/controlador de formulario que invoque `UserRegistrationService` y un `EmailRegistrationChecker` basado en `DbHelper`: **rojo**, escribir pruebas de correo inválido, longitud 7/8 y duplicado sin modificación; **verde**, integrar la mínima validación previa al guardado; **refactor**, aislar conversión de campos y mensajes. Las pruebas existentes prueban el servicio aislado; no demuestran por sí solas que el proceso TDD ocurriera ni que la UI cumpla CA-01/CA-02.

---

# Parte 2 - Bloque de ejecución para Integrante 2

## C. Estrategia de pruebas de Parte 2

Parte 2 aplica las mismas categorías y convenciones de la sección C, enfocándose en gestión visual de usuarios, reglas de alta/edición y experiencia de uso. Su orden depende de que Parte 1 disponga de roles y datos locales; puede usar datos SQLite preparados y, para casos puramente locales, trabajar en paralelo desde la preparación.

| Categoría priorizada | Componentes y foco | Técnica concreta | Dependencia |
|---|---|---|---|
| Aceptación y funcional | Alta/edición local y CA-01/CA-02. | Particiones, límites 7/8, tabla de decisión para duplicados. | Roles, criterio de administrador y decisión de integración del servicio. |
| UI/E2E | Lista, vacío, detalle, navegación y retorno tras edición. | Recorrido de usuario, transición de pantallas. | Datos locales de Parte 1 o fixture aprobado. |
| Usabilidad/accesibilidad | Error visible, foco, teclado, lector de pantalla y tamaños táctiles. | Checklist WCAG/Android reducido. | Build instalable. |
| Compatibilidad | API 24/intermedia/36, orientación y tamaños. | Matriz de equivalencia de dispositivos. | Emuladores/dispositivos disponibles. |
| Exploratoria/regresión | Flujo local sin rol, rotación, vuelta atrás, pulsaciones repetidas y edición. | Charter y regresión de impacto. | Resolución de defectos. |
| Seguridad básica | Confirmar edición sin exponer contraseña y restricción de correo al editar. | Revisión de datos visibles y pruebas negativas. | Datos no sensibles. |

## D. Catálogo de casos - Parte 2

| ID | Módulo / requisito / objetivo | Tipo / técnica / prioridad | Precondición y rol | Datos y pasos | Resultado esperado verificable | Modo, evidencia y bloque |
|---|---|---|---|---|---|---|
| USER-01 | Lista; F-USER-01. Mostrar estado vacío. | Funcional/UI; particiones; P1. | SQLite sin usuarios; usuario autenticado. | Abrir lista. | Se ve `No users found`; `RecyclerView` no se muestra. | Manual. Captura. Parte 2. |
| USER-02 | Lista; F-USER-01. Mostrar datos y rol. | Funcional/UI; equivalencia; P1. | SQLite con Ana/Administrador y Luis/Consulta. | Abrir lista. | Hay dos tarjetas con nombre, correo y rol correctos; no aparece `Unknown` cuando existe relación válida. | Manual/auto posterior. Captura y export BD. Parte 2. |
| USER-03 | Detalle; F-USER-01. Navegar desde tarjeta. | E2E; recorrido; P1. | Lista con Ana. | Pulsar tarjeta de Ana. | Detalle muestra exactamente nombre, correo y rol de la fila elegida y ofrece `Edit User`. | Manual. Video/captura. Parte 2. |
| USER-04 | Alta local; CA-01. Guardar datos válidos. | Aceptación/funcional; BDD; P0. | Rol Administrador cargado; correo no existente; administrador. | Nombre `Ana Pérez`, `qa.nuevo+001@example.com`, `ClaveSegura8`, rol Administrador; guardar. | Se crea una fila con esos nombre/correo/rol, contraseña no visible y mensaje `User saved locally`; al volver aparece en lista. | Manual/auto posterior. Captura y BD. Parte 2. |
| USER-05 | Alta; CA-01. Nombre/correo obligatorios. | Funcional; particiones; P0. | Formulario de alta; administrador. | A) nombre vacío/correo válido; B) nombre válido/correo vacío. Guardar. | No se persiste fila; se muestra `Name and Email are required`. | Manual. Capturas y BD. Parte 2. |
| USER-06 | Alta; CA-01. Rechazar correo de formato inválido. | Aceptación; particiones; P0. | Formulario; CA-01 confirmado. | `Ana`, `ana.machly`, `ClaveSegura8`, rol válido; guardar. | No se persiste fila y se muestra `Ingresa un correo electrónico válido.` | Manual/auto posterior. Captura/BD. Parte 2. |
| USER-07 | Alta; CA-01. Límite de contraseña. | Aceptación/unitaria; valores límite; P0. | Formulario y servicio integrados; CA-01 confirmado. | A) `Clave07` (7 caracteres); B) `Clave008` (8); resto válido. | A se rechaza con mensaje de longitud; B se acepta y queda almacenada como hash. | Manual/auto posterior. Evidencia de ambos resultados y BD sin secreto. Parte 2. |
| USER-08 | Alta; CA-02. Prevenir duplicado. | Aceptación/integración; decisión; P0. | Existe `ana.perez@example.com`; administrador. | Registrar el mismo correo con otro nombre, rol y clave. | Sin fila nueva ni cambio de la existente; mensaje exacto del CA-02. | Manual/auto posterior. Comparativo BD y captura. Parte 2. |
| USER-09 | Alta; CP-03. Exigir rol seleccionable. | Funcional; particiones; P1. | A) roles vacíos; B) roles cargados; CP-03 aprobado. | Completar nombre, correo y clave; guardar. | A no guarda y comunica que se requiere rol; B guarda con el id de rol elegido. Si CP-03 no se aprueba, registrar resultado observado. | Manual. Captura y BD. Parte 2. |
| USER-10 | Edición; F-USER-03. Actualizar nombre/rol y conservar contraseña. | Integración/BD; tabla de decisión; P1. | Usuario Ana existente, hash conocido solo por comparación; administrador. | Abrir detalle→editar; cambiar nombre y rol; dejar contraseña vacía; guardar. | Correo queda deshabilitado/inalterado, nombre/rol se actualizan y hash previo permanece igual. | Manual/auto posterior. Antes/después BD y capturas. Parte 2. |
| USER-11 | Edición; F-USER-03. Cambiar contraseña. | Integración/seguridad; transición; P1. | Usuario existente; administrador. | Editar con `NuevaClave8`; guardar; intentar login offline con nueva y antigua. | Hash cambia; nueva contraseña autentica offline y anterior se rechaza, si el algoritmo/flujo lo permite. Bloquear si contrato de hash servidor no es compatible. | Manual. BD redactada, capturas. Parte 2. |
| USER-12 | Navegación; F-USER-01/03. Refrescar tras edición. | E2E/regresión; recorrido; P1. | Usuario existente. | Editar, volver a detalle y lista. | Detalle y lista muestran el nombre/rol editados sin reinicio manual de app. | Manual. Video/capturas. Parte 2. |
| UI-01 | Formularios; F-UI-01. Foco, teclado y mensaje de error. | Usabilidad/accesibilidad; checklist; P1. | Login y alta, lector de pantalla disponible. | Navegar con teclado/TalkBack; provocar errores AUTH-01 y USER-05. | Campos tienen nombre comprensible, foco visible, orden lógico y error perceptible; controles táctiles se pueden accionar. Hallazgos se describen con pantalla/dispositivo. | Manual. Checklist y capturas. Parte 2. |
| UI-02 | Compatibilidad; F-UI-01. Mostrar pantallas en configuraciones objetivo. | Compatibilidad; matriz; P1. | Emuladores API 24, intermedia y 36; tamaños pequeño/normal. | Abrir login, lista, alta y detalle en retrato; rotar en formulario. | No hay texto/campo inaccesible, superposición o pérdida del dato en límites acordados. | Manual. Matriz y capturas. Parte 2. |
| UI-03 | Exploratoria; F-USER-01/03. Estados no previstos. | Exploratoria; charter; P1. | Timebox 45 min, datos de prueba. | Alternar red al editar, volver sin guardar, girar, pulsar guardar dos veces y abrir detalle inexistente. | Se documentan rutas, observaciones y defectos reproducibles; no se clasifica un comportamiento como fallo sin criterio. | Manual. Charter, video y tickets. Parte 2. |
| PERF-01 | Sincronización; F-PERF-01. Construir línea base. | Rendimiento básico; percentiles; P2. | Volúmenes y SLO acordados; ambiente estable. | Sincronizar conjuntos pequeño/medio/máximo tres veces cada uno. | Se registran tiempos de inicio/fin, volumen, red y p50/p95; se compara contra umbral acordado, no inventado. | Manual/instrumentado posterior. Hoja de métricas. Parte 2. |
| SEC-02 | Interfaz; F-SEC-01. No exponer contraseña. | Seguridad/UI; inspección; P1. | Usuario existente. | Abrir lista, detalle y edición; revisar capturas y controles. | Ninguna pantalla muestra contraseña/hash; campo usa entrada de contraseña y permite visibilidad solo durante interacción controlada. | Manual. Capturas redactadas/checklist. Parte 2. |

## E. ATDD, BDD y TDD - Parte 2

Parte 2 custodia la evidencia de aceptación de USER-04 a USER-09 y realiza revisión cruzada de los tests unitarios diseñados por Parte 1. Si la regla de dominio y la UI difieren, el resultado se registra como discrepancia de trazabilidad; no se adapta el criterio después de ejecutar para hacer pasar el caso.

---

## F. Herramientas y automatización

### Pareja principal solicitada

| Herramienta | Función propuesta | Requisitos técnicos | Evidencias y limitaciones |
|---|---|---|---|
| **TestLink** | Gestión de requisitos, casos, suites, ejecuciones, defectos y trazabilidad CA/F→caso→resultado. | Instancia accesible, roles de equipo, convención de IDs y vínculo con tablero/defectos. | Export de plan, ejecución, trazabilidad y reportes. No automatiza la app. |
| **Selenium WebDriver mediante Appium** | Automatización selectiva de flujos Android nativos: login, lista, alta y edición. Selenium solo no controla de forma nativa esta app; Appium/UiAutomator2 es requisito adicional de viabilidad. | JDK/Android SDK, emulador o dispositivo, Appium server, driver UiAutomator2, APK de prueba y localizadores estables. | Reportes de suite, capturas/video. Debe validarse estabilidad; para pruebas internas Android puede ser más apropiado Espresso. |

**Alternativas descartadas:** Squash cumple función similar a TestLink, por lo que no se recomienda usar ambos sin necesidad. Cypress está orientado a navegador web y no es adecuado para automatizar directamente esta app Android nativa. Para unitarias/instrumentadas, el ejecutor separado propuesto es Gradle + JUnit/Espresso, ya declarados por el proyecto; su incorporación operativa se decide en una etapa posterior.

### Automatización por capas

- **Primero:** unitarias de `UserRegistrationService`, mapeos y persistencia con datos controlados.
- **Después:** integración API/SQLite mediante servidor de prueba o mock de contrato.
- **Al final:** pocos E2E estables: AUTH-02, AUTH-06, SYNC-01, USER-04, USER-08 y USER-10.
- **Nunca automatizar por volumen:** exploración, usabilidad, lector de pantalla y revisión de evidencias requieren evaluación humana.

### Integración continua propuesta para una fase posterior

En cada pull request o actualización de rama: compilar, ejecutar unitarias y análisis estático acordado, publicar JUnit/lint. En una rama de integración o ejecución nocturna: instrumentadas y E2E con API sandbox, publicar XML/HTML, capturas, logs redactados y cobertura cuando se configure. No se propone despliegue automático.

## G. Revisión, inspección y auditoría

| Actividad | Objetivo, alcance y artefactos | Participantes y checklist | Registro, acciones y evidencia |
|---|---|---|---|
| Revisión de requisitos/diseño | Alinear CA-01/CA-02, contrato API y CP-01..CP-06 antes de ejecutar. Artefactos: ATDD, BDD, `AddEditUserActivity`, `ApiService`. | Autor del criterio, Integrante 1, Integrante 2. Checklist: rol administrador, mensajes, duplicado, qué significa `expiresIn`, política de sync/upgrade. | Minuta de decisiones con ID, responsable y fecha; actualizar casos bloqueados. Evidencia: minuta y matriz revisada. |
| Inspección formal de código/documentación | Revisar flujos AUTH/SYNC y alta local sin ejecutar. Artefactos: `LoginActivity`, `SyncManager`, `DbHelper`, `ApiClient`, `PrefsManager`, ATDD. | Moderador distinto del autor, lector, relator. Checklist: validación antes de persistir, atomicidad, nulos, secretos, HTTPS/logs, restricciones BD, mensajes y trazabilidad. | Hallazgo con archivo/símbolo, severidad, propuesta y estado. Evidencia: checklist y lista de hallazgos. |
| Auditoría interna | Confirmar que el plan se siguió y las evidencias corresponden a la versión/ambiente indicado. Artefactos: TestLink, tablero, defectos, reportes, capturas y BD redactada. | Cada integrante audita una muestra del otro; un tercer integrante consolida si existe. | Lista de cumplimiento, muestra revisada, desviación y acción correctiva. Evidencia: informe breve de auditoría. |

### Profundización obligatoria

1. **Historia CA-02, correo único:** revisar que el criterio “no crea ni modifica” coincide con el comportamiento del repositorio, inspeccionar USER-08/DB-02 y auditar la comparación de BD antes/después.
2. **Sincronización roles→usuarios:** revisar contrato, orden, manejo de fallos y evidencia de SYNC-01 a SYNC-04; quien diseñe no aprueba su propia evidencia.

## H. Criterios de entrada, salida y defectos

### Condiciones propuestas

| Hito | Condición verificable |
|---|---|
| Definition of Ready (DoR) de un caso | Caso con ID, objetivo, prioridad, precondiciones, datos no sensibles, esperado verificable, fuente/criterio y responsable; dependencias disponibles. |
| Entrada a ejecución | Build identificada e instalable; ambiente/API sandbox accesible si aplica; datos y dispositivo registrados; criterios pendientes resueltos o caso marcado bloqueado. |
| Suspensión | Crash bloqueante, ambiente no disponible, versión no identificable, datos corruptos o evidencia imposible de capturar. Registrar motivo y no marcar resultado final. |
| Reanudación | Causa de suspensión resuelta, build/ambiente confirmado y caso reiniciado con nueva evidencia. |
| Aprobación funcional | Todos los P0 aplicables aprobados; P1 resueltos o aceptados formalmente; trazabilidad completa; no quedan defectos críticos/altos sin decisión. Umbrales son propuesta pendiente de acuerdo. |
| Definition of Done (DoD) de testing | Resultado registrado, evidencia vinculada, defectos creados o verificados, regresión definida/ejecutada si procede y revisión cruzada completada. |
| Cierre del ciclo | Métricas calculadas desde datos reales, auditoría interna terminada, riesgos residuales aceptados y entrega de evidencias completa. |

### Registro de defectos

Cada ticket debe incluir: ID, título, severidad, prioridad, módulo/caso relacionado, build, dispositivo/API, red, precondiciones, datos no sensibles, pasos numerados, resultado esperado/obtenido, adjuntos, responsable, estado y decisión de regresión. Estados: Nuevo → Triado → En progreso → Corregido → Verificado → Cerrado; Reabierto si falla la verificación.

| Severidad | Definición propuesta | Ejemplos de referencia |
|---|---|---|
| Crítica | Impide usar la app, expone secretos o corrompe datos sin alternativa. | Inicio imposible, credencial/token expuesto, pérdida silenciosa de datos. |
| Alta | Bloquea un flujo P0 o incumple CA-01/CA-02. | Duplicado modifica al usuario existente, sync inutilizable. |
| Media | Afecta flujo P1 con alternativa o datos limitados. | Lista no se refresca tras edición. |
| Baja | Impacto menor visual o de redacción sin pérdida funcional. | Texto incoherente o alineación no crítica. |

Prioridad determina orden de corrección (P0 inmediata, P1 próxima iteración, P2 planificada, P3 diferible) y no sustituye a severidad.

## I. Métricas y trazabilidad

### Indicadores planificados (sin valores calculados)

| Indicador | Fórmula | Fuente | Medición |
|---|---|---|---|
| Cobertura de requisitos | requisitos/funcionalidades con ≥1 caso diseñado o ejecutado ÷ total identificado ×100 | Matriz de trazabilidad/TestLink | Diseño y cierre |
| Cobertura de código | líneas/ramas cubiertas ÷ líneas/ramas instrumentables ×100 | Herramienta de cobertura a acordar | CI posterior |
| Avance de ejecución | casos con resultado final ÷ casos planificados aplicables ×100 | TestLink | Diario/cierre |
| Tasa de aprobación | aprobados ÷ ejecutados ×100 | TestLink | Diario/cierre |
| Tasa de fallo | fallidos ÷ ejecutados ×100 | TestLink | Diario/cierre |
| Defectos por severidad/estado | conteo agrupado por severidad y estado | Registro de defectos | Diario/cierre |
| Densidad de defectos | defectos confirmados ÷ KLOC modificadas, o por caso ejecutado si no hay KLOC fiable; declarar unidad | Defectos y SCM | Cierre |
| Cumplimiento de auditoría | ítems conformes ÷ ítems auditados ×100 | Checklist auditoría | Cierre |
| Velocidad del equipo | casos cerrados o puntos terminados por iteración | Tablero | Solo si hay iteraciones comparables; de otro modo “no disponible”. |

### Matriz de trazabilidad inicial

| Funcionalidad/criterio | Criterio de aceptación | Casos | Responsable | Evidencia prevista |
|---|---|---|---|---|
| F-AUTH-01 | Login solo con respuesta válida; CP-01 pendiente para expiración | AUTH-01..05, API-01 | Integrante 1 | Respuesta redactada, capturas, JUnit/E2E posterior |
| F-AUTH-02 | Usuario cacheado y contraseña válida acceden offline | AUTH-06, AUTH-07 | Integrante 1 | Video, capturas, log red, BD |
| F-SYNC-01/02 | Roles preceden a usuarios; error se comunica; CP-04 pendiente | SYNC-01..05, DB-01, API-01, PERF-01 | I1 (SYNC), I2 (PERF) | API redactada, BD antes/después, logs, tiempos |
| CA-01 | Nombre, correo válido, clave ≥8, rol y almacenamiento local | USER-04..07, USER-09 | Integrante 2 | Capturas, BD y escenarios BDD |
| CA-02 | Correo único, sin crear ni modificar, mensaje específico | DB-02, USER-08 | I1 ejecuta BD / I2 ejecuta UI; revisión cruzada | Comparativo BD y captura |
| F-USER-01/03 | Listado, detalle y edición consistentes | USER-01..03, USER-10..12 | Integrante 2 | Capturas/video y BD |
| F-SEC-01 | CP-05 pendiente: HTTPS y sin secretos expuestos | SEC-01, SEC-02, AUTH-05 | I1 (plataforma), I2 (UI) | Checklist redactado y hallazgos |
| F-UI-01 | Navegación y accesibilidad aceptable conforme criterio acordado | UI-01..03 | Integrante 2 | Checklist, matriz dispositivos, charter |

## J. Secuencia y tablero de trabajo

### Secuencia futura

1. **Preparar:** decidir copia entregable, ambiente sandbox, API URL, datos, dispositivos y reglas pendientes.
2. **Revisar:** cerrar CA/CP y contrato API; inspección cruzada de AUTH/SYNC y alta.
3. **Humo:** instalar, abrir login, autenticar en sandbox y ver lista local.
4. **Base técnica:** unitarias e integración API/SQLite por dependencias de roles→usuarios.
5. **Negocio e interfaz:** aceptación de alta/edición, UI/E2E, offline, seguridad básica, accesibilidad y compatibilidad.
6. **Defectos:** registrar, priorizar, corregir por desarrollo y verificar por alguien distinto del autor.
7. **Regresión:** repetir humo y casos afectados; ejecutar rendimiento básico si hay datos/SLO.
8. **Cierre:** auditar evidencias, calcular métricas reales y acordar riesgos residuales.

### Tablero propuesto

| ID tablero | Descripción | Dependencia | Responsable | Criterio de finalización |
|---|---|---|---|---|
| QA-01 | Confirmar copia fuente y versión de prueba | Dueño del repositorio | Compartido | Ruta y commit/build anotados. |
| QA-02 | Acordar CA-01/02 y CP-01..06 | QA-01 | Integrante 1 | Minuta con decisiones y casos desbloqueados. |
| QA-03 | Preparar sandbox, roles, usuarios y dispositivo | QA-01, QA-02 | Integrante 1 | Datos no sensibles y ambiente registrados. |
| QA-04 | Diseñar/registrar AUTH, SYNC, DB y API | QA-02 | Integrante 1 | Casos en TestLink con evidencia prevista. |
| QA-05 | Diseñar/registrar USER, UI, A11y y compatibilidad | QA-02 | Integrante 2 | Casos en TestLink con evidencia prevista. |
| QA-06 | Ejecutar humo e integración base | QA-03, QA-04 | Integrante 1 | Resultados/evidencias y defectos registrados. |
| QA-07 | Ejecutar aceptación y UI | QA-03, QA-05 | Integrante 2 | Resultados/evidencias y defectos registrados. |
| QA-08 | Verificar correcciones y regresión dirigida | QA-06/07 y build corregida | Cruzado | Reprueba y regresión vinculadas al ticket. |
| QA-09 | Auditoría interna y métricas | QA-08 | Compartido | Checklist completo y métricas sin datos inventados. |

No hay marco ágil documentado. Se propone un flujo simple: backlog QA priorizado → diseño listo → en ejecución → bloqueado → defecto registrado → verificación/regresión → cerrado, con revisión cruzada semanal o por iteración.

---

## Distribución del trabajo

### Distribución principal - dos personas

| Bloque | Responsable genérico | Módulos y casos | Actividades y entregables | Dependencias | Esfuerzo relativo |
|---|---|---|---|---|---|
| Parte 1 | Integrante 1 | AUTH-01..07, SYNC-01..05, DB-01..03, SEC-01, API-01 | Diseñar/ejecutar autenticación, API, sync, SQLite y seguridad básica; evidencias BD/API/logs; tickets; verificar correcciones. Entrega: suite y reporte de Parte 1. | Sandbox, contrato API, datos/roles y decisiones CP-01/04/05/06. | Alto - mayor riesgo e integración. |
| Parte 2 | Integrante 2 | USER-01..12, UI-01..03, SEC-02, PERF-01 | Diseñar/ejecutar alta, edición, lista/detalle, aceptación, accesibilidad, compatibilidad y rendimiento básico; evidencias UI; tickets; verificar correcciones. Entrega: suite y reporte de Parte 2. | Roles/datos locales, decisiones CA/CP-03, dispositivos y SLO para PERF. | Alto - mayor interacción y validación funcional. |

**Trabajo compartido:** QA-01, QA-02, humo, regresión P0, triage, métricas y cierre. Integrante 1 revisa evidencia de USER-08 y USER-10; Integrante 2 revisa evidencia de SYNC-01, SYNC-03 y AUTH-06. Ningún integrante aprueba su propia muestra de auditoría.

### Alternativa - tres personas

| Integrante | Alcance | Entregables y revisión cruzada |
|---|---|---|
| Integrante 1 | AUTH-01..07, API-01, SEC-01 | Evidencia de sesión/API/seguridad. Revisa muestra de Integrante 2. |
| Integrante 2 | SYNC-01..05, DB-01..03, PERF-01 | Evidencia de sync/SQLite/rendimiento. Revisa muestra de Integrante 3. |
| Integrante 3 | USER-01..12, UI-01..03, SEC-02 | Evidencia de aceptación/UI. Revisa muestra de Integrante 1. Consolida el informe, trazabilidad y métricas con insumos verificables de todos. |

El consolidado no reemplaza la auditoría cruzada: cada evidencia conserva ejecutor, revisor, versión y fecha. Los tres participan en decisiones de criterios y cierre.

---

## Correspondencia con la actividad académica

| Apartado mínimo solicitado | Ubicación en este plan |
|---|---|
| 1. Introducción | Secciones 0 y A |
| 2. Alcance | Sección B |
| 3. Estrategia de pruebas | Sección C |
| 4. Roles y responsabilidades | Distribución del trabajo y sección G |
| 5. Ciclo de vida de las pruebas | Sección J |
| 6. Criterios de entrada y salida | Sección H |
| 7. Métricas y control de calidad | Sección I y auditoría de G |
| 8. Riesgos y mitigación | A.5, B, C y H |

### Checklist de evidencias posteriores

| Evidencia | Estado actual | Acción futura |
|---|---|---|
| Test Plan actualizado | **Existente:** este plan inicial | Actualizar tras resolver decisiones. |
| Repositorio con pruebas implementadas | **Pendiente de verificar/producir** | Implementar solo en etapa autorizada. |
| Informe breve de auditoría interna | **Pendiente de producir** | Generar tras ejecución y revisión cruzada. |
| Vinculación con tablero ágil | **Pendiente de producir** | Crear QA-01..09 en tablero elegido. |
| Resultados manuales y automatizados | **Pendiente de producir** | Registrar ejecuciones y adjuntos. |
| Indicadores y mejoras propuestas | **Pendiente de producir** | Calcular desde datos reales al cierre. |
| Evidencias de colaboración | **Pendiente de producir** | Conservar revisiones, minutas y asignaciones. |

## Supuestos, limitaciones y decisiones pendientes

1. Confirmar cuál de las dos copias anidadas del proyecto es la línea base a evaluar.
2. Proveer URL de sandbox, contrato API versionado, credenciales de prueba y política de autorización por rol.
3. Confirmar si el alta de usuarios es solo local o debe persistir remotamente; el código deja un TODO para API de alta/edición.
4. Ratificar CA-01/CA-02 y definir CP-01..CP-06, especialmente expiración de token, atomicidad, rol obligatorio y migración.
5. Acordar si `passwordHash` remoto es compatible con el acceso offline y la política criptográfica aceptable.
6. Definir dispositivos/API Android objetivo, tamaños, volúmenes de datos y umbrales de rendimiento.
7. Autorizar y preparar herramientas: TestLink, Selenium+Appium o alternativa Espresso, emuladores y almacenamiento de evidencias sin secretos.

## Siguientes instrucciones para ejecutar el plan

1. Confirma la copia del proyecto y proporciona el ambiente/API de pruebas sin secretos reales.
2. Solicita cerrar las decisiones pendientes CA/CP con los responsables funcionales y de API.
3. Solicita crear los casos del bloque Parte 1 en la herramienta de gestión elegida.
4. Solicita crear los casos del bloque Parte 2 y la matriz de dispositivos.
5. Solicita preparar datos SQLite/API y el checklist de humo.
6. Solicita ejecutar primero los casos P0 de autenticación, sincronización e integridad.
7. Solicita ejecutar aceptación de alta/edición, UI, accesibilidad y compatibilidad.
8. Solicita registrar y priorizar defectos; luego pide verificación y regresión dirigida.
9. Solicita generar el informe de auditoría y las métricas desde los resultados reales.
