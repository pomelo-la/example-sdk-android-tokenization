import type { InternalAxiosRequestConfig } from "axios";
import axios from "axios";
import { tokenStore } from "../auth/token-store.js";
import { env } from "../env.js";
import { HTTPException } from "hono/http-exception";

const getRequestMethod = (method?: string) => (method ?? "GET").toUpperCase();

const getRequestUrl = (url?: string) => {
  if (!url) return env.POMELO_BASE_URL;
  if (url.startsWith("http://") || url.startsWith("https://")) {
    return url;
  }

  return new URL(url, env.POMELO_BASE_URL).toString();
};

const stringifyPayload = (payload: unknown) => {
  if (payload === undefined) return "";

  try {
    return JSON.stringify(payload);
  } catch {
    return String(payload);
  }
};

export const pomeloHttp = axios.create({
  baseURL: env.POMELO_BASE_URL,
  headers: {
    "content-type": "application/json",
  },
});

pomeloHttp.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = tokenStore.getToken();
  const method = getRequestMethod(config.method);
  const url = getRequestUrl(config.url);

  if (!token) {
    throw new HTTPException(403, { message: "Pomelo token is not present" });
  }

  console.log(`<-- ${method} ${url} ${stringifyPayload(config.data)}`);

  config.headers.set("Authorization", `Bearer ${token}`);

  return config;
});

pomeloHttp.interceptors.response.use(
  (response) => {
    const method = getRequestMethod(response.config.method);
    const url = getRequestUrl(response.config.url);
    console.log(
      `--> ${method} ${url} ${response.status} ${stringifyPayload(response.data)}`,
    );
    return response;
  },
  (error: unknown) => {
    if (axios.isAxiosError(error)) {
      const method = getRequestMethod(error.config?.method);
      const url = getRequestUrl(error.config?.url);
      console.log(
        `--> ${method} ${url} ${error.response?.status ?? 500} ${stringifyPayload(error.response?.data)}`,
      );
    }

    return Promise.reject(error);
  },
);
