# Ejemplo de integracion con Google UPP

> [!IMPORTANT]
> Este repositorio contiene un ejemplo basico de una integracion con Google Tap And Pay SDK.
> Usalo solo como muestra de referencia y valida siempre los detalles de implementacion con la documentacion oficial provista por Google.
> Bajo ninguna circunstancia deberia usarse el codigo de este repositorio en produccion.

Este ejemplo se divide en dos partes:

- [`app/`](app/README.md): aplicacion de ejemplo para Android que integra Google Tap And Pay Push Provisioning y lanza el flujo de Google Wallet.
- [`backend-app/`](backend-app/README.md): backend de ejemplo en Node.js/TypeScript que expone los endpoints de tarjeta, usuario y push provisioning consumidos por la app Android.

## Estructura del repositorio

```text
example-android-app-google-upp/
├── app/
└── backend-app/
```

## Como esta organizado el ejemplo

Este ejemplo cubre dos flujos independientes de Google Wallet:

1. **Push provisioning manual** (Visa y Mastercard): el usuario toca "Agregar a Billetera de Google" dentro de la app.
2. **App2App / IDV** ("yellow path", solo Visa): Google Wallet invoca la app para verificar la identidad del cardholder y activar un token existente.

La app Android demuestra la integracion de Tap And Pay del lado cliente:

- renderizar el boton oficial de Google Wallet
- verificar el estado de tokenizacion
- lanzar `pushTokenize(...)`
- generar credenciales de pago mediante `PomeloCredentialsGenerator`
- manejar el resultado devuelto por Google Wallet
- recibir el Intent de App2App de Google Wallet, autenticar al cardholder (`BiometricPrompt`) y confirmar la activacion del token

La app backend demuestra la parte del servidor requerida por el ejemplo:

- exponer endpoints de consulta de tarjeta y usuario
- exponer endpoints de push provisioning para Mastercard y Visa
- hacer proxy de la solicitud de provisioning hacia Pomelo y devolver datos OPC al flujo cliente
- hacer proxy de la activacion de token App2App hacia Pomelo (Visa)

## Diagrama de secuencia: Push Provisioning
```mermaid
sequenceDiagram
    participant User as Tarjetahabiente
    participant App as App Mobile
    participant SDK as Tap And Pay SDK
    participant Backend as Backend App
    participant Pomelo as Pomelo

    User->>App: Presiona el botón "Agregar a Billetera de Google"

    Note over App: La app construye PushTokenizeRequest y configura PaymentCredentialsGenerator
    App->>SDK: pushTokenize
    SDK-->>App: Solicita las credenciales de pago
    App->>Backend: Solicita OPCs para la tarjeta<br/>GeneratePaymentCredentialsRequest y datos de tokenización

    Backend->>Pomelo: Solicita TSP OPC y Google OPC
    Note over Pomelo: Genera TSP OPC y Google OPC
    Pomelo-->>Backend: TSP OPC y Google OPC

    Backend-->>App: TSP OPC y Google OPC
    App-->>SDK: TSP OPC y Google OPC

    Note over App: Espera onActivityResult
    Note over SDK: Flujo de tokenización del dispositivo en Google Pay

    SDK-->>App: Devuelve resultado de pushTokenize<br/>PushTokenizeResult y TokenizationOutcome
    Note over App: Procesa el resultado
    App-->>User: Muestra la confirmación

```

## Diagrama de secuencia: App2App (IDV) para Visa

> [!IMPORTANT]
> Este flujo es independiente del push provisioning manual de arriba: acá Google Wallet inicia el Intent, no la app, y en ningún momento se llama al SDK Tap And Pay.

```mermaid
sequenceDiagram
    participant Wallet as Google Wallet
    participant App as App Mobile
    participant Bio as Biometría del dispositivo
    participant Backend as Backend App
    participant Pomelo as Pomelo
    participant Visa as Visa (Token Lifecycle API)

    Note over Wallet: Visa determina que el token necesita<br/>verificación de identidad (IDV / "yellow path")
    Wallet->>App: Intent explícito (action "{applicationId}.a2a")<br/>EXTRA_TEXT: payload Base64URL+JSON

    Note over App: Valida que el caller sea Google Wallet<br/>(callingPackage == com.google.android.gms)
    App->>App: Decodifica VisaAppToAppPayload<br/>(tokenReferenceID, panLast4, deviceID, ...)

    App->>Bio: BiometricPrompt.authenticate()
    Bio-->>App: Resultado de autenticación

    alt Autenticación fallida, cancelada o no disponible
        App-->>Wallet: setResult(RESULT_OK, STEP_UP_RESPONSE=declined)
    else Autenticación exitosa
        Note over App: Muestra pantalla de confirmación (card + Activar/Cancelar)
        App->>Backend: POST /tokens/:id/app-to-app-activation<br/>{ device_id }
        Backend->>Pomelo: POST /tokenization/v1/tokens/:id/app-to-app-activation
        Pomelo->>Visa: Activa el token (Token Lifecycle API)
        Visa-->>Pomelo: Resultado de activación
        Pomelo-->>Backend: { external_token_id, activation_result }
        Backend-->>App: activation_result (APPROVED/DECLINED/FAILURE)
        App-->>Wallet: setResult(RESULT_OK, STEP_UP_RESPONSE=approved/declined/failure)
    end
```

## Segui leyendo

- Para el flujo Android, los diagramas y las referencias del SDK, mira [`app/README.md`](app/README.md).
- Para los endpoints backend, las variables de entorno y las instrucciones de ejecucion local, mira [`backend-app/README.md`](backend-app/README.md).
