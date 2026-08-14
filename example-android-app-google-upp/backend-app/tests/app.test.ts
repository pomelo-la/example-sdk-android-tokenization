import assert from "node:assert/strict";
import test from "node:test";
import axios from "axios";
import { HTTPException } from "hono/http-exception";

process.env.POMELO_BASE_URL = "https://api.pomelo.la";
process.env.POMELO_AUDIENCE = "https://auth-dev.pomelo.la";
process.env.POMELO_CLIENT_ID = "client-id";
process.env.POMELO_CLIENT_SECRET = "client-secret";

const [{ default: app }, { pomeloHttp }] = await Promise.all([
  import("../src/index.js"),
  import("../src/clients/pomelo-http.js"),
]);

const originalGet = pomeloHttp.get.bind(pomeloHttp);
const originalPost = pomeloHttp.post.bind(pomeloHttp);

const mockPomeloHttp = () => {
  pomeloHttp.get = (async <_T = unknown>(path: string) => {
    if (path.startsWith("/cards/v1/")) {
      return {
        data: {
          data: {
            id: path.replace("/cards/v1/", ""),
            user_id: "usr-123",
            last_four: "1573",
            name_on_card: "Dieguito",
            provider: "MASTERCARD",
          },
        },
      };
    }

    if (path.startsWith("/users/v1/")) {
      return {
        data: {
          data: {
            id: path.replace("/users/v1/", ""),
            name: "Oscar",
            surname: "Odon",
            legal_address: {
              street_name: "Rua Amadeo Alvarez Gandara",
              street_number: "226",
              zip_code: "17516-636",
              neighborhood: "Marília (3529005)",
              city: "Marília",
              region: "SP",
              municipality: "3529005",
              country: "BRA",
              additional_info: "Casa G5",
            },
          },
        },
      };
    }

    throw new Error(`Unexpected GET path: ${path}`);
  }) as typeof pomeloHttp.get;

  pomeloHttp.post = (async <_T = unknown>(path: string, payload?: unknown) => {
    const activationMatch = path.match(
      /^\/tokenization\/v1\/tokens\/(.+)\/app-to-app-activation$/,
    );

    if (activationMatch) {
      return {
        data: {
          data: {
            external_token_id: activationMatch[1],
            activation_result: "APPROVED",
          },
          error: null,
        },
      };
    }

    return {
      data: {
        data: {
          provider: path.includes("/visa/") ? "VISA" : "MASTERCARD",
          payload,
        },
      },
    };
  }) as typeof pomeloHttp.post;
};

test.afterEach(() => {
  pomeloHttp.get = originalGet;
  pomeloHttp.post = originalPost;
});

test("GET /cards/:id forwards only id", async () => {
  mockPomeloHttp();
  const response = await app.request(
    "http://localhost/cards/crd-123?extend=pan",
  );

  assert.equal(response.status, 200);
  assert.deepEqual(await response.json(), {
    cardId: "crd-123",
    userId: "usr-123",
    lastFour: "1573",
    cardholderName: "Dieguito",
    brand: "MASTERCARD",
  });
});

test("GET /users/:id returns Pomelo user payload", async () => {
  mockPomeloHttp();
  const response = await app.request("http://localhost/users/usr-123");

  assert.equal(response.status, 200);
  assert.deepEqual(await response.json(), {
    id: "usr-123",
    name: "Oscar",
    surname: "Odon",
    legal_address: {
      street_name: "Rua Amadeo Alvarez Gandara",
      street_number: "226",
      zip_code: "17516-636",
      neighborhood: "Marília (3529005)",
      city: "Marília",
      region: "SP",
      municipality: "3529005",
      country: "BRA",
      additional_info: "Casa G5",
    },
  });
});

test("POST /push-provisioning/mastercard/google-pay validates required fields", async () => {
  mockPomeloHttp();
  const response = await app.request(
    "http://localhost/push-provisioning/mastercard/google-pay",
    {
      method: "POST",
      headers: {
        "content-type": "application/json",
      },
      body: JSON.stringify({
        card_id: "crd-123",
      }),
    },
  );

  assert.equal(response.status, 400);
  assert.equal(await response.text(), "✖ is required\n  → at user_id");
});

