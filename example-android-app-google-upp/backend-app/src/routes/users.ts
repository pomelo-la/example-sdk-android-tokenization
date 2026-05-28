import { Hono } from "hono";

import { pomeloHttp } from "../clients/pomelo-http.js";
import { requestValidator } from "../lib/request-validator.js";
import { resourceIdParamSchema } from "../lib/schemas.js";

const users = new Hono();

type PomeloUserResponse = {
  data: {
    id?: string;
    name?: string;
    surname?: string;
    legal_address?: {
      street_name?: string;
      street_number?: string;
      zip_code?: string;
      city?: string;
      region?: string;
      country?: string;
    };
  };
};

users.get(
  "/users/:id",
  requestValidator("param", resourceIdParamSchema),
  async (c) => {
    const { id } = c.req.valid("param");
    const response = await pomeloHttp.get<PomeloUserResponse>(
      `/users/v1/${encodeURIComponent(id)}`,
    );
    const user = response.data.data;

    return c.json(user);
  },
);

export default users;
