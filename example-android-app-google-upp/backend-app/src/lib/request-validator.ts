import { zValidator } from "@hono/zod-validator";
import type { ValidationTargets } from "hono";
import { HTTPException } from "hono/http-exception";
import type { ZodSchema } from "zod";
import { prettifyError } from "zod";

export const requestValidator = <
  TSchema extends ZodSchema,
  TTarget extends keyof ValidationTargets,
>(
  target: TTarget,
  schema: TSchema,
) =>
  zValidator(target, schema, (result) => {
    if (!result.success) {
      throw new HTTPException(400, {
        message: prettifyError(result.error),
      });
    }
  });
