# backend-app

> [!IMPORTANT]
> Este repositorio contiene un ejemplo basico de una integracion con Google Tap And Pay SDK.
> Usalo solo como muestra de referencia y valida siempre los detalles de implementacion con la documentacion oficial provista por Google.
> Bajo ninguna circunstancia deberia usarse el codigo de este repositorio en produccion.

Backend de ejemplo para integrar Pomelo Push Provisioning con Google UPP desde un servicio Node.js/TypeScript usando Hono.

## Endpoints expuestos

- `GET /cards/:id`
- `GET /users/:id`
- `POST /push-provisioning/mastercard/google-pay`
- `POST /push-provisioning/visa/google-pay`
- `POST /tokens/:id/activate`

## Variables de entorno

Requeridas:

- `POMELO_BASE_URL`
- `POMELO_AUDIENCE`
- `POMELO_CLIENT_ID`
- `POMELO_CLIENT_SECRET`

## Ejecutar localmente

```bash
npm install
npm run dev
```

## Build y tests

```bash
npm test
npm run build
```

## Ejemplos

### Obtener tarjeta

```bash
curl "http://localhost:3000/cards/crd-123"
```

Respuesta:

```json
{
  "cardId": "crd-123",
  "userId": "usr-123",
  "lastFour": "0317",
  "cardholderName": "Juan Perez",
  "brand": "VISA"
}
```

### Obtener usuario

```bash
curl "http://localhost:3000/users/usr-123"
```

Respuesta:

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

Respuesta:

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

Respuesta:

```json
{
  "opc": "eyJmdW5kaW5nQ ....",
  "google_opc": "eyJmdW5kaW5 ...."
}
```

### Activar token (App-to-App Verification)

Usado por el flujo A2A (Yellow Path) para activar un token luego de la verificacion biometrica en la app. Hace proxy hacia `POST /tokenization/v1/tokens/:id/activate` en Pomelo.

```bash
curl -X POST "http://localhost:3000/tokens/tok-123/activate" \
  -H "Content-Type: application/json" \
  -d '{
    "motive": "APP_TO_APP_ACTIVATION"
  }'
```

Respuesta: `202 Accepted` con el body devuelto por Pomelo.
