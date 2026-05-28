# backend-app

> [!IMPORTANT]
> This repository contains a basic example of a Google Tap And Pay SDK integration.
> Use it as a reference sample only, and always validate the implementation details against the official documentation provided by Google.
> Under no circumstances should the code in this repository be used in production.

Example backend for integrating Pomelo Push Provisioning with Google UPP from a Node.js/TypeScript service using Hono.

## Exposed Endpoints

- `GET /cards/:id`
- `GET /users/:id`
- `POST /push-provisioning/mastercard/google-pay`
- `POST /push-provisioning/visa/google-pay`

## Environment Variables

Required:

- `POMELO_BASE_URL`
- `POMELO_AUDIENCE`
- `POMELO_CLIENT_ID`
- `POMELO_CLIENT_SECRET`

## Run Locally

```bash
npm install
npm run dev
```

## Build and Tests

```bash
npm test
npm run build
```

## Examples

### Get Card

```bash
curl "http://localhost:3000/cards/crd-123"
```

Response:

```json
{
  "cardId": "crd-123",
  "userId": "usr-123",
  "lastFour": "0317",
  "cardholderName": "Juan Perez",
  "brand": "VISA"
}
```

### Get User

```bash
curl "http://localhost:3000/users/usr-123"
```

Response:

```json
{
  "id": "usr-123",
  "name": "Juan",
  "surname": "Perez",
  "gender": "MALE",
  "identification_type": "DNI",
  "identification_value": "12345678",
  "tax_identification_type": "CUIL",
  "tax_identification_value": "20123456785",
  "email": "juan.perez@pomelo.la",
  "phone": "541111111111",
  "birthdate": "1997-12-05",
  "legal_address": {
    "street_name": "Canonigo Miguel Calixto del Corro",
    "street_number": "312",
    "city": "Ciudad de Cordoba",
    "region": "Provincia de Cordoba",
    "zip_code": "1424",
    "country": "ARG"
  },
  "status": "ACTIVE",
  "operation_country": "ARG",
  "client_id": "cli-123"
}
```

### Push Provisioning Mastercard Google Pay

```bash
curl -X POST "http://localhost:3000/push-provisioning/mastercard/google-pay" \
  -H "Content-Type: application/json" \
  -d '{
    "card_id": "crd-123",
    "user_id": "usr-123"
  }'
```

Response:

```json
{
  "opc": "eyJmdW5kaW5nQ ....",
  "google_opc": "eyJmdW5kaW5 ...."
}
```

### Push Provisioning Visa Google Pay

```bash
curl -X POST "http://localhost:3000/push-provisioning/visa/google-pay" \
  -H "Content-Type: application/json" \
  -d '{
    "card_id": "crd-123",
    "user_id": "usr-123",
    "device_id": "device-123",
    "wallet_account_id": "wallet-123",
    "server_session_id": "session-123"
  }'
```

Response:

```json
{
  "opc": "eyJmdW5kaW5nQ ....",
  "google_opc": "eyJmdW5kaW5 ...."
}
```
