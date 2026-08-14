# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

> [!IMPORTANT]
> Este repositorio es un ejemplo de referencia de integracion con **Google Tap And Pay (UPP) Push Provisioning**, provisto por Pomelo. No es codigo de produccion: el foco es mostrar el flujo end-to-end, no cubrir todos los casos borde.

## Estructura del repositorio

Dos proyectos independientes que se ejecutan juntos para demostrar el flujo completo:

- `app/` — app Android (Kotlin + Compose) que integra el SDK Tap And Pay de Google y lanza el boton de Google Wallet. Ver [`app/CLAUDE.md`](app/CLAUDE.md) para arquitectura y comandos especificos.
- `backend-app/` — backend Node.js/TypeScript (Hono) que la app Android consume; hace passthrough de las llamadas de provisioning hacia Pomelo. Ver [`backend-app/CLAUDE.md`](backend-app/CLAUDE.md) y [`backend-app/AGENTS.md`](backend-app/AGENTS.md) para arquitectura, comandos y reglas de testing.

Cada subproyecto es autocontenido (su propio gradle wrapper / package.json) y tiene su propio README. Al trabajar dentro de `app/` o `backend-app/`, prioriza las reglas de su `CLAUDE.md` especifico por sobre este archivo raiz.

## Flujo end-to-end

Este ejemplo cubre dos flujos independientes de Google Wallet:

1. **Push provisioning manual** (Visa y Mastercard): el usuario toca el boton de Google Wallet en la app → la app llama a `pushTokenize` del SDK de Tap And Pay → cuando Google Wallet necesita credenciales de pago, invoca `PomeloCredentialsGenerator` → la app le pide al backend los OPCs → el backend hace proxy hacia Pomelo → los OPCs vuelven hasta el SDK, que completa la tokenizacion.
2. **App2App / IDV** ("yellow path", solo Visa): Google Wallet invoca la app via un Intent explicito cuando un token necesita verificacion de identidad del cardholder → la app autentica al usuario (`BiometricPrompt` en este ejemplo) → la app le pide al backend que active el token → el backend hace proxy hacia Pomelo, que activa el token en Visa → la app devuelve el resultado a Google Wallet. No usa el SDK Tap And Pay.

Diagramas de secuencia completos de ambos flujos en [`README.md`](README.md).
