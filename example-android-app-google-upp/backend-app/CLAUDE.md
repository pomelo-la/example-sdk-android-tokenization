# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Purpose

This is a reference backend for Pomelo clients migrating to Google UPP Push Provisioning SDK. It exposes simple passthrough endpoints to Pomelo for card/user lookups, push provisioning flows (Mastercard and Visa Google Pay), and App2App (IDV) token activation for Visa.

## Stack & Architecture

- **Runtime**: Node.js (ESM) + TypeScript + Hono (lightweight HTTP framework)
- **HTTP Client**: Axios with interceptors for auth handling
- **Validation**: Zod + `@hono/zod-validator`
- **Env Validation**: `@t3-oss/env-core` (see `src/env.ts`)
- **Testing**: Node.js built-in `test` module (`node:test`)
- **Code Quality**: Biome (format + lint)

## Key Design Patterns

### Auth Flow
1. **Bootstrap on Startup**: `src/auth/bootstrap-token.ts` fetches M2M token from `POST /oauth/token` when the app starts
2. **Token Storage**: Token is stored in-memory in `src/auth/token-store.ts` (access token, expiry, type)
3. **Auto-Injection**: `pomeloHttp` (Axios instance in `src/clients/pomelo-http.ts`) automatically adds `Authorization: Bearer ...` header via interceptor
4. **Startup Blocking**: If token fetch fails, the app does not start the HTTP server

### Route Structure
- Routes are Hono handlers exported directly from `src/routes/*.ts`
- No service/factory abstraction layer (intentional to keep this simple)
- All routes use Zod validators from `src/lib/schemas.ts`
- Request/response validation centralized in `src/lib/request-validator.ts`

### Code Organization
```
src/
  index.ts                 # Main app & startup logic
  env.ts                   # Environment config schema
  clients/pomelo-http.ts   # Shared Axios instance with interceptors
  auth/                    # Token bootstrap & storage
  lib/                     # Shared validation & schemas
  routes/                  # Endpoint handlers
```

## Commands

### Development
```bash
npm run dev          # Watch-mode TypeScript development server on port 3000
npm test             # Run all tests in tests/**/*.test.ts
npm test -- --grep "pattern"  # Run tests matching a pattern
npm run build        # Compile TypeScript to dist/
npm start            # Run built dist/index.js (requires .env)
```

### Single Test Execution
```bash
npm test -- --grep "test name pattern"
```

### Linting & Formatting
```bash
npx biome check       # Check formatting and lints
npx biome check --write  # Auto-fix formatting and some lints
```

## Environment Setup

Copy `.env.example` to `.env` and populate with:
- `POMELO_BASE_URL`: Pomelo API base URL
- `POMELO_AUDIENCE`: OAuth audience
- `POMELO_CLIENT_ID`: M2M client ID
- `POMELO_CLIENT_SECRET`: M2M client secret

The app validates all required env vars at startup (see `src/env.ts`).

## Working Rules for This Codebase

1. **Minimize Abstractions**: Prefer direct changes. No unnecessary service/factory layers.
2. **Keep Patterns Consistent**:
   - Routes exported directly from `src/routes/`
   - App logic stays in `index.ts`
   - `pomeloHttp` is a singleton imported directly (not injected)
3. **Validation First**: Use Zod validators for all inputs via `@hono/zod-validator`
4. **Testing**: If a test imports `src/index.ts`, set up `process.env` before the import (token bootstrap happens at module load)
5. **Mocking pomeloHttp**: When mocking, restore its methods after each test to avoid state leakage

## Before Committing

Always run:
```bash
npm test
npm run build
```

Both must pass cleanly. Biome formatting is not enforced as a pre-commit step, but code should be lintable without errors.
