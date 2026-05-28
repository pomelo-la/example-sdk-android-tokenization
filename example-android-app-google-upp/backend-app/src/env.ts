import { createEnv } from "@t3-oss/env-core";
import { z } from "zod";

export const env = createEnv({
	server: {
		PORT: z.coerce.number().int().positive().default(3000),
		POMELO_BASE_URL: z.string().url(),
		POMELO_AUDIENCE: z.string().url(),
		POMELO_CLIENT_ID: z.string().min(1),
		POMELO_CLIENT_SECRET: z.string().min(1),
	},
	runtimeEnv: process.env,
	emptyStringAsUndefined: true,
});
