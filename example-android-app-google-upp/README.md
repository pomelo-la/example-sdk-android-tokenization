# Example Google UPP integration

> [!IMPORTANT]
> This repository contains a basic example of a Google Tap And Pay SDK integration.
> Use it as a reference sample only, and always validate the implementation details against the official documentation provided by Google.
> Under no circumstances should the code in this repository be used in production.

This example is split into two parts:

- [`app/`](app/README.md): Android sample app that integrates Google Tap And Pay Push Provisioning and launches the Google Wallet flow.
- [`backend-app/`](backend-app/README.md): Node.js/TypeScript sample backend that exposes the card, user, and push-provisioning endpoints consumed by the Android app.

## Repository structure

```text
example-android-app-google-upp/
├── app/
└── backend-app/
```

## How the example is organized

The Android app demonstrates the client-side Tap And Pay integration:

- render the official Google Wallet button
- check tokenization state
- launch `pushTokenize(...)`
- generate payment credentials through `PomeloCredentialsGenerator`
- handle the result returned by Google Wallet

The backend app demonstrates the server-side piece required by the sample:

- expose card and user lookup endpoints
- expose push provisioning endpoints for Mastercard and Visa
- proxy the provisioning request to Pomelo and return OPC data to the client flow

## Read next

- For the Android flow, diagrams, and SDK references, see [`app/README.md`](app/README.md).
- For backend endpoints, environment variables, and local run instructions, see [`backend-app/README.md`](backend-app/README.md).
