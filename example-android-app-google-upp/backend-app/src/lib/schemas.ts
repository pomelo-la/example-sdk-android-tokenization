import { z } from "zod";

const REQUIRED_STRING_MESSAGE = "is required";

const nonEmptyString = () =>
	z
		.string({ error: REQUIRED_STRING_MESSAGE })
		.min(1, REQUIRED_STRING_MESSAGE);

export const resourceIdParamSchema = z.object({
	id: nonEmptyString(),
});

export const pushProvisioningBodySchema = z.looseObject({
	card_id: nonEmptyString(),
	user_id: nonEmptyString(),
	server_session_id: z
		.string()
		.min(1, "server_session_id cannot be empty")
		.optional(),
});

export const visaPushProvisioningBodySchema = pushProvisioningBodySchema.extend(
	{
		device_id: nonEmptyString(),
		wallet_account_id: nonEmptyString(),
		server_session_id: nonEmptyString(),
	},
);

export type PushProvisioningRequest = z.infer<
	typeof pushProvisioningBodySchema
>;

export const activateTokenBodySchema = z.object({
	motive: z.enum(["APP_TO_APP_ACTIVATION"]),
});

export type ActivateTokenRequest = z.infer<typeof activateTokenBodySchema>;
