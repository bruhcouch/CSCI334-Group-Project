"use client";

import { useState } from "react";
import { fetcher } from "../../../lib/api";

type RegisterRequest = {
    email: string;
    password: string;
    username: string;
};

type RegisterResponse = {
    id: number;
    username: string;
    email: string;
    createdAt: string;
    message: string;
};

type RegisterMode = "user" | "staff";

export default function RegisterPage() {
    const [registerMode, setRegisterMode] = useState<RegisterMode>("user");
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [username, setUsername] = useState("");
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [success, setSuccess] = useState<string | null>(null);

    async function handleRegister(e: React.FormEvent) {
        e.preventDefault();
        setLoading(true);
        setError(null);
        setSuccess(null);

        try {
            const data = await fetcher<RegisterResponse>(registerMode === "staff" ? "/api/accounts/register/staff" : "/api/accounts/register", {
                method: "POST",
                body: JSON.stringify({
                    email,
                    password,
                    username
                } satisfies RegisterRequest),
                credentials: "include",
            });

            if (registerMode === "staff") {
                setSuccess(data.message || "Staff account submitted for approval.");
                setEmail("");
                setPassword("");
                setUsername("");
            } else {
                window.location.href = "/user";
            }
        } catch (err: unknown) {
            setError(err instanceof Error ? err.message : "Registration failed");
        } finally {
            setLoading(false);
        }
    }

    return (
        <div className="flex items-center justify-center min-h-screen bg-gray-50">
            <div className="w-full max-w-md rounded-lg border bg-white p-8 shadow-lg">
                <h2 className="text-2xl font-semibold mb-6 text-center text-black">
                    {registerMode === "staff" ? "Staff Registration" : "User Registration"}
                </h2>

                <div className="mb-6 grid grid-cols-2 rounded-md border border-gray-200 bg-gray-100 p-1">
                    <button
                        type="button"
                        onClick={() => {
                            setRegisterMode("user");
                            setError(null);
                            setSuccess(null);
                        }}
                        className={`rounded px-3 py-2 text-sm font-semibold transition ${registerMode === "user" ? "bg-white text-black shadow-sm" : "text-gray-600 hover:text-black"}`}
                    >
                        User
                    </button>
                    <button
                        type="button"
                        onClick={() => {
                            setRegisterMode("staff");
                            setError(null);
                            setSuccess(null);
                        }}
                        className={`rounded px-3 py-2 text-sm font-semibold transition ${registerMode === "staff" ? "bg-white text-black shadow-sm" : "text-gray-600 hover:text-black"}`}
                    >
                        Staff
                    </button>
                </div>

                <form onSubmit={handleRegister} className="space-y-4">
                    <div>
                        <label className="text-sm font-medium text-black">Username</label>
                        <input
                            type="text"
                            className="mt-1 w-full rounded-md border px-3 py-2 text-black focus:outline-none focus:ring"
                            value={username}
                            onChange={(e) => setUsername(e.target.value)}
                            required
                        />
                    </div>

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

                    {success && (
                        <p className="rounded-md border border-emerald-200 bg-emerald-50 px-3 py-2 text-sm text-emerald-800">{success}</p>
                    )}

                    <button
                        type="submit"
                        disabled={loading}
                        className="w-full rounded-md bg-black py-2 text-white transition hover:bg-gray-800 disabled:opacity-50"
                    >
                        {loading ? "Registering..." : registerMode === "staff" ? "Register as staff" : "Register as user"}
                    </button>

                    <div className="text-center text-sm text-gray-600">
                        Already have an account?{" "}
                        <a href="/login" className="text-blue-500 hover:underline">
                            Login here
                        </a>
                    </div>
                </form>
            </div>
        </div>
    );
}
