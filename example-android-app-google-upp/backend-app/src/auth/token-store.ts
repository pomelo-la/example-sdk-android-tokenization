type StoredToken = {
	accessToken: string;
	expiresIn: number;
	tokenType: string;
	obtainedAt: number;
};

export class TokenStore {
	#token?: StoredToken;

	setToken(input: {
		accessToken: string;
		expiresIn: number;
		tokenType: string;
	}) {
		this.#token = {
			accessToken: input.accessToken,
			expiresIn: input.expiresIn,
			tokenType: input.tokenType,
			obtainedAt: Date.now(),
		};
	}

	getToken() {
		return this.#token?.accessToken;
	}

	getSnapshot() {
		return this.#token;
	}

	clear() {
		this.#token = undefined;
	}
}

export const tokenStore = new TokenStore();
