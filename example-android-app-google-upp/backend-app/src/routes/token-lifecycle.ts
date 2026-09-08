import { Hono } from "hono";

import { pomeloHttp } from "../clients/pomelo-http.js";
import { requestValidator } from "../lib/request-validator.js";
import { activateTokenBodySchema, resourceIdParamSchema } from "../lib/schemas.js";

const tokenLifecycle = new Hono();

tokenLifecycle.post(
	"/tokens/:id/activate",
	requestValidator("param", resourceIdParamSchema),
	requestValidator("json", activateTokenBodySchema),
	async (c) => {
		const { id } = c.req.valid("param");
		const payload = c.req.valid("json");

		const response = await pomeloHttp.post(
			`/tokenization/v1/tokens/${encodeURIComponent(id)}/activate`,
			payload,
		);

		return c.json(response.data, 202);
	},
);

export default tokenLifecycle;