test("POST /push-provisioning/visa/google-pay forwards payload", async () => {
  mockPomeloHttp();
  const response = await app.request(
    "http://localhost/push-provisioning/visa/google-pay",
    {
      method: "POST",
      headers: {
        "content-type": "application/json",
      },
      body: JSON.stringify({
        card_id: "crd-123",
        user_id: "usr-123",
        device_id: "device-123",
        wallet_account_id: "wallet-123",
        server_session_id: "session-123",
      }),
    },
  );

  assert.equal(response.status, 200);
  assert.deepEqual(await response.json(), {
    provider: "VISA",
    payload: {
      card_id: "crd-123",
      user_id: "usr-123",
      device_id: "device-123",
      wallet_account_id: "wallet-123",
      server_session_id: "session-123",
    },
  });
});

test("POST /push-provisioning/visa/google-pay validates Google-provided ids", async () => {
  mockPomeloHttp();
  const response = await app.request(
    "http://localhost/push-provisioning/visa/google-pay",
    {
      method: "POST",
      headers: {
        "content-type": "application/json",
      },
      body: JSON.stringify({
        card_id: "crd-123",
        user_id: "usr-123",
        server_session_id: "session-123",
      }),
    },
  );

  assert.equal(response.status, 400);
  assert.equal(
    await response.text(),
    "✖ is required\n  → at device_id\n✖ is required\n  → at wallet_account_id",
  );
});

test("POST /tokens/:id/app-to-app-activation forwards external_token_id", async () => {
  mockPomeloHttp();
  const response = await app.request(
    "http://localhost/tokens/token-abc-123/app-to-app-activation",
    {
      method: "POST",
      headers: {
        "content-type": "application/json",
      },
      body: JSON.stringify({ device_id: "device-abc" }),
    },
  );

  assert.equal(response.status, 200);
  assert.deepEqual(await response.json(), {
    external_token_id: "token-abc-123",
    activation_result: "APPROVED",
  });
});

test("POST /tokens/:id/app-to-app-activation forwards device_id to Pomelo", async () => {
  let receivedPath: string | undefined;
  let receivedPayload: unknown;

  pomeloHttp.post = (async (path: string, payload?: unknown) => {
    receivedPath = path;
    receivedPayload = payload;

    return {
      data: {
        data: {
          external_token_id: "token-abc-123",
          activation_result: "APPROVED",
        },
      },
    };
  }) as typeof pomeloHttp.post;

  await app.request(
    "http://localhost/tokens/token-abc-123/app-to-app-activation",
    {
      method: "POST",
      headers: {
        "content-type": "application/json",
      },
      body: JSON.stringify({ device_id: "device-abc" }),
    },
  );

  assert.equal(
    receivedPath,
    "/tokenization/v1/tokens/token-abc-123/app-to-app-activation",
  );
  assert.deepEqual(receivedPayload, { device_id: "device-abc" });
});

test("POST /tokens/:id/app-to-app-activation accepts a missing device_id", async () => {
  mockPomeloHttp();
  const response = await app.request(
    "http://localhost/tokens/token-abc-123/app-to-app-activation",
    {
      method: "POST",
      headers: {
        "content-type": "application/json",
      },
      body: JSON.stringify({}),
    },
  );

  assert.equal(response.status, 200);
  assert.deepEqual(await response.json(), {
    external_token_id: "token-abc-123",
    activation_result: "APPROVED",
  });
});

test("route errors are normalized", async () => {
  pomeloHttp.get = (async () => {
    throw new HTTPException(404, {
      res: Response.json(
        {
          statusCode: 404,
          statusName: "Not Found",
        },
        { status: 404 },
      ),
    });
  }) as typeof pomeloHttp.get;

  const response = await app.request("http://localhost/users/usr-missing");

  assert.equal(response.status, 404);
  assert.deepEqual(await response.json(), {
    statusCode: 404,
    statusName: "Not Found",
  });
});

test("axios errors from Pomelo are forwarded as-is", async () => {
  pomeloHttp.get = (async () => {
    throw axios.AxiosError.from(
      new Error("Request failed with status code 404"),
      undefined,
      undefined,
      undefined,
      {
        status: 404,
        statusText: "Not Found",
        headers: {},
        config: { headers: {} as never },
        data: {
          error: {
            error_code: "NOT_FOUND",
            title: "Not Found",
            message: "Pomelo user not found",
          },
        },
      },
    );
  }) as typeof pomeloHttp.get;

  const response = await app.request("http://localhost/users/usr-missing");

  assert.equal(response.status, 404);
  assert.equal(await response.text(), "");
});
