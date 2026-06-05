import axios from "axios";
import { env } from "../env.js";
import { tokenStore } from "./token-store.js";

type TokenResponse = {
  access_token: string;
  expires_in: number;
  token_type: string;
};

export const bootstrapToken = async () => {
  const baseUrl = env.POMELO_BASE_URL.replace(/\/$/, "");

  try {
    console.log("[bootstrapToken] Requesting Pomelo M2M token", {
      baseUrl,
      audience: env.POMELO_AUDIENCE,
      clientId: env.POMELO_CLIENT_ID,
    });

    const { data } = await axios.post<TokenResponse>(
      `${baseUrl}/oauth/token`,
      {
        client_id: env.POMELO_CLIENT_ID,
        client_secret: env.POMELO_CLIENT_SECRET,
        audience: env.POMELO_AUDIENCE,
        grant_type: "client_credentials",
      },
      {
        headers: {
          "content-type": "application/json",
        },
      },
    );

    const { access_token, expires_in, token_type } = data;

    tokenStore.setToken({
      accessToken: access_token,
      expiresIn: expires_in,
      tokenType: token_type,
    });

    console.log("[bootstrapToken] Pomelo M2M token stored in memory", {
      tokenType: token_type,
      expiresIn: expires_in,
    });
  } catch (error) {
    console.error("[bootstrapToken] Failed to obtain Pomelo M2M token", {
      message: error instanceof Error ? error.message : String(error),
    });

    throw error;
  }
};
