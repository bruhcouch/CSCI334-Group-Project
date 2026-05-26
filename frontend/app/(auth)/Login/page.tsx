"use client";

import { useState } from "react";
import { fetcher } from "../../../lib/api";

type LoginRequest = {
    email: string;
    password: string;
};

type Role = "USER" | "STAFF" | "ADMIN";

type AuthResponse = {
    id: number;
    username: string;
    email: string;
    role: Role;
};

type LoginMode = "user" | "admin";

const defaultAdminEmail = "admin@uowmail.edu.au";
const defaultAdminPassword = "test123";

function clearJwtCookie() {
    document.cookie = "jwt=; Max-Age=0; path=/; SameSite=Lax";
}

export default function LoginPage() {
    const [loginMode, setLoginMode] = useState<LoginMode>("user");
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    function switchMode(mode: LoginMode) {
        setLoginMode(mode);
        setError(null);

        if (mode === "admin" && !email && !password) {
            setEmail(defaultAdminEmail);
            setPassword(defaultAdminPassword);
        }

        if (mode === "user" && email === defaultAdminEmail && password === defaultAdminPassword) {
            setEmail("");
            setPassword("");
        }
    }

    async function handleLogin(e: React.FormEvent) {
        e.preventDefault();
        setLoading(true);
        setError(null);

        try {
            const data = await fetcher<AuthResponse>("/api/accounts/login", {
                method: "POST",
                body: JSON.stringify({
                    email,
                    password,
                } satisfies LoginRequest),
                credentials: "include",
            });

            const staffLogin = data.role === "STAFF" || data.role === "ADMIN";

            if (loginMode === "admin" && !staffLogin) {
                clearJwtCookie();
                setError("Use the user login for regular accounts.");
                return;
            }

            if (loginMode === "user" && staffLogin) {
                clearJwtCookie();
                setError("Use the admin login for staff and admin accounts.");
                return;
            }

            if (staffLogin) {
                window.location.href = "/staff";
            } else {
                window.location.href = "/user";
            }

        } catch (err: unknown) {
            if (err instanceof Error && err.name === "403" && loginMode === "admin") {
                setError("Admin account is waiting for approval from another admin.");
            } else {
                setError(err instanceof Error ? err.message : "Login failed");
            }
        } finally {
            setLoading(false);
        }
    }

    return (
        <div className="flex items-center justify-center min-h-screen bg-gray-50">
            <div className="w-full max-w-md rounded-lg border bg-white p-8 shadow-lg">
                <h2 className="text-2xl font-semibold mb-6 text-center text-black">
                    {loginMode === "admin" ? "Admin Login" : "User Login"}
                </h2>

                <div className="mb-6 grid grid-cols-2 rounded-md border border-gray-200 bg-gray-100 p-1">
                    <button
                        type="button"
                        onClick={() => switchMode("user")}
                        className={`rounded px-3 py-2 text-sm font-semibold transition ${loginMode === "user" ? "bg-white text-black shadow-sm" : "text-gray-600 hover:text-black"}`}
                    >
                        User
                    </button>
                    <button
                        type="button"
                        onClick={() => switchMode("admin")}
                        className={`rounded px-3 py-2 text-sm font-semibold transition ${loginMode === "admin" ? "bg-white text-black shadow-sm" : "text-gray-600 hover:text-black"}`}
                    >
                        Admin
                    </button>
                </div>

                <form onSubmit={handleLogin} className="space-y-4">
                    <div>
                        <label className="text-sm font-medium text-black">Email</label>
                        <input
                            type="email"
                            className="mt-1 w-full rounded-md border px-3 py-2 text-black focus:outline-none focus:ring"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            required
                        />
                    </div>

                    <div>
                        <label className="text-sm font-medium text-black">Password</label>
                        <input
                            type="password"
                            className="mt-1 w-full rounded-md border px-3 py-2 text-black focus:outline-none focus:ring"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            required
                        />
                    </div>

                    {error && (
                        <p className="text-red-500 text-sm">{error}</p>
                    )}

                    <button
                        type="submit"
                        disabled={loading}
                        className="w-full rounded-md bg-black py-2 text-white transition hover:bg-gray-800 disabled:opacity-50"
                    >
                        {loading ? "Logging in..." : loginMode === "admin" ? "Login as admin" : "Login as user"}
                    </button>

                    <div className="text-center text-sm text-gray-600">
                        Don&apos;t have an account?{" "}
                        <a href="/register" className="text-blue-500 hover:underline">
                            Register here
                        </a>
                    </div>
                </form>
            </div>
        </div>
    );
}
