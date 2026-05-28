import { Hono } from "hono";

import { pomeloHttp } from "../clients/pomelo-http.js";
import { requestValidator } from "../lib/request-validator.js";
import { resourceIdParamSchema } from "../lib/schemas.js";

const cards = new Hono();

type PomeloCardResponse = {
  data?: {
    id?: string;
    user_id?: string;
    last_four?: string;
    name_on_card?: string;
    provider?: string;
  };
};

cards.get(
  "/cards/:id",
  requestValidator("param", resourceIdParamSchema),
  async (c) => {
    const { id } = c.req.valid("param");
    const response = await pomeloHttp.get<PomeloCardResponse>(
      `/cards/v1/${encodeURIComponent(id)}`,
    );
    const card = response.data.data;

    return c.json({
      cardId: card?.id,
      userId: card?.user_id,
      lastFour: card?.last_four,
      cardholderName: card?.name_on_card,
      brand: card?.provider,
    });
  },
);

export default cards;
