import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";

export function proxy(req: NextRequest) {
    const jwt = req.cookies.get("jwt")?.value;
    const path = req.nextUrl.pathname;

    if (path === "/") {
        return NextResponse.redirect(new URL(jwt ? "/user" : "/login", req.url));
    }

    if ((path.startsWith("/staff") || path.startsWith("/user")) && !jwt) {
        return NextResponse.redirect(new URL("/login", req.url));
    }

    return NextResponse.next();
}

export const config = {
    matcher: ["/", "/user/:path*", "/staff/:path*"],
};
