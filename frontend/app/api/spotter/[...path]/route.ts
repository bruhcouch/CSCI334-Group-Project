import { NextRequest } from "next/server";

const SPOTTER_SERVICE_URL = process.env.SPOTTER_SERVICE_URL || "http://localhost:8085/api/spotter";

export const dynamic = "force-dynamic";

async function forwardSpotterRequest(request: NextRequest) {
    const url = new URL(request.url);
    const path = url.pathname.replace(/^\/api\/spotter/, "");
    const target = `${SPOTTER_SERVICE_URL}${path}${url.search}`;
    const headers = new Headers();
    const contentType = request.headers.get("content-type");

    if (contentType) {
        headers.set("content-type", contentType);
    }

    const hasBody = !["GET", "HEAD"].includes(request.method);
    const response = await fetch(target, {
        method: request.method,
        headers,
        body: hasBody ? await request.text() : undefined,
        cache: "no-store",
    });

    const responseHeaders = new Headers();
    const responseContentType = response.headers.get("content-type");

    if (responseContentType) {
        responseHeaders.set("content-type", responseContentType);
    }

    return new Response(await response.text(), {
        status: response.status,
        statusText: response.statusText,
        headers: responseHeaders,
    });
}

export async function GET(request: NextRequest) {
    return forwardSpotterRequest(request);
}

export async function POST(request: NextRequest) {
    return forwardSpotterRequest(request);
}

export async function PATCH(request: NextRequest) {
    return forwardSpotterRequest(request);
}
