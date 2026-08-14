# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

> [!IMPORTANT]
> App Android de ejemplo para integrar Google Tap And Pay (UPP) Push Provisioning. Es codigo de referencia, no de produccion.

## Stack & Arquitectura

Kotlin + Jetpack Compose + Koin (DI) + Ktor (cliente HTTP) + Gson + Google Play Services Tap And Pay SDK. `minSdk = 24`, `compileSdk = 36`. Formato de codigo con **ktfmt** (estilo Kotlinlang, ver `.idea/ktfmt.xml`).

## Comandos (desde `app/`)

```bash
./gradlew assembleDebug         # compilar APK debug
./gradlew testDebugUnitTest     # unit tests (JVM, src/test)
./gradlew connectedAndroidTest  # instrumented tests (requiere emulador/dispositivo, src/androidTest)
./gradlew lint                  # Android lint
```

Para correr un solo test unitario:
```bash
./gradlew testDebugUnitTest --tests "com.example.example_google_upp.wallet.PushProvisioningResultResolverTest"
```

La URL del backend esta hardcodeada por build type en `app/build.gradle.kts` (`BuildConfig.BACKEND_BASE_URL`): `http://127.0.0.1:3000/` en debug, `http://10.0.2.2:3000/` (loopback del emulador Android) en release. Para probar contra un backend local hay que tener `backend-app` corriendo en el puerto 3000 (ver `backend-app/CLAUDE.md`).

## Organizacion del codigo

DI con Koin, un unico modulo en `di/AppModule.kt` que resuelve: `BackendService` (cliente HTTP), `TapAndPayClient` (SDK de Google, via `TapAndPay.getClient`), `WalletProvisioningGateway` (interfaz implementada por `TapAndPayService`) y el `CardSearchViewModel`.

Paquetes bajo `com.example.example_google_upp/`:
- `wallet/` — toda la integracion con el SDK Tap And Pay: `TapAndPayService` (llamadas al `TapAndPayClient`), `PomeloCredentialsGenerator` (implementa `PaymentCredentialsGenerator`, genera los OPCs pidiendoselos al backend), `PushProvisioningResultResolver` (traduce el resultado de la Activity de Google Wallet a `PushProvisioningResult`: Success/Cancelled/Error). Tambien vive aca `VisaAppToAppVerificationActivity` (flujo App2App/IDV, ver mas abajo) y `wallet/models/` con `VisaAppToAppPayload` y `VisaAppToAppStepUpResponse`.
- `data/` — `BackendService` (cliente Ktor hacia el backend) y los DTOs de red (`data/dto/`), con extensiones de mapeo DTO → modelo de dominio (`data/dto/BackendMappingExtensions.kt`).
- `model/` — modelos de dominio (`Card`, `User`, `ProvisioningData`, `AppToAppActivationResult`).
- `ui/` — pantalla de busqueda de tarjeta en Compose (`CardSearchScreen`, `CardSearchRoute`, `CardSearchViewModel`, `CardSearchUiState`) y la pantalla de confirmacion App2App (`VisaAppToAppScreen`, `VisaAppToAppViewModel`, `VisaAppToAppUiState`).
- `components/` — `GoogleWalletProvisionButton` (boton oficial "Add to Google Wallet" via Provision Button API) y `PomeloCardComposable` (reusado tambien en la pantalla App2App).

## Flujo clave del SDK Tap And Pay

Entender esto antes de tocar codigo de `wallet/` (diagramas completos con mas detalle en `README.md`):

1. `MainActivity` registra `registerForActivityResult(...)` y un `TapAndPay.DataChangedListener`, y llama a `CardSearchViewModel.refreshWalletEligibility()` en `onResume` porque el estado de Wallet puede cambiar fuera de la app.
2. `TapAndPayService.isTokenized(card)` determina si la tarjeta ya esta en Google Wallet para decidir el estado del boton (`ALREADY_ADDED` / `READY_TO_ADD` / `UNAVAILABLE`).
3. `TapAndPayService.createPushTokenizePendingIntent(card)` arma el `PushTokenizeRequest` con `PomeloCredentialsGenerator` como `PaymentCredentialsGenerator`, y `MainActivity` lanza el `PendingIntent` resultante.
4. Cuando Google Wallet necesita OPCs, invoca `PomeloCredentialsGenerator.generate(request)`, que llama a `BackendService.getProvisioningData(...)` (Visa o Mastercard segun `card.brand`) y arma el `GeneratePaymentCredentialsResponse`.
5. `MainActivity` recibe el resultado de la Activity, lo delega a `PushProvisioningResultResolver`, y el `CardSearchViewModel` actualiza la UI segun Success/Cancelled/Error.

Referencias oficiales del SDK usadas por este ejemplo estan linkeadas en `README.md` (Provision Button API, isTokenized, Data Change Callbacks, pushTokenize, PaymentCredentialsGenerator, etc.) — consultalas ante cualquier duda de comportamiento del SDK en vez de asumir.

## Flujo App2App (IDV) para Visa

Flujo independiente del anterior: no usa el SDK Tap And Pay, es un handoff de Intent + llamada HTTP propia. Detalle completo en `README.md`, seccion "App2App (IDV) para Visa".

1. Google Wallet lanza `VisaAppToAppVerificationActivity` con un Intent explicito (action `<applicationId>.a2a`) y el payload Visa en `Intent.EXTRA_TEXT` (Base64URL + JSON).
2. La Activity valida `callingPackage == "com.google.android.gms"` (en debug tambien acepta `null`, para poder testear con `adb shell am start`), decodifica el payload con `VisaAppToAppPayload`, y pide autenticacion con `BiometricPrompt` antes de mostrar nada — es solo un ejemplo runnable, no una recomendacion de estrategia de autenticacion.
3. Si la autenticacion es exitosa, muestra `VisaAppToAppScreen` (reusa `PomeloCardComposable`). Al tocar "Activar", `VisaAppToAppViewModel` llama a `BackendService.activateAppToAppToken(tokenId, deviceId)`.
4. El backend hace passthrough hacia Pomelo (`POST /tokens/:id/app-to-app-activation`, ver `backend-app/CLAUDE.md`), que activa el token en Visa (Token Lifecycle API, "Opcion 1": sin devolver codigo de autenticacion/TAV).
5. El resultado (`APPROVED`/`DECLINED`/`FAILURE`) se mapea a `AppToAppActivationResult` y luego, via `VisaAppToAppStepUpResponse`, al extra `STEP_UP_RESPONSE` que la Activity devuelve a Google Wallet con `setResult(RESULT_OK, ...)`.

Nota de naming: solo lo que es especifico de Visa (Activity, ViewModel, UiState, Screen, payload, mapeo de resultado) esta prefijado `Visa*`. `AppToAppActivationResult` y el endpoint del backend quedan sin prefijo porque ya son genericos (el mismo contrato serviria para otro TSP el dia de manana).
