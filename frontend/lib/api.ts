const API_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8089";
const SPOTTER_API_URL = "/api/spotter";

async function requestJson<T>(
    baseUrl: string,
    endpoint: string,
    options?: RequestInit,
    credentials: RequestCredentials = "include"
): Promise<T> {
    const headers = new Headers(options?.headers);

    if (!headers.has("Content-Type") && options?.body) {
        headers.set("Content-Type", "application/json");
    }

    const response = await fetch(`${baseUrl}${endpoint}`, {
        ...options,
        headers,
        credentials,
    });

    if (!response.ok) {
        const errorBody = await response.text().catch(() => "");
        const error = new Error(`Request failed: ${response.status} ${response.statusText}${errorBody ? ` - ${errorBody}` : ""}`);
        error.name = String(response.status);
        throw error;
    }

    if (response.status === 204) {
        return undefined as T;
    }

    const body = await response.text();
    return (body ? JSON.parse(body) : undefined) as T;
}

export async function fetcher<T>(endpoint: string, options?: RequestInit): Promise<T> {
    return requestJson<T>(API_URL, endpoint, options, "include");
}

export async function spotterFetcher<T>(endpoint: string, options?: RequestInit): Promise<T> {
    return requestJson<T>(SPOTTER_API_URL, endpoint, options, "omit");
}
