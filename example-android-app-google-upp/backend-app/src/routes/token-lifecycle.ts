import { Hono } from "hono";

import { pomeloHttp } from "../clients/pomelo-http.js";
import { requestValidator } from "../lib/request-validator.js";
import {
  appToAppActivationBodySchema,
  resourceIdParamSchema,
} from "../lib/schemas.js";

const tokenLifecycle = new Hono();

/**
 * App2App (IDV / step-up) activation: Google Wallet launches the issuer app via an
 * explicit Intent when Visa needs cardholder verification to activate a token. Once
 * the app authenticates the cardholder, it calls this endpoint, which forwards the
 * device_id from the Visa payload (EXTRA_TEXT) so Pomelo can activate the token
 * against Visa's Token Lifecycle API.
 */
tokenLifecycle.post(
  "/tokens/:id/app-to-app-activation",
  requestValidator("param", resourceIdParamSchema),
  requestValidator("json", appToAppActivationBodySchema),
  async (c) => {
    const { id } = c.req.valid("param");
    const { device_id } = c.req.valid("json");
    const response = await pomeloHttp.post(
      `/tokenization/v1/tokens/${encodeURIComponent(id)}/app-to-app-activation`,
      { device_id: device_id ?? null },
    );

    return c.json(response.data.data);
  },
);

export default tokenLifecycle;
