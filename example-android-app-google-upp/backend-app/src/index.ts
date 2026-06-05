import { pathToFileURL } from "node:url";
import { serve } from "@hono/node-server";
import axios from "axios";
import { Hono } from "hono";
import { HTTPException } from "hono/http-exception";
import { logger } from "hono/logger";
import { bootstrapToken } from "./auth/bootstrap-token.js";
import { env } from "./env.js";

import cards from "./routes/cards.js";
import pushProvisioning from "./routes/push-provisioning.js";
import users from "./routes/users.js";

const app = new Hono();

app.use(logger());

app.onError((error, _) => {
  if (error instanceof HTTPException) return error.getResponse();

  if (axios.isAxiosError(error)) {
    return new HTTPException((error.status || 500) as never).getResponse();
  }

  return new HTTPException(500).getResponse();
});

app.route("/", cards);
app.route("/", users);
app.route("/", pushProvisioning);

const main = async () => {
  await bootstrapToken();

  serve(
    {
      fetch: app.fetch,
      port: env.PORT,
    },
    (info) => {
      console.log(`Server is running on http://localhost:${info.port}`);
    },
  );
};

if (
  process.argv[1] &&
  import.meta.url === pathToFileURL(process.argv[1]).href
) {
  void main().catch((error) => {
    console.error("Failed to start backend-app");
    console.error(error);
    process.exit(1);
  });
}

export default app;
