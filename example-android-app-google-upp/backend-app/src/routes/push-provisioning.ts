import { Hono } from "hono";

import { pomeloHttp } from "../clients/pomelo-http.js";
import { requestValidator } from "../lib/request-validator.js";
import {
  pushProvisioningBodySchema,
  visaPushProvisioningBodySchema,
} from "../lib/schemas.js";

const pushProvisioning = new Hono();

pushProvisioning.post(
  "/push-provisioning/visa/google-pay",
  requestValidator("json", visaPushProvisioningBodySchema),
  async (c) => {
    const payload = c.req.valid("json");
    const response = await pomeloHttp.post(
      "/token-provisioning/v2/visa/google-pay",
      payload,
    );

    return c.json(response.data.data);
  },
);

pushProvisioning.post(
  "/push-provisioning/mastercard/google-pay",
  requestValidator("json", pushProvisioningBodySchema),
  async (c) => {
    const payload = c.req.valid("json");
    const response = await pomeloHttp.post(
      "/token-provisioning/v2/mastercard/google-pay",
      payload,
    );

    return c.json(response.data.data);
  },
);

export default pushProvisioning;
