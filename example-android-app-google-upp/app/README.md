# Ejemplo de app Android con Google UPP

> [!IMPORTANT]
> Este repositorio contiene un ejemplo basico de una integracion con Google Tap And Pay SDK.
> Usalo solo como muestra de referencia y valida siempre los detalles de implementacion con la documentacion oficial provista por Google.
> Bajo ninguna circunstancia deberia usarse el codigo de este repositorio en produccion.


Esta carpeta contiene una app Android de ejemplo que demuestra una integracion con Google Tap And Pay Push Provisioning. El foco esta en el flujo de la app cliente: renderizar el boton oficial de Google Wallet, verificar el estado de tokenizacion, lanzar `pushTokenize(...)`, generar credenciales de Pomelo, resolver el resultado devuelto por Google Wallet y soportar Bounce Provisioning.

## Piezas clave

### GoogleWalletProvisionButton

[GoogleWalletProvisionButton.kt](app/src/main/java/com/example/example_google_upp/components/GoogleWalletProvisionButton.kt) es el punto de entrada en Compose para renderizar el boton oficial "Add to Google Wallet".

La app usa la Provision Button API con integracion programatica mediante `ProvisionButton`. Esto evita mantener assets estaticos del boton y le permite al SDK encargarse del branding, la localizacion y el escalado. La API soporta dos modos de visualizacion: `PROVISION_BUTTON_DISPLAY_MODE_PRIMARY` y `PROVISION_BUTTON_DISPLAY_MODE_CONDENSED`; este ejemplo usa `PRIMARY`.

