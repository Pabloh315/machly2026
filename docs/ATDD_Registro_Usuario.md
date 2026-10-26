# Entrega individual: ATDD - Registro de usuario

**Proyecto:** Machly Android App  
**Rol asumido:** representante del cliente / analista de aceptación  
**Funcionalidad:** registro local de usuario

## 1. Concepto de ATDD

ATDD (*Acceptance Test-Driven Development*) es una práctica ágil donde cliente, desarrollador y pruebas acuerdan los criterios de aceptación antes de implementar. Los criterios describen comportamiento observable y permiten crear pruebas que confirman que el producto entrega valor. En este trabajo, el cliente define qué debe ocurrir al registrar un usuario y el equipo lo traduce a escenarios verificables.

## 2. Lenguaje ubicuo

| Término | Significado acordado |
|---|---|
| Usuario | Persona registrada que puede usar el sistema. |
| Registro | Alta de una cuenta local con nombre, correo, contraseña y rol. |
| Correo duplicado | Correo que ya pertenece a un usuario almacenado. |
| Validación | Revisión de las reglas antes de guardar la cuenta. |
| Repositorio | Componente que consulta o guarda usuarios persistidos. |

## 3. Criterios de aceptación (ATDD)

### CA-01 - Registro exitoso

**Como** nuevo usuario  
**Quiero** registrarme con nombre, correo electrónico válido, contraseña de al menos 8 caracteres y un rol  
**Para** disponer de una cuenta almacenada en el sistema.

**Condiciones verificables:**

- El nombre no puede estar vacío.
- El correo debe tener formato válido.
- La contraseña debe tener al menos 8 caracteres.
- El correo no debe existir previamente.
- Al cumplir las reglas, el sistema almacena el usuario y muestra el mensaje `User saved locally`.

### CA-02 - Protección contra correo duplicado

**Como** administrador  
**Quiero** que el sistema rechace el registro con un correo ya existente  
**Para** conservar la integridad e identificar de forma única a cada usuario.

**Condiciones verificables:**

- Si el correo ya está registrado, no se crea ni se modifica ningún usuario.
- El sistema informa: `El correo electrónico ya está registrado.`

## 4. Escenarios BDD en Gherkin

Aunque la rúbrica exige un escenario, se redactan los dos para que cada criterio quede cubierto.

```gherkin
Funcionalidad: Registro de usuario

Escenario: Registro exitoso con datos válidos
  Dado que no existe un usuario con el correo "ana.perez@example.com"
  Cuando registro a "Ana Pérez" con el correo "ana.perez@example.com"
    y con la contraseña "claveSegura8"
  Entonces el sistema acepta el registro
    y guarda al usuario localmente
    y muestra el mensaje "User saved locally"

Escenario: Intento de registro con correo duplicado
  Dado que ya existe un usuario con el correo "ana.perez@example.com"
  Cuando intento registrar a "Ana Pérez" con el mismo correo y una contraseña válida
  Entonces el sistema rechaza el registro
    y muestra el mensaje "El correo electrónico ya está registrado."
```

## 5. Modelo de dominio (DDD)

```text
Entidad: Usuario
  Atributos: nombreCompleto, email, passwordHash, rol, id

Servicio de dominio: UserRegistrationService
  Colaborador: EmailRegistrationChecker (puerto hacia el repositorio)
  Regla 1: el nombre es obligatorio.
  Regla 2: el email debe tener formato válido.
  Regla 3: la contraseña debe tener 8 o más caracteres.
  Regla 4: el email no puede estar repetido.
```

## 6. Evidencia TDD y SQA

La prueba unitaria `UserRegistrationServiceTest` comprueba el escenario principal y el caso de correo duplicado sin depender de Android ni de SQLite. El ciclo aplicado es:

1. **RED:** se especifica que un registro válido se acepta y que uno duplicado se rechaza.
2. **GREEN:** `UserRegistrationService` aplica las reglas mínimas para hacer pasar las pruebas.
3. **REFACTOR:** se extrae `EmailRegistrationChecker`, separando la regla de negocio de la base de datos.

| Caso de prueba | Resultado esperado | Evidencia |
|---|---|---|
| Registro válido | Aceptado | `acceptsRegistrationWhenDataIsValidAndEmailIsAvailable` |
| Correo duplicado | Rechazado | `rejectsRegistrationWhenEmailAlreadyExists` |
| Correo con formato inválido | Rechazado | Regla implementada en `UserRegistrationService` |
| Contraseña menor a 8 caracteres | Rechazada | Regla implementada en `UserRegistrationService` |

## 7. Análisis de calidad

Antes del ajuste, el formulario solo verificaba campos vacíos y `DbHelper.insertOrUpdateUser` actualizaba un usuario al encontrar el mismo correo; esto incumplía CA-02. La solución propuesta valida los datos antes de persistir y consulta si el correo ya existe únicamente en altas nuevas. La contraseña se conserva como hash y no en texto plano.

**Riesgo pendiente de producción:** `BCryptUtils` usa SHA-256 pese a su nombre. Para un sistema real se debe sustituir por BCrypt o Argon2 con sal y factor de trabajo.

## 8. Cómo ejecutar la evidencia

Desde la carpeta raíz del proyecto, ejecutar:

```powershell
.\gradlew.bat testDebugUnitTest
```

La prueba se encuentra en `app/src/test/java/com/machly/app/domain/registration/UserRegistrationServiceTest.java`.
