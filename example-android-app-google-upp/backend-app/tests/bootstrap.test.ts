import assert from "node:assert/strict";
import test from "node:test";
import axios from "axios";

const originalEnv = { ...process.env };
const originalAxiosPost = axios.post;

test.afterEach(() => {
	process.env = { ...originalEnv };
	axios.post = originalAxiosPost;
});

test("env reads and validates environment variables", async () => {
	process.env.POMELO_BASE_URL = "https://api.pomelo.la";
	process.env.POMELO_AUDIENCE = "https://auth-dev.pomelo.la";
	process.env.POMELO_CLIENT_ID = "client-id";
	process.env.POMELO_CLIENT_SECRET = "client-secret";
	process.env.PORT = "4000";

	const { env } = await import(`../src/env.js?test=${Date.now()}`);

	assert.equal(env.POMELO_BASE_URL, "https://api.pomelo.la");
	assert.equal(env.POMELO_AUDIENCE, "https://auth-dev.pomelo.la");
	assert.equal(env.POMELO_CLIENT_ID, "client-id");
	assert.equal(env.POMELO_CLIENT_SECRET, "client-secret");
	assert.equal(env.PORT, 4000);
});

test("bootstrapToken stores token when auth succeeds", async () => {
	const calls: Array<{ url: string; body: unknown; config?: unknown }> = [];
	const nonce = Date.now();

	process.env.POMELO_BASE_URL = "https://api.pomelo.la";
	process.env.POMELO_AUDIENCE = "https://auth-dev.pomelo.la";
	process.env.POMELO_CLIENT_ID = "client-id";
	process.env.POMELO_CLIENT_SECRET = "client-secret";

	axios.post = (async <TResponse>(
		url: string,
		body: unknown,
		config?: unknown,
	) => {
		calls.push({ url, body, config });

		return {
			data: {
				access_token: "token-123",
				expires_in: 86400,
				token_type: "Bearer",
			},
		} as { data: TResponse };
	}) as typeof axios.post;

	const [{ bootstrapToken }, { tokenStore }] = await Promise.all([
		import(`../src/auth/bootstrap-token.js?test=${nonce}`),
		import("../src/auth/token-store.js"),
	]);

	await bootstrapToken();

	assert.equal(calls.length, 1);
	assert.equal(calls[0]?.url, "https://api.pomelo.la/oauth/token");
	assert.equal(tokenStore.getToken(), "token-123");
});

test("bootstrapToken throws unauthorized errors", async () => {
	const nonce = Date.now();

	process.env.POMELO_BASE_URL = "https://api.pomelo.la";
	process.env.POMELO_AUDIENCE = "https://auth-dev.pomelo.la";
	process.env.POMELO_CLIENT_ID = "client-id";
	process.env.POMELO_CLIENT_SECRET = "client-secret";

	axios.post = (async () => {
		throw axios.AxiosError.from(
			new Error("Request failed with status code 401"),
			undefined,
			undefined,
			undefined,
			{
				status: 401,
				statusText: "Unauthorized",
				headers: {},
				config: { headers: {} as never },
				data: {
					error: {
						error_code: "UNAUTHORIZED",
						title: "Unauthorized",
						message: "invalid credentials",
					},
				},
			},
		);
	}) as typeof axios.post;

	const [{ bootstrapToken }] = await Promise.all([
		import(`../src/auth/bootstrap-token.js?test=${nonce}`),
	]);

	await assert.rejects(() => bootstrapToken(), (error: unknown) => {
		assert.ok(axios.isAxiosError(error));
		assert.equal(error.response?.status, 401);
		assert.deepEqual(error.response?.data, {
			error: {
				error_code: "UNAUTHORIZED",
				title: "Unauthorized",
				message: "invalid credentials",
			},
		});
		return true;
	});
});

test("bootstrapToken wraps network failures", async () => {
	const nonce = Date.now();

	process.env.POMELO_BASE_URL = "https://api.pomelo.la";
	process.env.POMELO_AUDIENCE = "https://auth-dev.pomelo.la";
	process.env.POMELO_CLIENT_ID = "client-id";
	process.env.POMELO_CLIENT_SECRET = "client-secret";

	axios.post = (async () => {
		throw axios.AxiosError.from(new Error("socket hang up"));
	}) as typeof axios.post;

	const [{ bootstrapToken }] = await Promise.all([
		import(`../src/auth/bootstrap-token.js?test=${nonce}`),
	]);

	await assert.rejects(() => bootstrapToken(), (error: unknown) => {
		assert.ok(axios.isAxiosError(error));
		assert.equal(error.message, "socket hang up");
		return true;
	});
});
