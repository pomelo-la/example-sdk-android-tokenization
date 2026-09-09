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

La app Android demuestra la integracion de Tap And Pay del lado cliente:

- renderizar el boton oficial de Google Wallet
- verificar el estado de tokenizacion
- lanzar `pushTokenize(...)`
- generar credenciales de pago mediante `PomeloCredentialsGenerator`
- manejar el resultado devuelto por Google Wallet
- soportar Bounce Provisioning, para que Google Wallet pueda redirigir al usuario a esta app y agregar la tarjeta sin navegacion adicional
- soportar App-to-App Verification (A2A) para el flujo Yellow Path de Google Wallet, con autenticación biométrica antes de mostrar cualquier dato de la tarjeta

La app backend demuestra la parte del servidor requerida por el ejemplo:

- exponer endpoints de consulta de tarjeta y usuario
- exponer endpoints de push provisioning para Mastercard y Visa
- hacer proxy de la solicitud de provisioning hacia Pomelo y devolver datos OPC al flujo cliente
- exponer el endpoint de activacion de token consumido por el flujo A2A tras la verificacion biometrica

## Diagrama de secuencia: Push Provisioning

Este es el flujo principal, disparado por el usuario desde la app al presionar "Agregar a Billetera de Google" (incluye tambien la variante Bounce Provisioning, donde Google Wallet es quien lanza la app).

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

## Diagrama de secuencia: App-to-App Verification (A2A)

Este es un flujo distinto e independiente del anterior: no lo inicia el usuario desde esta app, sino que Google Wallet lanza esta app cuando necesita verificar la identidad del titular antes de activar un token (Yellow Path), en lugar de enviar un OTP por SMS/email.

```mermaid
sequenceDiagram
    participant User as Tarjetahabiente
    participant GW as Google Wallet
    participant A2A as AppToAppVerificationActivity
    participant Bio as BiometricPrompt
    participant VM as AppToAppViewModel
    participant Backend as Backend App
    participant Pomelo as Pomelo

    User->>GW: Agrega tarjeta manualmente
    Note over GW: Yellow Path: requiere verificación
    GW->>A2A: Intent con action .a2a y payload Base64URL
    A2A->>A2A: Parsea payload y valida caller
    A2A->>Bio: authenticate()
    Note over Bio: Biometría ANTES de mostrar datos
    User->>Bio: Huella/Reconocimiento facial
    Bio-->>A2A: onAuthenticationSucceeded()
    A2A->>VM: onAuthenticationConfirmed()
    VM->>A2A: Estado: AwaitingConfirmation
    A2A->>User: Muestra •••• •••• •••• 1234
    User->>A2A: Presiona "Activar tarjeta"
    A2A->>VM: onActivate()
    VM->>A2A: Estado: Activating
    VM->>Backend: activateToken(tokenId, APP_TO_APP_ACTIVATION)
    Backend->>Pomelo: POST /tokens/{id}/activate
    Pomelo-->>Backend: 202 Accepted
    Backend-->>VM: Éxito
    VM->>A2A: Estado: Finished(Approved)
    A2A->>GW: setResult(STEP_UP_RESPONSE: "approved")
```

Para el detalle completo de este flujo (biometria, manejo de cancelacion/error, formato del payload de Visa) mira la seccion [App-to-App Verification (A2A)](app/README.md#app-to-app-verification-a2a) en `app/README.md`.

## Segui leyendo

- Para el flujo Android, los diagramas y las referencias del SDK, mira [`app/README.md`](app/README.md).
- Para los endpoints backend, las variables de entorno y las instrucciones de ejecucion local, mira [`backend-app/README.md`](backend-app/README.md).
