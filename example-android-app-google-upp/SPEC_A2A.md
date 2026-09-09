# Spec: implementar App-to-App Verification (A2A) en `example-android-app-google-upp`

> Este documento es autocontenido: un agente sin contexto previo de la conversación que lo originó
> debe poder implementarlo leyendo solo este archivo + el código del repo.

## Contexto y objetivo

Este repo (`example-android-app-google-upp`) es un ejemplo de referencia de integración con Google
Tap And Pay / Google Wallet Push Provisioning, con dos partes: `app/` (Android) y `backend-app/`
(Node.js/TypeScript, proxy hacia la API de Pomelo).

Pomelo tiene una guía pública de integración de **App-to-App Verification (A2A)** en
`pomelo-docs/docs/modules/tokenization/google-pay-a2a.es.md` (repo `pomelo-docs`), que este mismo
repo de ejemplo referencia como implementación de punta a punta (ver sección "Repositorio de
ejemplo" de esa guía). Hoy ese flujo **no está implementado** en este repo. El objetivo de este spec
es agregarlo, del lado Android, con el mismo nivel de documentación y calidad que ya tienen
Push Provisioning y Bounce Provisioning en este mismo proyecto.

**Qué es A2A (resumen funcional):** cuando un cliente agrega una tarjeta a Google Wallet ingresando
los datos a mano, Google a veces necesita verificar la identidad del titular antes de activar el
token (Yellow Path). Con A2A, en vez de mandar un OTP por SMS/email, Google Wallet invoca
directamente la app del emisor (esta app) con un Intent explícito conteniendo el payload de Visa en
Base64URL. La app autentica al usuario con su propio mecanismo (login/biometría — a elección del
emisor), muestra la tarjeta y, si el usuario confirma, llama a la API de Pomelo para activar el
token. Finalmente devuelve el resultado a Google Wallet.

**Importante:** el **backend ya tiene el endpoint necesario implementado** (commit `04dbf19`,
`feat(backend-app): add token activation endpoint`): `POST /tokens/:id/activate` en
`backend-app/src/routes/token-lifecycle.ts`, validado con `activateTokenBodySchema` en
`backend-app/src/lib/schemas.ts` (campo `motive`, enum `["APP_TO_APP_ACTIVATION"]`), con tests en
`backend-app/tests/app.test.ts`. **No hay que tocar nada del backend.** Este spec es 100% del lado
Android (`app/`).

## Fuente de la guía funcional (léela antes de implementar)

Archivo: `/Users/julian.bragazzi/Documents/git/pomelo-docs/docs/modules/tokenization/google-pay-a2a.es.md`

Puntos clave de esa guía que hay que respetar:

1. **Manifest**: intent-filter con acción `{package name}.a2a` (formato fijo definido por Visa) y
   categoría `DEFAULT`, en la activity que recibe la verificación.
2. **Payload**: Google Wallet manda el payload de Visa en `Intent.EXTRA_TEXT`, como JSON codificado
   en Base64URL. Campos: `panReferenceID`, `tokenRequestorID`, `tokenReferenceID`, `panLast4`,
   `deviceID`, `walletAccountID` (todos opcionales/nullable al parsear, por robustez).
3. **Validar el caller**: antes de mostrar cualquier dato, confirmar que `callingPackage` sea
   `com.google.android.gms` (Google Play Services / Google Wallet).
4. **Autenticación del cliente**: queda 100% a criterio del emisor (login, biometría, PIN, lo que
   ya use la app). Si el cliente cancela o falla, no se debe llamar a la API de activación.
5. **Mostrar la tarjeta**: usar `panLast4` del payload y una acción para confirmar la activación.
6. **Activar el token**: llamar a `POST /tokenization/tokens/{tokenId}/activate` usando
   `tokenReferenceID` del payload como `tokenId`, con body `{ "motive": "APP_TO_APP_ACTIVATION" }`.
   Si la respuesta es exitosa, Visa activa el token sin necesidad de devolver ningún código de
   autenticación adicional.
7. **Devolver el resultado a Google Wallet**: `setResult(RESULT_OK, intent)` con
   `intent.putExtra("STEP_UP_RESPONSE", "approved" | "declined" | "failure")` y `finish()`.
   - `approved`: se autenticó al cliente y se activó el token exitosamente.
   - `declined`: el cliente canceló la autenticación o eligió no continuar (no se llama a la API).
   - `failure`: error técnico (autenticación fallida, error de red, timeout).
8. **Pruebas con adb**: se puede simular el intent de Google Wallet con `adb shell am start`, ver
   sección "Pruebas con Android Debug Bridge (adb)" de la guía para el comando exacto.

## Decisiones de diseño ya tomadas (no las re-abras, seguilas)

Estas decisiones se tomaron para **priorizar simplicidad** y mantener consistencia con el código
existente del repo (que usa Gson + Ktor manual, no kotlinx.serialization ni Retrofit):

| Punto | Decisión | Por qué |
|---|---|---|
| Parsing del payload | **Gson** (`@SerializedName`), no kotlinx.serialization | `BackendService.kt` ya usa Gson (`com.google.gson.Gson`) en todo el proyecto. No sumar un plugin/dependencia nueva solo para esto. |
| Llamada a `/activate` | Método nuevo en `BackendService`, mismo patrón `post<T>` interno con Ktor | El proyecto no usa Retrofit; ya existe `BackendService.post()` genérico. No introducir Retrofit. |
| Autenticación del cliente | **Simulada**: pantalla Compose con botón "Simular autenticación" (mock login), documentado con KDoc explícito de que en producción va el mecanismo real del emisor | Sumar `androidx.biometric`/`BiometricPrompt` real agrega una dependencia y complejidad que no aportan al propósito del ejemplo (mostrar el cableado de A2A, no un tutorial de biometría). |
| Datos de tarjeta a mostrar | Solo `panLast4` del payload (texto simple tipo `•••• •••• •••• 1234`), **sin** reusar `PomeloCardComposable` completo | El payload de A2A no trae `cardholderName` ni `brand`, y no hay `cardId` para hacer un lookup contra `/cards`. Forzar ese lookup agregaría alcance no pedido por la guía. |

Si en algún punto de la implementación aparece una razón de peso para apartarse de alguna de estas
decisiones, documentarlo en el PR/commit correspondiente, pero por defecto **seguir esta tabla**.

## Convenciones del repo a seguir (mirar antes de escribir código)

Archivos de referencia ya existentes que muestran el estilo esperado — leerlos antes de escribir
código nuevo, para igualar naming, KDoc, y estructura:

- `app/app/src/main/java/com/example/example_google_upp/wallet/models/PushProvisioningResult.kt`
  → patrón de `sealed interface` para resultados (Success/Cancelled/Error).
- `app/app/src/main/java/com/example/example_google_upp/wallet/PushProvisioningResultResolver.kt`
  → patrón de "resolver" object que traduce un resultado de Android/SDK a un modelo interno, con
  KDoc explicando el porqué de las decisiones y links a la doc oficial de Google.
- `app/app/src/main/java/com/example/example_google_upp/MainActivity.kt`
  → patrón de Activity que registra `registerForActivityResult`, tiene KDoc de clase extenso
  explicando responsabilidades, y delega lógica de negocio a objetos/servicios separados.
- `app/app/src/main/java/com/example/example_google_upp/data/BackendService.kt`
  → patrón de cliente HTTP interno (`get<T>`, `post<T>` genéricos con Ktor + Gson).
- `app/app/src/main/java/com/example/example_google_upp/di/AppModule.kt`
  → módulo de Koin donde registrar nuevos `single`/`viewModelOf`.
- `app/app/src/main/AndroidManifest.xml`
  → ya tiene el intent-filter de Bounce Provisioning como ejemplo de cómo documentar un
  intent-filter con un comentario XML explicando qué es y quién lo invoca.
- `app/README.md`, sección "### Bounce Provisioning" (al final del archivo)
  → estructura exacta a replicar para documentar A2A: explicación en prosa, lista numerada de
  partes de la integración, diagrama `mermaid sequenceDiagram`, y un bullet de "Referencia oficial".
- `app/app/src/test/java/com/example/example_google_upp/wallet/PushProvisioningResultResolverTest.kt`
  y `TapAndPayMappingsTest.kt` → estilo de tests unitarios esperado (JUnit, nombres descriptivos).
- `backend-app/CLAUDE.md` → reglas del backend (no debería hacer falta tocarlo, es solo contexto).

## Archivos nuevos a crear

Todo bajo un paquete nuevo `a2a/`, paralelo a `wallet/`, en
`app/app/src/main/java/com/example/example_google_upp/`:

```
a2a/
├── AppToAppVerificationActivity.kt
├── AppToAppScreen.kt
├── AppToAppViewModel.kt
├── CallerValidator.kt
├── StepUpResultResolver.kt
└── models/
    ├── VisaA2aPayload.kt
    ├── StepUpResult.kt
    └── AppToAppUiState.kt
```

### `models/VisaA2aPayload.kt`

- `data class VisaA2aPayload` con los 6 campos de la tabla de la guía, todos `String?` con default
  `null`, anotados `@SerializedName` (Gson):
  `panReferenceID`, `tokenRequestorID`, `tokenReferenceID`, `panLast4`, `deviceID`,
  `walletAccountID`.
- `companion object` con `fun fromExtraText(extraText: String?, gson: Gson = Gson()): VisaA2aPayload?`:
  - Si `extraText` es nulo o blank, devuelve `null`.
  - Decodifica Base64URL: `Base64.decode(extraText, Base64.URL_SAFE or Base64.NO_WRAP)`.
  - Parsea el JSON decodificado con `gson.fromJson(..., VisaA2aPayload::class.java)`.
  - Envuelve todo en `try/catch` (Base64 inválido, JSON malformado) y devuelve `null` en error.

### `models/StepUpResult.kt`

```kotlin
sealed interface StepUpResult {
    data object Approved : StepUpResult
    data object Declined : StepUpResult
    data class Failure(val message: String?) : StepUpResult
}
```

### `models/AppToAppUiState.kt`

Estado de UI para el ViewModel, como `sealed interface` o `data class` con un enum de fase. Fases
necesarias:
- `Loading` — validando payload/caller al arrancar.
- `InvalidRequest` — payload ausente/inválido o caller no es Google Wallet (termina en `Failure`).
- `AwaitingAuthentication(payload)` — pantalla mostrando la tarjeta + botón "Simular autenticación".
- `Authenticated(payload)` — ya autenticado, mostrando botón "Activar".
- `Activating(payload)` — llamando al backend, botón deshabilitado con texto "Activando…".
- `Finished(result: StepUpResult)` — resultado final, listo para que la Activity haga `finish()`.

(El nombre exacto de las fases queda a criterio de quien implemente, siempre que cubran estos
casos y el naming sea consistente con `CardSearchUiState.kt`, que ya existe como referencia de
estilo para estados de UI en este proyecto.)

### `CallerValidator.kt`

```kotlin
object CallerValidator {
    private const val GOOGLE_WALLET_PACKAGE = "com.google.android.gms"

    fun isGoogleWallet(callingPackage: String?): Boolean = callingPackage == GOOGLE_WALLET_PACKAGE
}
```

### `AppToAppViewModel.kt`

- Recibe el `VisaA2aPayload` ya parseado (o `null`) y si el caller es válido, vía constructor o un
  método `init(payload, isValidCaller)` llamado desde la Activity en `onCreate`.
- Expone el estado (`StateFlow<AppToAppUiState>`) y un evento/flow de `StepUpResult` final para que
  la Activity cierre y devuelva el resultado.
- `onSimulatedAuthenticationConfirmed()`: pasa de `AwaitingAuthentication` a `Authenticated`.
- `onAuthenticationDeclined()`: emite `StepUpResult.Declined` **sin** llamar al backend.
- `onActivate()`:
  - pasa a `Activating`.
  - llama a `BackendService.activateToken(tokenId = payload.tokenReferenceID.orEmpty(), motive = "APP_TO_APP_ACTIVATION")`.
  - éxito → emite `StepUpResult.Approved`.
  - excepción → emite `StepUpResult.Failure(mensaje)`.
- Si el payload es `null` o el caller no es válido al iniciar, emite directamente
  `StepUpResult.Failure(...)` sin pasar por autenticación (según la guía: "Payload ausente o
  inválido: no puedes continuar el flujo").
- Sigue el patrón de Koin `viewModelOf` visto en `CardSearchViewModel.kt` + `AppModule.kt`.

### `AppToAppScreen.kt`

Composable(s) que renderizan cada fase del `AppToAppUiState`:
- Texto "Activa tu tarjeta".
- Tarjeta simplificada mostrando `•••• •••• •••• {panLast4}` (sin reusar `PomeloCardComposable`,
  ver tabla de decisiones).
- Botón "Simular autenticación" (visible en `AwaitingAuthentication`) que llama a
  `onSimulatedAuthenticationConfirmed()`, y un botón/link secundario para simular que el cliente
  cancela (`onAuthenticationDeclined()`).
- Botón "Activar" (visible en `Authenticated`), deshabilitado y con texto "Activando…" durante
  `Activating` (igual al snippet `AppToAppScreen` de la guía).
- Estado de error simple si `Finished(Failure)` antes de que la Activity cierre.

### `AppToAppVerificationActivity.kt`

- `ComponentActivity` (no `AppCompatActivity`, para ser consistente con `MainActivity.kt`).
- `onCreate`:
  1. Parsear el payload: `VisaA2aPayload.fromExtraText(intent?.getStringExtra(Intent.EXTRA_TEXT))`.
  2. Validar el caller: `CallerValidator.isGoogleWallet(callingPackage)`.
  3. Inicializar el ViewModel con el resultado de 1 y 2.
  4. `setContent { AppToAppScreen(...) }` observando el `StateFlow` del ViewModel.
  5. Recolectar (con `lifecycleScope`) el evento de `StepUpResult` final del ViewModel y, al
     recibirlo, llamar a `finishWithResult(result)`.
- `finishWithResult(result: StepUpResult)`: delega en `StepUpResultResolver.toResultIntent(result)`
  para construir el Intent, hace `setResult(RESULT_OK, intent)` y `finish()`.
- KDoc de clase extenso (mismo estilo que `MainActivity.kt`), explicando el flujo completo y
  linkeando a la doc oficial de Google
  (https://developers.google.com/pay/issuers/tsp-integration/app-to-app-idv) y a
  `google-pay-a2a.es.md`.

### `StepUpResultResolver.kt`

```kotlin
object StepUpResultResolver {
    private const val STEP_UP_RESPONSE_EXTRA = "STEP_UP_RESPONSE"

    fun toResultIntent(result: StepUpResult): Intent = Intent().apply {
        putExtra(STEP_UP_RESPONSE_EXTRA, result.toStepUpResponseValue())
    }

    private fun StepUpResult.toStepUpResponseValue(): String = when (this) {
        StepUpResult.Approved -> "approved"
        StepUpResult.Declined -> "declined"
        is StepUpResult.Failure -> "failure"
    }
}
```

## Archivos existentes a modificar

### `app/app/src/main/AndroidManifest.xml`

Agregar, dentro de `<application>`, una nueva activity (siguiendo el mismo estilo de comentario que
ya tiene el intent-filter de Bounce Provisioning en este archivo):

```xml
<!--
  App-to-app verification (IDV) invocada por Google Wallet cuando hace falta confirmar
  la identidad del cliente (Yellow Path) durante el agregado de una tarjeta Visa.

  Visa define un formato fijo para la acción: "{package name}.a2a".

  Docs: https://developers.google.com/pay/issuers/tsp-integration/app-to-app-idv
-->
<activity
    android:name=".a2a.AppToAppVerificationActivity"
    android:exported="true">

    <intent-filter>
        <action android:name="com.example.example_google_upp.a2a" />

        <category android:name="android.intent.category.DEFAULT" />
    </intent-filter>
</activity>
```

Nota: la acción va **hardcodeada** con el `applicationId` real (`com.example.example_google_upp`),
igual que en la guía — no se puede usar `${applicationId}` como placeholder de manifest merger para
esto porque Visa espera el string literal exacto; si se usa `${applicationId}.a2a` verificar que el
manifest merger de Gradle lo resuelva correctamente antes de darlo por bueno (probarlo con
`./gradlew :app:processDebugManifest` o inspeccionando el APK).

### `app/app/src/main/java/com/example/example_google_upp/data/BackendService.kt`

Agregar un método nuevo, usando el `post<T>` interno ya existente:

```kotlin
suspend fun activateToken(tokenId: String, motive: String = "APP_TO_APP_ACTIVATION") {
    post<Unit>("tokens/${tokenId}/activate", mapOf("motive" to motive))
}
```

Ajustar el tipo de retorno/manejo de body vacío según lo que devuelva realmente el backend (ver
`token-lifecycle.ts`: responde `202` con el JSON de la respuesta de Pomelo — puede no ser necesario
parsear el body, con que no lance excepción en caso de status de error alcanza). Revisar cómo
`post<T>` maneja hoy los status codes de error de Ktor (¿lanza excepción automáticamente o hay que
chequear `response.status`?) y replicar ese mismo comportamiento para consistencia.

### `app/app/src/main/java/com/example/example_google_upp/di/AppModule.kt`

Agregar `viewModelOf(::AppToAppViewModel)` al módulo `appModule`.

### `app/README.md`

Agregar una sección nueva `### App-to-App Verification (A2A)` **después** de la sección
`### Bounce Provisioning` (al final del archivo, antes de "## Referencias del SDK usadas por la
app"), replicando exactamente su estructura:

1. Párrafo explicando qué es A2A y cuándo lo invoca Google Wallet (Yellow Path).
2. Lista numerada de las piezas de la integración (manifest, Activity, ViewModel, BackendService).
3. Diagrama `mermaid sequenceDiagram` mostrando: Google Wallet → Activity (intent) → validación de
   caller → autenticación simulada → activación vía backend → Pomelo → resultado de vuelta a
   Google Wallet.
4. Bullet(s) de referencia oficial:
   - https://developers.google.com/pay/issuers/tsp-integration/app-to-app-idv
   - Guía interna de Pomelo: `google-pay-a2a.es.md`
5. Agregar también las URLs nuevas a la sección final `## Referencias del SDK usadas por la app`.

### `README.md` (raíz de `example-android-app-google-upp`)

En la sección "Como esta organizado el ejemplo", agregar un bullet mencionando que la app también
implementa App-to-App Verification (A2A) para el Yellow Path de Google Wallet.

## Tests a agregar

Bajo `app/app/src/test/java/com/example/example_google_upp/a2a/` (crear el paquete), replicando el
estilo de los tests existentes en `wallet/`:

- `VisaA2aPayloadTest.kt`:
  - payload válido → todos los campos parseados correctamente.
  - `extraText` nulo → `null`.
  - `extraText` blank/vacío → `null`.
  - Base64 corrupto (no decodifica) → `null`, no debe lanzar excepción.
  - JSON válido en Base64 pero con campos faltantes → payload con esos campos en `null`, no falla.
  - JSON con campos extra desconocidos → no falla (Gson ignora campos desconocidos por defecto).
- `CallerValidatorTest.kt`:
  - `"com.google.android.gms"` → `true`.
  - `null`, string vacío, cualquier otro package → `false`.
- `StepUpResultResolverTest.kt`:
  - `Approved` → extra `STEP_UP_RESPONSE == "approved"`.
  - `Declined` → `"declined"`.
  - `Failure(...)` → `"failure"`.
- `AppToAppViewModelTest.kt` (usar `kotlinx-coroutines-test`, ya es dependencia de test del
  proyecto):
  - Payload inválido o caller inválido al iniciar → resultado final `Failure` sin pasar por
    autenticación.
  - Flujo feliz: `onSimulatedAuthenticationConfirmed()` → `onActivate()` con
    `BackendService.activateToken` mockeado exitoso → resultado final `Approved`.
  - `onAuthenticationDeclined()` → resultado final `Declined`, y verificar que
    `BackendService.activateToken` **no** fue invocado (mock/verify).
  - `onActivate()` con `BackendService.activateToken` mockeado lanzando excepción → resultado final
    `Failure`.
- `BackendServiceTest.kt` (archivo ya existente, agregar un test más siguiendo el mismo patrón que
  los tests actuales con el mock engine de Ktor): request a `tokens/{id}/activate` con el body
  correcto (`{"motive":"APP_TO_APP_ACTIVATION"}`) y manejo de respuesta exitosa/error.

## Pruebas manuales (para documentar en el README, no hace falta ejecutarlas como parte del PR)

Reusar los comandos `adb` de la guía tal cual, con el `applicationId` real de este proyecto:

```bash
# Codificar un payload de ejemplo en Base64URL
echo -n '{"panReferenceID":"V-3815023863409817870482","tokenRequestorID":"42301999123","tokenReferenceID":"DNITHE381502386342002358","panLast4":"1234"}' \
  | base64 | tr '+/' '-_'

# Simular el intent de Google Wallet
adb shell am start \
  -a com.example.example_google_upp.a2a \
  -p com.example.example_google_upp \
  --es android.intent.extra.TEXT 'tuPayloadEnBase64URL'
```

## Fuera de alcance (no implementar)

- Certificación con Google (la guía aclara explícitamente que no es necesaria para A2A).
- Autenticación biométrica real (`BiometricPrompt`) — se simula, según la tabla de decisiones.
- Lookup de tarjeta completa (`cardholderName`/`brand`) vía `/cards` — no hay dato para hacerlo.
- Cualquier cambio en `backend-app/` — el endpoint de activación ya existe y no se toca.

## Checklist de aceptación

- [ ] `AndroidManifest.xml` declara el intent-filter `com.example.example_google_upp.a2a` en la
      nueva Activity.
- [ ] El payload de Visa se parsea correctamente en Base64URL + JSON (con Gson), tolerando errores.
- [ ] Se valida `callingPackage == "com.google.android.gms"` antes de mostrar cualquier dato.
- [ ] La UI muestra los últimos 4 dígitos de la tarjeta y permite simular autenticación y activar.
- [ ] `BackendService.activateToken(...)` llama a `POST tokens/{tokenId}/activate` con
      `motive = "APP_TO_APP_ACTIVATION"`.
- [ ] Se devuelve `STEP_UP_RESPONSE` con `"approved"`, `"declined"` o `"failure"` según corresponda.
- [ ] Si el cliente cancela/declina, **no** se llama a la API de activación.
- [ ] Todos los tests nuevos pasan (`./gradlew :app:testDebugUnitTest` desde `app/`).
- [ ] `app/README.md` y `README.md` (raíz) documentan el flujo con diagrama y referencias, siguiendo
      el mismo estilo que la sección de Bounce Provisioning.
- [ ] No se modificó nada en `backend-app/`.

## Convención de branch y commits

Según las reglas globales del usuario: los prefijos de branch válidos son únicamente `feature/`,
`temporaldev/`, `hotfix/`, `release/` y `temporalstg/`. La branch activa al momento de escribir este
spec es `feature/google-a2a` (base: `develop`) — reusarla si sigue siendo la branch actual, o crear
una nueva con prefijo `feature/` si no.

Commits sugeridos (uno por unidad lógica, no hace falta que sean exactamente estos):

1. `feat(app): parse Visa A2A payload and validate caller`
2. `feat(app): add App-to-App verification screen and view model`
3. `feat(app): wire token activation endpoint in BackendService`
4. `feat(app): register A2A activity in manifest and DI`
5. `docs: document App-to-App verification flow in app/README.md`

Antes de dar por terminada la implementación, correr los tests de `app/` (Gradle) y confirmar que
compilan y pasan.
