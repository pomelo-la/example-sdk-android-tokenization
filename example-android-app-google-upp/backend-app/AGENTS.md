# AGENTS.md

## Project Goal

- `backend-app/` es un backend de ejemplo para clientes de Pomelo que migren al SDK de Google UPP Push Provisioning.
- El backend expone endpoints simples y hace passthrough hacia Pomelo para:
  - `GET /cards/:id`
  - `GET /users/:id`
  - `POST /push-provisioning/mastercard/google-pay`
  - `POST /push-provisioning/visa/google-pay`
  - `POST /tokens/:id/app-to-app-activation` (App2App / IDV, Visa)

## Runtime

- Stack: Node.js + TypeScript + Hono + Axios.
- Env validation: `@t3-oss/env-core` en `src/env.ts`.
- Startup entrypoint: `src/index.ts`.
- Auth flow:
  - al arrancar, `bootstrapToken()` solicita el token M2M
  - el token se guarda en memoria en `src/auth/token-store.ts`
  - `pomeloHttp` agrega `Authorization: Bearer ...` por interceptor

## Working Rules

- Preferir cambios pequeños y directos. Este backend evita capas innecesarias a propósito.
- Mantener el patrón actual:
  - rutas Hono exportadas directamente
  - `index.ts` contiene la app y el startup
  - `pomeloHttp` es una instancia compartida importada directo
- No reintroducir services/factories genéricos salvo necesidad real.
- Mantener validaciones con `zod` y `@hono/zod-validator`.

## Files That Matter

- `src/index.ts`: app principal y arranque.
- `src/env.ts`: schema de env.
- `src/clients/pomelo-http.ts`: instancia Axios compartida con interceptores.
- `src/auth/bootstrap-token.ts`: obtención del token M2M.
- `src/routes/`: endpoints HTTP.
- `tests/`: tests con `node:test`.

## Commands

Desde `backend-app/`:

```bash
npm run dev
npm test
npm run build
```

- `dev` y `start` cargan `.env` usando `node --env-file=.env`.
- Tomar `.env.example` como base.

## Testing Notes

- Si un test importa `src/index.ts`, preparar `process.env` antes del import.
- Cuando se mockea `pomeloHttp`, restaurar sus métodos al final de cada test.
- Antes de cerrar cualquier cambio, correr:
  - `npm test`
  - `npm run build`