Referencia oficial: [Provision Button API](https://developers.google.com/pay/issuers/apis/push-provisioning/android/provision-button-api)

### MainActivity

[MainActivity.kt](app/src/main/java/com/example/example_google_upp/MainActivity.kt) conecta la UI de Compose con el flujo de Tap And Pay. Sus responsabilidades son:

- Registrar `registerForActivityResult(...)` para escuchar el resultado de la Activity de Google Wallet.
- Registrar `TapAndPay.DataChangedListener` para refrescar el estado local cuando cambia Wallet.
- Revalidar elegibilidad en `onResume`, porque el estado de Wallet puede cambiar fuera de la app.
- Detectar si la Activity fue lanzada por Google Wallet via Bounce Provisioning (`intent.action == ACTION_INITIATE_PROVISIONING`).
- Solicitar el `PendingIntent` de push tokenization y lanzarlo con `IntentSenderRequest`, marcando `isBounceProvisioned` cuando corresponde.
- Delegar la interpretacion del resultado en `PushProvisioningResultResolver`.
- Cerrar la Activity (`finish()`) al resolver el resultado cuando el flujo vino de Bounce Provisioning, para devolver al usuario a Google Wallet.

Referencias oficiales:

- [Handling result callbacks](https://developers.google.com/pay/issuers/apis/push-provisioning/android/wallet-operations#handling_result_callbacks)
- [Data Change Callbacks](https://developers.google.com/pay/issuers/apis/push-provisioning/android/reading-wallet#data_change_callbacks)
- [Bounce Provisioning](https://developers.google.com/pay/issuers/apis/push-provisioning/android/bounce-provisioning)

```mermaid
flowchart TD
    A["User taps GoogleWalletProvisionButton"] --> B["MainActivity.launchProvisioningIntent(card)"]
    B --> C["TapAndPayService.createPushTokenizePendingIntent(card, isBounceProvisioned)"]
    C --> D["Build PushTokenizeRequest"]
    D --> E["Attach PomeloCredentialsGenerator + PushTokenizeExtraOptions"]
    E --> F["tapAndPayClient.pushTokenize(request)"]
    F --> G["Google Wallet provisioning flow"]
    G --> H["MainActivity registerForActivityResult callback"]
    H --> I["PushProvisioningResultResolver.resolve(...)"]
    I --> J["CardSearchViewModel.onProvisioningResult(...)"]
    J --> K{"Resolved result"}
    K -->|Success| L["Show success snackbar and refresh UI state"]
    K -->|Cancelled| M["Show cancellation snackbar"]
    K -->|Error| N["Show error snackbar"]
    L --> O{"isBounceProvisioning?"}
    M --> O
    N --> O
    O -->|Si| P["MainActivity.finish() -> vuelve a Google Wallet"]
    O -->|No| Q["Usuario permanece en la app"]
```

### CardSearchViewModel.refreshWalletEligibility

[CardSearchViewModel.kt](app/src/main/java/com/example/example_google_upp/ui/CardSearchViewModel.kt) maneja el estado de UI de la tarjeta seleccionada.

`refreshWalletEligibility()` sincroniza el estado del boton de Google Wallet. Se llama despues de eventos relevantes para Wallet como `onResume`, los callbacks de `DataChangedListener` y un provisioning exitoso. La funcion cancela cualquier verificacion previa, llama a `walletProvisioningGateway.isTokenized(card)` y mapea el resultado a la UI:

- `isTokenized == true` -> `WalletButtonState.ALREADY_ADDED`
- `isTokenized == false` -> `WalletButtonState.READY_TO_ADD`
- error al verificar Tap And Pay -> `WalletButtonState.UNAVAILABLE`

La documentacion de Tap And Pay recomienda actualizar la UI cuando la Activity vuelve al foreground y cuando llega un callback de cambio de datos.

```mermaid
flowchart TD
    A["CardSearchRoute selects a card"] --> B["CardSearchViewModel.refreshWalletEligibility()"]
    C["MainActivity.onResume()"] --> B
    D["TapAndPay.DataChangedListener callback"] --> B
    E["PushProvisioningResult.Success"] --> B

    B --> F["TapAndPayService.isTokenized(card)"]
    F --> G{"Result"}
    G -->|Tokenized| H["Show ALREADY_ADDED state"]
    G -->|Not tokenized| I["Show GoogleWalletProvisionButton"]
    G -->|Error| J["Show UNAVAILABLE state"]
```

### TapAndPayService

[TapAndPayService.kt](app/src/main/java/com/example/example_google_upp/wallet/TapAndPayService.kt) es la puerta de entrada de la app hacia `TapAndPayClient` y el backend emisor.

`isTokenized(card)` construye un `IsTokenizedRequest` con los ultimos cuatro digitos de la tarjeta, la red y el token service provider. Devuelve `true` o `false` segun si Tap And Pay encuentra esa tarjeta tokenizada en Google Wallet del dispositivo actual.

Referencia oficial: [isTokenized](https://developers.google.com/pay/issuers/apis/push-provisioning/android/reading-wallet#istokenized)

`createPushTokenizePendingIntent(card, isBounceProvisioned)` obtiene el usuario desde `BackendService`, construye `UserAddress`, arma el `PushTokenizeRequest` y llama a `tapAndPayClient.pushTokenize(request)`. La solicitud pasa `PomeloCredentialsGenerator` como `PaymentCredentialsGenerator`, que es el componente que Google Wallet invoca cuando necesita credenciales OPC. Cuando `isBounceProvisioned` es `true`, la request tambien incluye `PushTokenizeExtraOptions.setIsBounceProvisioned(true)`, para que Google Wallet identifique que el token se agrego a traves del flujo de Bounce Provisioning.

Referencias oficiales:

- [pushTokenize](https://developers.google.com/pay/issuers/apis/push-provisioning/android/wallet-operations#pushtokenize)
- [Bounce Provisioning](https://developers.google.com/pay/issuers/apis/push-provisioning/android/bounce-provisioning)

### PomeloCredentialsGenerator

[PomeloCredentialsGenerator.kt](app/src/main/java/com/example/example_google_upp/wallet/PomeloCredentialsGenerator.kt) implementa la interfaz `PaymentCredentialsGenerator` de Tap And Pay.

Su responsabilidad es generar credenciales del emisor cuando Google Wallet llega al paso de push tokenization que requiere datos OPC. El metodo `generate(request)` recibe un `GeneratePaymentCredentialsRequest` con contexto provisto por Google:

- `serverSessionId`
- `stableHardwareId`
- `walletId`

La app envia esos valores a Pomelo mediante `BackendService.getProvisioningData(...)` y usa la respuesta para construir un `GeneratePaymentCredentialsResponse` con:

- `setOpaquePaymentCard(...)`
- `setGoogleOpaquePaymentCard(...)`

La implementacion devuelve un `Future<GeneratePaymentCredentialsResponse>`, tal como espera la interfaz del SDK.

Referencias oficiales:

- [PaymentCredentialsGenerator interface](https://developers.google.com/pay/issuers/apis/push-provisioning/android/wallet-operations#paymentcredentialsgenerator_interface)
- [GeneratePaymentCredentialsRequest interface](https://developers.google.com/pay/issuers/apis/push-provisioning/android/wallet-operations#generatepaymentcredentialsrequest_interface)
- [GeneratePaymentCredentialsResponse interface](https://developers.google.com/pay/issuers/apis/push-provisioning/android/wallet-operations#generatepaymentcredentialsresponse_interface)

### PushProvisioningResultResolver

[PushProvisioningResultResolver.kt](app/src/main/java/com/example/example_google_upp/wallet/PushProvisioningResultResolver.kt) traduce el resultado de Google Wallet al modelo interno `PushProvisioningResult`.

`MainActivity` delega en este resolver el `activityResultCode` y el `Intent` recibidos desde Google Wallet. El resolver lee `TapAndPay.EXTRA_PUSH_TOKENIZE_RESULT` cuando esta disponible y devuelve uno de tres resultados simples para que el `ViewModel` actualice la UI:

- `Success`: la tokenizacion fue exitosa.
- `Cancelled`: Tap And Pay devolvio un estado de cancelacion, como `TAP_AND_PAY_USER_CANCELED_FLOW` o `CANCELED`.
- `Error`: falta el payload de Tap And Pay, faltan los resultados de tokenizacion, fallo el guardado de FPAN o se devolvio cualquier otro estado de error.

Este handler es intencionalmente basico para la app de ejemplo: no intenta modelar una UI avanzada para cada codigo de error, solo distingue entre exito, cancelacion y error.

Referencia oficial: [Sample code for pushTokenize(...)](https://developers.google.com/pay/issuers/apis/push-provisioning/android/upgrade_to_upp#sample_code_for_pushtokenize)

### Bounce Provisioning

Bounce Provisioning le permite a Google Wallet redirigir al usuario desde la propia app de Wallet hacia esta app emisora, cuando la tarjeta ya es conocida por Google pero todavia no fue agregada. El objetivo es que el usuario llegue directo a la pantalla de "Agregar a Google Wallet" de esta app, sin navegacion adicional.

La integracion tiene tres partes:

1. **Manifest**: se declara un `intent-filter` con la accion `com.google.android.gms.tapandpay.issuer.ACTION_INITIATE_PROVISIONING` y la categoria `DEFAULT` en `MainActivity`. Google Wallet usa `queryIntentActivities` para descubrir que apps emisoras lo soportan.
2. **MainActivity**: al recibir ese intent, la Activity no agrega ningun paso de navegacion extra -- el buscador de tarjetas ya se muestra de entrada cuando no hay una tarjeta seleccionada -- pero guarda el flag `isBounceProvisioning` para propagarlo al `pushTokenize` y para cerrarse (`finish()`) una vez resuelto el resultado, devolviendo al usuario a Google Wallet.
3. **TapAndPayService**: agrega `PushTokenizeExtraOptions.setIsBounceProvisioned(true)` al `PushTokenizeRequest` cuando el flag esta activo.

```mermaid
sequenceDiagram
    actor User
    participant GW as Google Wallet
    participant IA as Esta app (Issuer app)
    participant TAP as Tap And Pay SDK

    User->>GW: Presiona "Agregar tarjeta de pago"
    Note over GW: Descubre apps emisoras con ACTION_INITIATE_PROVISIONING
    User->>GW: Elige esta app
    GW->>IA: Lanza MainActivity con ACTION_INITIATE_PROVISIONING
    Note over IA: Muestra directo la pantalla de agregar tarjeta
    User->>IA: Presiona "Add to Google Wallet"
    IA->>TAP: pushTokenize(..., PushTokenizeExtraOptions(isBounceProvisioned=true))
    TAP-->>IA: onActivityResult
    IA-->>User: Muestra confirmacion
    IA->>GW: finish() -> vuelve a Google Wallet
```

Referencia oficial: [Bounce Provisioning](https://developers.google.com/pay/issuers/apis/push-provisioning/android/bounce-provisioning)

## Referencias del SDK usadas por la app

- [Provision Button API](https://developers.google.com/pay/issuers/apis/push-provisioning/android/provision-button-api)
- [isTokenized](https://developers.google.com/pay/issuers/apis/push-provisioning/android/reading-wallet#istokenized)
- [Data Change Callbacks](https://developers.google.com/pay/issuers/apis/push-provisioning/android/reading-wallet#data_change_callbacks)
- [pushTokenize](https://developers.google.com/pay/issuers/apis/push-provisioning/android/wallet-operations#pushtokenize)
- [Bounce Provisioning](https://developers.google.com/pay/issuers/apis/push-provisioning/android/bounce-provisioning)
- [Handling result callbacks](https://developers.google.com/pay/issuers/apis/push-provisioning/android/wallet-operations#handling_result_callbacks)
- [PaymentCredentialsGenerator interface](https://developers.google.com/pay/issuers/apis/push-provisioning/android/wallet-operations#paymentcredentialsgenerator_interface)
- [GeneratePaymentCredentialsRequest interface](https://developers.google.com/pay/issuers/apis/push-provisioning/android/wallet-operations#generatepaymentcredentialsrequest_interface)
- [GeneratePaymentCredentialsResponse interface](https://developers.google.com/pay/issuers/apis/push-provisioning/android/wallet-operations#generatepaymentcredentialsresponse_interface)
- [Sample code for pushTokenize(...)](https://developers.google.com/pay/issuers/apis/push-provisioning/android/upgrade_to_upp#sample_code_for_pushtokenize)
