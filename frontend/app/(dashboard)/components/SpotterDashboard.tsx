"use client";

import { type FormEvent, useCallback, useEffect, useMemo, useState } from "react";
import { fetcher, spotterFetcher } from "../../../lib/api";

type DashboardMode = "user" | "staff";
type Role = "USER" | "STAFF" | "ADMIN";
type Subscription = "FREE" | "PREMIUM";

type Account = {
    id: number;
    email: string;
    username: string;
    role: Role;
    enabled: boolean;
    createdAt: string;
    subscription?: Subscription | null;
};

type ZoneSummary = {
    lotName: string;
    zone: string;
    totalSpaces: number;
    occupiedSpaces: number;
    availableSpaces: number;
    occupancyRate: number;
};

type SpotterSummary = {
    totalSpaces: number;
    occupiedSpaces: number;
    availableSpaces: number;
    disabilityPermitSpaces: number;
    availableDisabilityPermitSpaces: number;
    occupancyRate: number;
    zones: ZoneSummary[];
};

type Space = {
    id: number;
    lotName: string;
    zone: string;
    bayNumber: string;
    displayName: string;
    sensorId: string;
    maxParkingMinutes: number;
    disabilityPermitRequired: boolean;
    occupied: boolean;
    confidence: number;
    statusSource: string;
    lastUpdated: string;
    latitude?: number | null;
    longitude?: number | null;
};

type DetectionEvent = {
    id: number;
    spaceId: number;
    sensorId: string;
    lotName: string;
    zone: string;
    bayNumber: string;
    previousOccupied: boolean;
    occupied: boolean;
    confidence: number;
    source: string;
    detectedAt: string;
};

type SimulationEvent = {
    sequenceNumber: number;
    space: Space;
    event: DetectionEvent;
};

type SimulationRunResponse = {
    appliedEvents: number;
    feedSize: number;
    nextFeedIndex: number;
    events: SimulationEvent[];
    summary: SpotterSummary;
};

type AccountStats = {
    total: number;
    users: number;
    staff: number;
    disabled: number;
    premium: number;
};

type ParkingBooking = {
    id: number;
    accountId: number;
    parkingLot: string;
    parkingSpace: string;
    vehicle: string;
    mobilityParkingPermitNumber?: string | null;
    startTime: string;
    endTime: string;
    cost: number;
    status: "RESERVED" | "ACTIVE" | "EXPIRED" | "CANCELLED" | "COMPLETED";
    createdAt: string;
};

type OccupancyPrediction = {
    lotId: string;
    predictedOccupancyRate: number;
    availabilityProbability: number;
    estimatedAvailableSpaces: number;
    totalSpaces: number;
    targetTime: string;
};

type AdminSummary = {
    date: string;
    occupancySnapshot?: {
        hour: number;
        spotsTaken: number;
        spotsTotal: number;
    };
    peakHourSnapshot?: {
        hour: number;
        occupancyRate: number;
    };
    utilisationSnapshot?: {
        utilisationRate: number;
    };
};

function toQuery(params: URLSearchParams) {
    const query = params.toString();
    return query ? `?${query}` : "";
}

function formatTime(value?: string | Date | null) {
    if (!value) {
        return "Waiting";
    }

    const date = value instanceof Date ? value : new Date(value);

    if (Number.isNaN(date.getTime())) {
        return "Waiting";
    }

    return new Intl.DateTimeFormat("en-AU", {
        hour: "2-digit",
        minute: "2-digit",
        second: "2-digit",
    }).format(date);
}

function formatDateTime(value?: string | null) {
    if (!value) {
        return "Not recorded";
    }

    const date = new Date(value);

    if (Number.isNaN(date.getTime())) {
        return "Not recorded";
    }

    return new Intl.DateTimeFormat("en-AU", {
        day: "2-digit",
        month: "short",
        hour: "2-digit",
        minute: "2-digit",
    }).format(date);
}

function toDateTimeInput(date: Date) {
    const pad = (value: number) => String(value).padStart(2, "0");
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

function defaultBookingStart() {
    const start = new Date(Date.now() + 15 * 60 * 1000);
    start.setSeconds(0, 0);
    return toDateTimeInput(start);
}

function defaultBookingEnd() {
    const end = new Date(Date.now() + 75 * 60 * 1000);
    end.setSeconds(0, 0);
    return toDateTimeInput(end);
}

function percent(value: number) {
    return `${Math.round(value)}%`;
}

function probability(value: number) {
    return `${Math.round(value * 100)}%`;
}

function metricValue(value: number | undefined) {
    return typeof value === "number" ? value.toLocaleString("en-AU") : "...";
}

function eventChangeText(event: DetectionEvent) {
    if (event.previousOccupied === event.occupied) {
        return event.occupied ? "Stayed occupied" : "Stayed free";
    }

    return event.occupied ? "Free to occupied" : "Occupied to free";
}

function accountStats(accounts: Account[]): AccountStats {
    return {
        total: accounts.length,
        users: accounts.filter((account) => account.role === "USER").length,
        staff: accounts.filter((account) => account.role === "STAFF" || account.role === "ADMIN").length,
        disabled: accounts.filter((account) => !account.enabled).length,
        premium: accounts.filter((account) => account.subscription === "PREMIUM").length,
    };
}

function distanceMeters(space: Space) {
    if (typeof space.latitude !== "number" || typeof space.longitude !== "number") {
        return Number.POSITIVE_INFINITY;
    }

    const campusLat = -34.4068;
    const campusLng = 150.8793;
    const earthRadius = 6371000;
    const toRadians = (value: number) => value * Math.PI / 180;
    const dLat = toRadians(space.latitude - campusLat);
    const dLng = toRadians(space.longitude - campusLng);
    const lat1 = toRadians(campusLat);
    const lat2 = toRadians(space.latitude);
    const a = Math.sin(dLat / 2) ** 2 + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLng / 2) ** 2;
    return earthRadius * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

function formatDistance(value: number) {
    if (!Number.isFinite(value)) {
        return "Campus distance unavailable";
    }

    return value < 1000 ? `${Math.round(value)}m from campus centre` : `${(value / 1000).toFixed(1)}km from campus centre`;
}

function bayTypeLabel(value: string) {
    return value === "Accessible" ? "Accessible" : "General";
}

function zoneName(value: string) {
    return value === "Accessible" ? "Accessible bays" : "General bays";
}

function bookingBayLabel(booking: ParkingBooking) {
    const parts = booking.parkingSpace.split("-");
    if (parts.length >= 4) {
        const type = parts[2] === "ACC" ? "Accessible" : "Bay";
        return `${booking.parkingLot} ${type} ${parts[3]}`;
    }

    return booking.parkingLot;
}

function clearJwtCookie() {
    document.cookie = "jwt=; Max-Age=0; path=/; SameSite=Lax";
}

function logout() {
    clearJwtCookie();
    window.location.href = "/login";
}

export default function SpotterDashboard({ mode }: { mode: DashboardMode }) {
    const isStaff = mode === "staff";
    const [summary, setSummary] = useState<SpotterSummary | null>(null);
    const [lots, setLots] = useState<string[]>([]);
    const [zoneOptions, setZoneOptions] = useState<ZoneSummary[]>([]);
    const [spaces, setSpaces] = useState<Space[]>([]);
    const [events, setEvents] = useState<DetectionEvent[]>([]);
    const [selectedLot, setSelectedLot] = useState("all");
    const [selectedZone, setSelectedZone] = useState("all");
    const [accessibleEligible, setAccessibleEligible] = useState(false);
    const [lastSync, setLastSync] = useState<Date | null>(null);
    const [spotterError, setSpotterError] = useState<string | null>(null);
    const [account, setAccount] = useState<Account | null>(null);
    const [accounts, setAccounts] = useState<Account[]>([]);
    const [accountError, setAccountError] = useState<string | null>(null);
    const [actionStatus, setActionStatus] = useState<string | null>(null);
    const [actionEvents, setActionEvents] = useState<DetectionEvent[]>([]);
    const [actionBusy, setActionBusy] = useState<string | null>(null);
    const [approvalBusyId, setApprovalBusyId] = useState<number | null>(null);
    const [approvalBusyAction, setApprovalBusyAction] = useState<"approve" | "reject" | null>(null);
    const [approvalStatus, setApprovalStatus] = useState<string | null>(null);
    const [selectedSensor, setSelectedSensor] = useState("");
    const [manualOccupied, setManualOccupied] = useState("true");
    const [bookings, setBookings] = useState<ParkingBooking[]>([]);
    const [bookingSensor, setBookingSensor] = useState("");
    const [bookingRegistration, setBookingRegistration] = useState("");
    const [bookingPermitNumber, setBookingPermitNumber] = useState("");
    const [bookingStart, setBookingStart] = useState(defaultBookingStart);
    const [bookingEnd, setBookingEnd] = useState(defaultBookingEnd);
    const [bookingBusy, setBookingBusy] = useState<string | null>(null);
    const [bookingStatus, setBookingStatus] = useState<string | null>(null);
    const [bookingError, setBookingError] = useState<string | null>(null);
    const [predictions, setPredictions] = useState<OccupancyPrediction[]>([]);
    const [predictionError, setPredictionError] = useState<string | null>(null);
    const [adminSummary, setAdminSummary] = useState<AdminSummary | null>(null);

    const loadSpotter = useCallback(async () => {
        try {
            const summaryParams = new URLSearchParams();
            const zoneParams = new URLSearchParams();

            if (selectedLot !== "all") {
                summaryParams.set("lotName", selectedLot);
                zoneParams.set("lotName", selectedLot);
            }

            if (selectedZone !== "all") {
                summaryParams.set("zone", selectedZone);
            }

            const spaceParams = new URLSearchParams(summaryParams);

            if (!isStaff) {
                spaceParams.set("occupied", "false");
            }

            const [nextSummary, nextLots, nextZones, nextSpaces, nextEvents] = await Promise.all([
                spotterFetcher<SpotterSummary>(`/summary${toQuery(summaryParams)}`),
                spotterFetcher<string[]>("/lots"),
                spotterFetcher<ZoneSummary[]>(`/zones${toQuery(zoneParams)}`),
                spotterFetcher<Space[]>(`/spaces${toQuery(spaceParams)}`),
                spotterFetcher<DetectionEvent[]>("/events"),
            ]);

            setSummary(nextSummary);
            setLots(nextLots);
            setZoneOptions(nextZones);
            setSpaces(nextSpaces);
            setEvents(nextEvents);
            setLastSync(new Date());
            setSpotterError(null);
        } catch (error) {
            setSpotterError(error instanceof Error ? error.message : "Parking availability is unavailable");
        }
    }, [isStaff, selectedLot, selectedZone]);

    useEffect(() => {
        const timeout = window.setTimeout(() => void loadSpotter(), 0);
        const interval = window.setInterval(() => void loadSpotter(), 2000);

        return () => {
            window.clearTimeout(timeout);
            window.clearInterval(interval);
        };
    }, [loadSpotter]);

    useEffect(() => {
        let cancelled = false;

        async function loadAccountData() {
            try {
                const profile = await fetcher<Account>("/api/accounts");
                if (!cancelled) {
                    setAccount(profile);
                    setAccountError(null);
                }
            } catch (error) {
                if (error instanceof Error && (error.name === "401" || error.name === "403")) {
                    clearJwtCookie();
                    window.location.href = "/login";
                    return;
                }

                if (!cancelled) {
                    setAccountError(error instanceof Error ? error.message : "Account service is unavailable");
                }
            }

            if (isStaff) {
                try {
                    const staffAccounts = await fetcher<Account[]>("/api/staff/accounts");
                    if (!cancelled) {
                        setAccounts(staffAccounts);
                    }
                } catch {
                    if (!cancelled) {
                        setAccounts([]);
                    }
                }
            }
        }

        void loadAccountData();

        return () => {
            cancelled = true;
        };
    }, [isStaff]);

    const loadProjectData = useCallback(async () => {
        if (!account) {
            return;
        }

        try {
            const bookingEndpoint = isStaff ? "/api/parking" : `/api/parking?accountId=${account.id}`;
            const nextBookings = await fetcher<ParkingBooking[]>(bookingEndpoint);
            setBookings(nextBookings);
            setBookingError(null);
        } catch (error) {
            setBookingError(error instanceof Error ? error.message : "Booking service is unavailable");
        }

        try {
            const nextPredictions = await fetcher<OccupancyPrediction[]>("/api/occupancy/predictions");
            setPredictions(nextPredictions);
            setPredictionError(null);
        } catch (error) {
            setPredictions([]);
            setPredictionError(error instanceof Error ? error.message : "Parking forecast is unavailable");
        }

        if (isStaff) {
            try {
                setAdminSummary(await fetcher<AdminSummary>("/api/adminstats/latest"));
            } catch {
                setAdminSummary(null);
            }
        }
    }, [account, isStaff]);

    useEffect(() => {
        const timeout = window.setTimeout(() => void loadProjectData(), 0);
        const interval = window.setInterval(() => void loadProjectData(), 5000);

        return () => {
            window.clearTimeout(timeout);
            window.clearInterval(interval);
        };
    }, [loadProjectData]);

    const zoneNames = useMemo(() => {
        const names = Array.from(new Set(zoneOptions.map((zone) => zone.zone))).sort();
        return isStaff || accessibleEligible ? names : names.filter((zone) => zone !== "Accessible");
    }, [accessibleEligible, isStaff, zoneOptions]);

    const visibleZones = useMemo(() => {
        const zones = summary?.zones ?? [];
        return isStaff || accessibleEligible ? zones : zones.filter((zone) => zone.zone !== "Accessible");
    }, [accessibleEligible, isStaff, summary]);
    const bookableSpaces = useMemo(() => (
        isStaff || accessibleEligible ? spaces : spaces.filter((space) => !space.disabilityPermitRequired)
    ), [accessibleEligible, isStaff, spaces]);
    const visibleSpaces = (isStaff ? spaces : bookableSpaces).slice(0, isStaff ? 14 : 12);
    const recentEvents = events.slice(0, 8);
    const staffStats = useMemo(() => accountStats(accounts), [accounts]);
    const pendingApprovals = useMemo(() => {
        return accounts.filter((staffAccount) => (
            (staffAccount.role === "ADMIN" || staffAccount.role === "STAFF")
            && !staffAccount.enabled
            && staffAccount.id !== account?.id
        ));
    }, [account?.id, accounts]);
    const bestZone = useMemo(() => {
        return [...visibleZones]
            .filter((zone) => zone.availableSpaces > 0)
            .sort((first, second) => first.occupancyRate - second.occupancyRate || second.availableSpaces - first.availableSpaces)[0];
    }, [visibleZones]);
    const bestPrediction = useMemo(() => predictions[0], [predictions]);
    const nearestSpace = useMemo(() => {
        return [...bookableSpaces]
            .filter((space) => !space.occupied)
            .sort((first, second) => distanceMeters(first) - distanceMeters(second))[0];
    }, [bookableSpaces]);
    const activeBookings = useMemo(() => {
        return bookings.filter((booking) => booking.status === "ACTIVE" || booking.status === "RESERVED");
    }, [bookings]);
    const selectedBookingSpace = useMemo(() => {
        const sensorId = bookingSensor || bookableSpaces.find((space) => !space.occupied)?.sensorId || "";
        return bookableSpaces.find((space) => space.sensorId === sensorId);
    }, [bookableSpaces, bookingSensor]);

    async function approveAccount(accountToApprove: Account) {
        setApprovalBusyId(accountToApprove.id);
        setApprovalBusyAction("approve");
        setApprovalStatus(null);

        try {
            await fetcher<void>(`/api/admin/accounts/${accountToApprove.id}/enable`, {
                method: "PATCH",
            });
            setAccounts((currentAccounts) => currentAccounts.map((currentAccount) => (
                currentAccount.id === accountToApprove.id ? { ...currentAccount, enabled: true } : currentAccount
            )));
            setApprovalStatus(`${accountToApprove.username} approved`);
        } catch (error) {
            setApprovalStatus(error instanceof Error ? error.message : "Approval failed");
        } finally {
            setApprovalBusyId(null);
            setApprovalBusyAction(null);
        }
    }

    async function rejectAccount(accountToReject: Account) {
        setApprovalBusyId(accountToReject.id);
        setApprovalBusyAction("reject");
        setApprovalStatus(null);

        try {
            await fetcher<void>(`/api/admin/accounts/${accountToReject.id}`, {
                method: "DELETE",
            });
            setAccounts((currentAccounts) => currentAccounts.filter((currentAccount) => currentAccount.id !== accountToReject.id));
            setApprovalStatus(`${accountToReject.username} rejected`);
        } catch (error) {
            setApprovalStatus(error instanceof Error ? error.message : "Rejection failed");
        } finally {
            setApprovalBusyId(null);
            setApprovalBusyAction(null);
        }
    }

    async function createBooking(event: FormEvent) {
        event.preventDefault();

        if (!account) {
            setBookingError("Account is still loading");
            return;
        }

        const sensorId = bookingSensor || selectedBookingSpace?.sensorId;
        const space = spaces.find((candidate) => candidate.sensorId === sensorId);

        if (!sensorId || !space) {
            setBookingError("Choose an available bay");
            return;
        }

        if (!isStaff && space.disabilityPermitRequired && !accessibleEligible) {
            setBookingError("Accessible bays are only shown when you tick accessible parking eligibility");
            return;
        }

        const registration = bookingRegistration.trim().toUpperCase();
        const permitNumber = bookingPermitNumber.trim();

        if (!registration) {
            setBookingError("Enter your vehicle registration");
            return;
        }

        if ((accessibleEligible || space.disabilityPermitRequired) && !permitNumber) {
            setBookingError("Enter your Mobility Parking Scheme permit number");
            return;
        }

        setBookingBusy("create");
        setBookingStatus(null);
        setBookingError(null);

        try {
            const booking = await fetcher<ParkingBooking>("/api/parking", {
                method: "POST",
                body: JSON.stringify({
                    accountId: account.id,
                    parkingLot: space.lotName,
                    parkingSpace: sensorId,
                    vehicle: registration,
                    mobilityParkingPermitNumber: permitNumber || undefined,
                    startTime: bookingStart,
                    endTime: bookingEnd,
                }),
            });
            setBookingStatus(`${bookingBayLabel(booking)} booked`);
            setBookingSensor("");
            setBookingRegistration("");
            setBookingPermitNumber("");
            setBookingStart(defaultBookingStart());
            setBookingEnd(defaultBookingEnd());
            await Promise.all([loadProjectData(), loadSpotter()]);
        } catch (error) {
            setBookingError(error instanceof Error ? error.message : "Booking failed");
        } finally {
            setBookingBusy(null);
        }
    }

    async function cancelBooking(booking: ParkingBooking) {
        setBookingBusy(`cancel-${booking.id}`);
        setBookingStatus(null);
        setBookingError(null);

        try {
            await fetcher<ParkingBooking>(`/api/parking/${booking.id}/cancel`, {
                method: "PATCH",
            });
            setBookingStatus(`${bookingBayLabel(booking)} cancelled`);
            await Promise.all([loadProjectData(), loadSpotter()]);
        } catch (error) {
            setBookingError(error instanceof Error ? error.message : "Cancellation failed");
        } finally {
            setBookingBusy(null);
        }
    }

    async function deleteBooking(booking: ParkingBooking) {
        setBookingBusy(`delete-${booking.id}`);
        setBookingStatus(null);
        setBookingError(null);

        try {
            await fetcher<void>(`/api/parking/${booking.id}`, {
                method: "DELETE",
            });
            setBookingStatus(`${bookingBayLabel(booking)} deleted`);
            await Promise.all([loadProjectData(), loadSpotter()]);
        } catch (error) {
            setBookingError(error instanceof Error ? error.message : "Delete failed");
        } finally {
            setBookingBusy(null);
        }
    }

    async function runSimulation(eventCount: number) {
        setActionBusy(`run-${eventCount}`);
        setActionStatus(null);
        setActionEvents([]);

        try {
            const result = await spotterFetcher<SimulationRunResponse>("/simulation/run", {
                method: "POST",
                body: JSON.stringify({
                    eventCount,
                    publishEvents: true,
                }),
            });

            setSummary(result.summary);
            setActionEvents(result.events.map((item) => item.event));
            setActionStatus(`${result.appliedEvents} update${result.appliedEvents === 1 ? "" : "s"} applied`);
            await Promise.all([loadSpotter(), loadProjectData()]);
        } catch (error) {
            setActionEvents([]);
            setActionStatus(error instanceof Error ? error.message : "Sensor feed failed");
        } finally {
            setActionBusy(null);
        }
    }

    async function resetSimulation() {
        setActionBusy("reset");
        setActionStatus(null);
        setActionEvents([]);

        try {
            await spotterFetcher<SimulationRunResponse>("/simulation/reset", {
                method: "POST",
            });
            setActionStatus("Parking data reset");
            await Promise.all([loadSpotter(), loadProjectData()]);
        } catch (error) {
            setActionEvents([]);
            setActionStatus(error instanceof Error ? error.message : "Reset failed");
        } finally {
            setActionBusy(null);
        }
    }

    async function recordManualReading() {
        const sensorId = selectedSensor || spaces[0]?.sensorId;

        if (!sensorId) {
            return;
        }

        setActionBusy("manual");
        setActionStatus(null);
        setActionEvents([]);

        try {
            const event = await spotterFetcher<DetectionEvent>(`/sensors/${encodeURIComponent(sensorId)}/detect`, {
                method: "POST",
                body: JSON.stringify({
                    occupied: manualOccupied === "true",
                    confidence: 0.99,
                    source: "staff-dashboard",
                }),
            });
            setActionEvents([event]);
            setActionStatus("Manual reading recorded");
            await Promise.all([loadSpotter(), loadProjectData()]);
        } catch (error) {
            setActionEvents([]);
            setActionStatus(error instanceof Error ? error.message : "Manual reading failed");
        } finally {
            setActionBusy(null);
        }
    }

    return (
        <div className="min-h-screen bg-[#f7f7f4] text-slate-950">
            <main className="mx-auto flex w-full max-w-7xl flex-col gap-6 px-4 py-6 sm:px-6 lg:px-8">
                <header className="flex flex-col gap-4 border-b border-stone-300 pb-5 lg:flex-row lg:items-end lg:justify-between">
                    <div className="space-y-2">
                        <p className="text-sm font-semibold uppercase tracking-normal text-teal-700">
                            {isStaff ? "Staff dashboard" : "User dashboard"}
                        </p>
                        <h1 className="text-3xl font-semibold tracking-normal text-slate-950 sm:text-4xl">
                            {isStaff ? "Parking operations" : "Find a parking space"}
                        </h1>
                        <div className="flex flex-wrap items-center gap-3 text-sm text-slate-600">
                            <span className="inline-flex items-center gap-2 rounded-md border border-emerald-200 bg-emerald-50 px-3 py-1 font-medium text-emerald-800">
                                <span className="h-2 w-2 rounded-full bg-emerald-500" />
                                Live
                            </span>
                            <span>Last sync {formatTime(lastSync)}</span>
                            {account && <span>{account.username} - {account.role}</span>}
                            <button
                                type="button"
                                onClick={logout}
                                className="rounded-md border border-stone-300 bg-white px-3 py-1 font-medium text-slate-700 transition hover:bg-stone-100"
                            >
                                Logout
                            </button>
                        </div>
                    </div>

                    <div className="flex flex-wrap gap-2">
                        <label className="flex min-w-36 flex-col gap-1 text-sm font-medium text-slate-700">
                            Lot
                            <select
                                value={selectedLot}
                                onChange={(event) => {
                                    setSelectedLot(event.target.value);
                                    setSelectedZone("all");
                                }}
                                className="h-10 rounded-md border border-stone-300 bg-white px-3 text-sm text-slate-950 shadow-sm outline-none focus:border-teal-500"
                            >
                                <option value="all">All lots</option>
                                {lots.map((lot) => (
                                    <option key={lot} value={lot}>
                                        {lot}
                                    </option>
                                ))}
                            </select>
                        </label>

                        <label className="flex min-w-36 flex-col gap-1 text-sm font-medium text-slate-700">
                            Bay type
                            <select
                                value={selectedZone}
                                onChange={(event) => setSelectedZone(event.target.value)}
                                className="h-10 rounded-md border border-stone-300 bg-white px-3 text-sm text-slate-950 shadow-sm outline-none focus:border-teal-500"
                            >
                                <option value="all">All bay types</option>
                                {zoneNames.map((zone) => (
                                    <option key={zone} value={zone}>
                                        {zoneName(zone)}
                                    </option>
                                ))}
                            </select>
                        </label>

                        {!isStaff && (
                            <label className="flex min-h-10 min-w-56 items-center gap-2 rounded-md border border-stone-300 bg-white px-3 text-sm font-medium text-slate-700 shadow-sm">
                                <input
                                    type="checkbox"
                                    checked={accessibleEligible}
                                    onChange={(event) => {
                                        const checked = event.target.checked;
                                        setAccessibleEligible(checked);

                                        if (!checked) {
                                            if (selectedZone === "Accessible") {
                                                setSelectedZone("all");
                                            }

                                            const selectedSpace = spaces.find((space) => space.sensorId === bookingSensor);
                                            if (selectedSpace?.disabilityPermitRequired) {
                                                setBookingSensor("");
                                            }

                                            setBookingPermitNumber("");
                                        }
                                    }}
                                    className="h-4 w-4 rounded border-stone-300 text-teal-700 focus:ring-teal-600"
                                />
                                Eligible for accessible parking
                            </label>
                        )}
                    </div>
                </header>

                {(spotterError || accountError || bookingError || predictionError) && (
                    <section className="rounded-lg border border-amber-300 bg-amber-50 px-4 py-3 text-sm text-amber-900">
                        {spotterError && <p>Parking availability: {spotterError}</p>}
                        {accountError && <p>Accounts: {accountError}</p>}
                        {bookingError && <p>Bookings: {bookingError}</p>}
                        {predictionError && <p>Predictions: {predictionError}</p>}
                    </section>
                )}

                <section className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
                    <Metric label="Available" value={metricValue(summary?.availableSpaces)} detail={`${metricValue(summary?.totalSpaces)} total bays`} tone="emerald" />
                    <Metric label="Occupied" value={metricValue(summary?.occupiedSpaces)} detail={`${percent(summary?.occupancyRate ?? 0)} occupancy`} tone="rose" />
                    <Metric label="Accessible bays free" value={metricValue(summary?.availableDisabilityPermitSpaces)} detail={`${metricValue(summary?.disabilityPermitSpaces)} accessible bays`} tone="sky" />
                    <Metric
                        label={isStaff ? "Accounts" : "Best car park"}
                        value={isStaff ? metricValue(staffStats.total) : bestZone ? bestZone.lotName : "Checking"}
                        detail={isStaff ? `${staffStats.staff} staff/admin, ${staffStats.disabled} disabled` : bestZone ? `${bestZone.availableSpaces} free ${zoneName(bestZone.zone).toLowerCase()}` : "No bays match filters"}
                        tone="violet"
                    />
                </section>

                {isStaff && (
                    <section className="grid gap-3 sm:grid-cols-3">
                        <Metric label="User accounts" value={metricValue(staffStats.users)} detail={`${staffStats.premium} premium subscriptions`} tone="sky" />
                        <Metric label="Sensor events" value={metricValue(events.length)} detail="Recent stored readings" tone="amber" />
                        <Metric label="Active bookings" value={metricValue(activeBookings.length)} detail={`${metricValue(bookings.length)} booking records`} tone="emerald" />
                    </section>
                )}

                {isStaff && (
                    <BookingManager
                        bookings={bookings}
                        busy={bookingBusy}
                        status={bookingStatus}
                        onCancel={cancelBooking}
                        onDelete={deleteBooking}
                    />
                )}

                {!isStaff && (
                    <SmartRecommendations
                        bestZone={bestZone}
                        bestPrediction={bestPrediction}
                        nearestSpace={nearestSpace}
                    />
                )}

                {!isStaff && (
                    <section className="grid gap-4 lg:grid-cols-[minmax(0,1fr)_minmax(320px,420px)]">
                        <BookingPanel
                            spaces={bookableSpaces}
                            bookingSensor={bookingSensor || selectedBookingSpace?.sensorId || ""}
                            registration={bookingRegistration}
                            permitNumber={bookingPermitNumber}
                            showPermitNumber={accessibleEligible}
                            startTime={bookingStart}
                            endTime={bookingEnd}
                            busy={bookingBusy}
                            status={bookingStatus}
                            onSensorChange={setBookingSensor}
                            onRegistrationChange={setBookingRegistration}
                            onPermitNumberChange={setBookingPermitNumber}
                            onStartChange={setBookingStart}
                            onEndChange={setBookingEnd}
                            onSubmit={createBooking}
                        />
                        <BookingList
                            bookings={bookings.slice(0, 8)}
                            title="Your bookings"
                            emptyText="No bookings yet."
                            busy={bookingBusy}
                            onCancel={cancelBooking}
                        />
                    </section>
                )}

                {isStaff && (
                    <section className="grid gap-4 lg:grid-cols-[minmax(0,1fr)_minmax(320px,420px)]">
                        <StaffControls
                            spaces={spaces}
                            selectedSensor={selectedSensor || spaces[0]?.sensorId || ""}
                            manualOccupied={manualOccupied}
                            actionBusy={actionBusy}
                            actionStatus={actionStatus}
                            actionEvents={actionEvents}
                            onSensorChange={setSelectedSensor}
                            onOccupiedChange={setManualOccupied}
                            onRunSimulation={runSimulation}
                            onReset={resetSimulation}
                            onRecordManual={recordManualReading}
                        />
                        <RecentEvents events={recentEvents} totalEvents={events.length} />
                    </section>
                )}

                {isStaff && account?.role === "ADMIN" && (
                    <PendingAccountApprovals
                        accounts={pendingApprovals}
                        approvalBusyId={approvalBusyId}
                        approvalBusyAction={approvalBusyAction}
                        approvalStatus={approvalStatus}
                        onApprove={approveAccount}
                        onReject={rejectAccount}
                    />
                )}

                {isStaff && (
                    <section className="grid gap-4 lg:grid-cols-[minmax(0,1fr)_minmax(320px,420px)]">
                        <BookingList
                            bookings={bookings.slice(0, 10)}
                            title="Booking activity"
                            emptyText="No booking records yet."
                            busy={bookingBusy}
                            onCancel={cancelBooking}
                            onDelete={deleteBooking}
                        />
                        <StaffAnalytics
                            adminSummary={adminSummary}
                            predictions={predictions}
                        />
                    </section>
                )}

                <section className="grid gap-4 lg:grid-cols-[minmax(0,0.95fr)_minmax(0,1.05fr)]">
                    <ZoneOverview zones={visibleZones} />
                    <SpaceList spaces={visibleSpaces} isStaff={isStaff} />
                </section>
            </main>
        </div>
    );
}

function Metric({ label, value, detail, tone }: { label: string; value: string; detail: string; tone: "emerald" | "rose" | "sky" | "violet" | "amber" }) {
    const tones = {
        emerald: "border-emerald-200 bg-emerald-50",
        rose: "border-rose-200 bg-rose-50",
        sky: "border-sky-200 bg-sky-50",
        violet: "border-violet-200 bg-violet-50",
        amber: "border-amber-200 bg-amber-50",
    };

    return (
        <div className={`rounded-lg border p-4 shadow-sm ${tones[tone]}`}>
            <p className="text-sm font-medium text-slate-600">{label}</p>
            <p className="mt-2 break-words text-3xl font-semibold tracking-normal text-slate-950">{value}</p>
            <p className="mt-1 text-sm text-slate-600">{detail}</p>
        </div>
    );
}

function SmartRecommendations({
    bestZone,
    bestPrediction,
    nearestSpace,
}: {
    bestZone?: ZoneSummary;
    bestPrediction?: OccupancyPrediction;
    nearestSpace?: Space;
}) {
    const nearestDistance = nearestSpace ? distanceMeters(nearestSpace) : Number.POSITIVE_INFINITY;

    return (
        <section className="grid gap-3 md:grid-cols-3">
            <RecommendationCard
                label="Best now"
                value={bestZone ? bestZone.lotName : "Checking"}
                detail={bestZone ? `${bestZone.availableSpaces} free ${zoneName(bestZone.zone).toLowerCase()} right now` : "Waiting for live availability"}
            />
            <RecommendationCard
                label="Best later"
                value={bestPrediction ? bestPrediction.lotId : "Checking"}
                detail={bestPrediction ? `${probability(bestPrediction.availabilityProbability)} availability, about ${bestPrediction.estimatedAvailableSpaces} bays free` : "Waiting for forecast"}
            />
            <RecommendationCard
                label="Closest free"
                value={nearestSpace ? nearestSpace.displayName : "Checking"}
                detail={nearestSpace ? formatDistance(nearestDistance) : "No free bays match filters"}
            />
        </section>
    );
}

function RecommendationCard({ label, value, detail }: { label: string; value: string; detail: string }) {
    return (
        <article className="rounded-lg border border-stone-300 bg-white p-4 shadow-sm">
            <p className="text-sm font-medium text-slate-600">{label}</p>
            <p className="mt-2 break-words text-xl font-semibold tracking-normal text-slate-950">{value}</p>
            <p className="mt-1 text-sm text-slate-600">{detail}</p>
        </article>
    );
}

function BookingPanel({
    spaces,
    bookingSensor,
    registration,
    permitNumber,
    showPermitNumber,
    startTime,
    endTime,
    busy,
    status,
    onSensorChange,
    onRegistrationChange,
    onPermitNumberChange,
    onStartChange,
    onEndChange,
    onSubmit,
}: {
    spaces: Space[];
    bookingSensor: string;
    registration: string;
    permitNumber: string;
    showPermitNumber: boolean;
    startTime: string;
    endTime: string;
    busy: string | null;
    status: string | null;
    onSensorChange: (value: string) => void;
    onRegistrationChange: (value: string) => void;
    onPermitNumberChange: (value: string) => void;
    onStartChange: (value: string) => void;
    onEndChange: (value: string) => void;
    onSubmit: (event: FormEvent) => Promise<void>;
}) {
    return (
        <section className="rounded-lg border border-stone-300 bg-white p-4 shadow-sm">
            <div className="flex items-center justify-between gap-3">
                <div>
                    <h2 className="text-lg font-semibold text-slate-950">Book a parking bay</h2>
                    <p className="text-sm text-slate-600">Reserve an available parking bay.</p>
                </div>
                <span className="rounded-md bg-slate-100 px-2 py-1 text-xs font-semibold text-slate-600">{spaces.length}</span>
            </div>

            <form onSubmit={(event) => void onSubmit(event)} className="mt-4 grid gap-3">
                <label className="flex min-w-0 flex-col gap-1 text-sm font-medium text-slate-700">
                    Parking bay
                    <select
                        value={bookingSensor}
                        onChange={(event) => onSensorChange(event.target.value)}
                        className="h-10 rounded-md border border-stone-300 bg-white px-3 text-sm text-slate-950 outline-none focus:border-teal-500"
                    >
                        {spaces.map((space) => (
                            <option key={space.sensorId} value={space.sensorId}>
                                {space.displayName}
                            </option>
                        ))}
                    </select>
                </label>

                <label className="flex min-w-0 flex-col gap-1 text-sm font-medium text-slate-700">
                    Vehicle registration
                    <input
                        value={registration}
                        onChange={(event) => onRegistrationChange(event.target.value.toUpperCase())}
                        placeholder="ABC123"
                        className="h-10 rounded-md border border-stone-300 bg-white px-3 text-sm text-slate-950 outline-none focus:border-teal-500"
                        required
                    />
                </label>

                {showPermitNumber && (
                    <label className="flex min-w-0 flex-col gap-1 text-sm font-medium text-slate-700">
                        Mobility Parking Scheme permit number
                        <input
                            value={permitNumber}
                            onChange={(event) => onPermitNumberChange(event.target.value.toUpperCase())}
                            placeholder="MPS permit number"
                            className="h-10 rounded-md border border-stone-300 bg-white px-3 text-sm text-slate-950 outline-none focus:border-teal-500"
                            required
                        />
                    </label>
                )}

                <div className="grid gap-3 sm:grid-cols-2">
                    <label className="flex min-w-0 flex-col gap-1 text-sm font-medium text-slate-700">
                        Start
                        <input
                            type="datetime-local"
                            value={startTime}
                            onChange={(event) => onStartChange(event.target.value)}
                            className="h-10 rounded-md border border-stone-300 bg-white px-3 text-sm text-slate-950 outline-none focus:border-teal-500"
                            required
                        />
                    </label>
                    <label className="flex min-w-0 flex-col gap-1 text-sm font-medium text-slate-700">
                        End
                        <input
                            type="datetime-local"
                            value={endTime}
                            onChange={(event) => onEndChange(event.target.value)}
                            className="h-10 rounded-md border border-stone-300 bg-white px-3 text-sm text-slate-950 outline-none focus:border-teal-500"
                            required
                        />
                    </label>
                </div>

                <button
                    type="submit"
                    disabled={Boolean(busy) || spaces.length === 0}
                    className="h-10 rounded-md bg-teal-700 px-4 text-sm font-semibold text-white transition hover:bg-teal-800 disabled:cursor-not-allowed disabled:opacity-50"
                >
                    {busy === "create" ? "Booking" : "Book bay"}
                </button>
            </form>

            {status && <p className="mt-3 text-sm font-medium text-slate-700">{status}</p>}
        </section>
    );
}

function bookingOptionLabel(booking: ParkingBooking) {
    return `${bookingBayLabel(booking)} - Rego ${booking.vehicle} - Account ${booking.accountId} - ${booking.status.toLowerCase()}`;
}

function BookingManager({
    bookings,
    busy,
    status,
    onCancel,
    onDelete,
}: {
    bookings: ParkingBooking[];
    busy: string | null;
    status: string | null;
    onCancel: (booking: ParkingBooking) => Promise<void>;
    onDelete: (booking: ParkingBooking) => Promise<void>;
}) {
    const [selectedBookingId, setSelectedBookingId] = useState("");
    const selectedBooking = bookings.find((booking) => String(booking.id) === selectedBookingId) ?? bookings[0];
    const cancellable = selectedBooking?.status === "RESERVED" || selectedBooking?.status === "ACTIVE";

    return (
        <section className="rounded-lg border border-stone-300 bg-white p-4 shadow-sm">
            <div className="flex items-center justify-between gap-3">
                <h2 className="text-lg font-semibold text-slate-950">Manage bookings</h2>
                <span className="rounded-md bg-slate-100 px-2 py-1 text-xs font-semibold text-slate-600">{bookings.length}</span>
            </div>

            <div className="mt-4 grid gap-3 lg:grid-cols-[minmax(0,1fr)_auto] lg:items-end">
                <label className="flex min-w-0 flex-col gap-1 text-sm font-medium text-slate-700">
                    Booking
                    <select
                        value={selectedBooking ? String(selectedBooking.id) : ""}
                        onChange={(event) => setSelectedBookingId(event.target.value)}
                        disabled={bookings.length === 0}
                        className="h-10 rounded-md border border-stone-300 bg-white px-3 text-sm text-slate-950 outline-none focus:border-teal-500 disabled:cursor-not-allowed disabled:opacity-50"
                    >
                        {bookings.length === 0 && <option value="">No bookings</option>}
                        {bookings.map((booking) => (
                            <option key={booking.id} value={booking.id}>
                                {bookingOptionLabel(booking)}
                            </option>
                        ))}
                    </select>
                </label>

                <div className="flex flex-wrap gap-2">
                    <button
                        type="button"
                        onClick={() => selectedBooking && void onCancel(selectedBooking)}
                        disabled={Boolean(busy) || !selectedBooking || !cancellable}
                        className="h-10 rounded-md border border-stone-300 bg-white px-4 text-sm font-semibold text-slate-700 transition hover:bg-stone-100 disabled:cursor-not-allowed disabled:opacity-50"
                    >
                        {selectedBooking && busy === `cancel-${selectedBooking.id}` ? "Cancelling" : "Cancel"}
                    </button>
                    <button
                        type="button"
                        onClick={() => selectedBooking && void onDelete(selectedBooking)}
                        disabled={Boolean(busy) || !selectedBooking}
                        className="h-10 rounded-md border border-rose-200 bg-rose-50 px-4 text-sm font-semibold text-rose-800 transition hover:bg-rose-100 disabled:cursor-not-allowed disabled:opacity-50"
                    >
                        {selectedBooking && busy === `delete-${selectedBooking.id}` ? "Deleting" : "Delete"}
                    </button>
                </div>
            </div>

            {selectedBooking && (
                <div className="mt-3 grid gap-2 text-xs text-slate-600 sm:grid-cols-5">
                    <p><span className="font-semibold text-slate-900">Bay:</span> {bookingBayLabel(selectedBooking)}</p>
                    <p><span className="font-semibold text-slate-900">Rego:</span> {selectedBooking.vehicle}</p>
                    <p><span className="font-semibold text-slate-900">Account:</span> {selectedBooking.accountId}</p>
                    <p><span className="font-semibold text-slate-900">Status:</span> {selectedBooking.status.toLowerCase()}</p>
                    <p><span className="font-semibold text-slate-900">MPS permit:</span> {selectedBooking.mobilityParkingPermitNumber || "Not supplied"}</p>
                </div>
            )}

            {status && <p className="mt-3 text-sm font-medium text-slate-700">{status}</p>}
        </section>
    );
}

function BookingList({
    bookings,
    title,
    emptyText,
    busy,
    onCancel,
    onDelete,
}: {
    bookings: ParkingBooking[];
    title: string;
    emptyText: string;
    busy: string | null;
    onCancel: (booking: ParkingBooking) => Promise<void>;
    onDelete?: (booking: ParkingBooking) => Promise<void>;
}) {
    return (
        <section className="rounded-lg border border-stone-300 bg-white p-4 shadow-sm">
            <div className="flex items-center justify-between gap-3">
                <h2 className="text-lg font-semibold text-slate-950">{title}</h2>
                <span className="rounded-md bg-slate-100 px-2 py-1 text-xs font-semibold text-slate-600">{bookings.length}</span>
            </div>

            <div className="mt-3 divide-y divide-stone-200">
                {bookings.length === 0 && <p className="py-6 text-sm text-slate-600">{emptyText}</p>}
                {bookings.map((booking) => {
                    const cancellable = booking.status === "RESERVED" || booking.status === "ACTIVE";
                    return (
                        <div key={booking.id} className="grid gap-3 py-3 sm:grid-cols-[minmax(0,1fr)_auto] sm:items-center">
                            <div className="min-w-0">
                                <div className="flex flex-wrap items-center gap-2">
                                    <p className="truncate text-sm font-semibold text-slate-950">{bookingBayLabel(booking)}</p>
                                    <StatusPill status={booking.status} />
                                </div>
                                <p className="truncate text-xs text-slate-600">
                                    {booking.parkingLot} - Rego {booking.vehicle}
                                </p>
                                <p className="truncate text-xs text-slate-500">
                                    {formatDateTime(booking.startTime)} to {formatDateTime(booking.endTime)}
                                </p>
                                {booking.mobilityParkingPermitNumber && (
                                    <p className="truncate text-xs text-slate-500">
                                        MPS permit {booking.mobilityParkingPermitNumber}
                                    </p>
                                )}
                            </div>
                            {(cancellable || onDelete) && (
                                <div className="flex flex-wrap gap-2 sm:justify-end">
                                    {cancellable && (
                                        <button
                                            type="button"
                                            onClick={() => void onCancel(booking)}
                                            disabled={Boolean(busy)}
                                            className="h-9 rounded-md border border-stone-300 bg-white px-3 text-sm font-semibold text-slate-700 transition hover:bg-stone-100 disabled:cursor-not-allowed disabled:opacity-50"
                                        >
                                            {busy === `cancel-${booking.id}` ? "Cancelling" : "Cancel"}
                                        </button>
                                    )}
                                    {onDelete && (
                                        <button
                                            type="button"
                                            onClick={() => void onDelete(booking)}
                                            disabled={Boolean(busy)}
                                            className="h-9 rounded-md border border-rose-200 bg-rose-50 px-3 text-sm font-semibold text-rose-800 transition hover:bg-rose-100 disabled:cursor-not-allowed disabled:opacity-50"
                                        >
                                            {busy === `delete-${booking.id}` ? "Deleting" : "Delete"}
                                        </button>
                                    )}
                                </div>
                            )}
                        </div>
                    );
                })}
            </div>
        </section>
    );
}

function StaffAnalytics({
    adminSummary,
    predictions,
}: {
    adminSummary: AdminSummary | null;
    predictions: OccupancyPrediction[];
}) {
    const bestPrediction = predictions[0];

    return (
        <section className="rounded-lg border border-stone-300 bg-white p-4 shadow-sm">
            <div className="flex items-center justify-between gap-3">
                <h2 className="text-lg font-semibold text-slate-950">Predictions and stats</h2>
                <span className="rounded-md bg-slate-100 px-2 py-1 text-xs font-semibold text-slate-600">{predictions.length}</span>
            </div>

            <div className="mt-4 grid gap-3">
                <MiniStat
                    label="Best predicted lot"
                    value={bestPrediction ? bestPrediction.lotId : "Checking"}
                    detail={bestPrediction ? `${probability(bestPrediction.availabilityProbability)} availability` : "Prediction service loading"}
                />
                <MiniStat
                    label="Latest occupancy snapshot"
                    value={adminSummary?.occupancySnapshot ? `${adminSummary.occupancySnapshot.spotsTaken}/${adminSummary.occupancySnapshot.spotsTotal}` : "Unavailable"}
                    detail={adminSummary?.occupancySnapshot ? `${adminSummary.occupancySnapshot.hour}:00 on ${adminSummary.date}` : "Admin stats service loading"}
                />
                <MiniStat
                    label="Peak and utilisation"
                    value={adminSummary?.peakHourSnapshot ? `${adminSummary.peakHourSnapshot.hour}:00 peak` : "Unavailable"}
                    detail={adminSummary?.utilisationSnapshot ? `${percent(adminSummary.utilisationSnapshot.utilisationRate)} daily utilisation` : "Waiting for analytics"}
                />
            </div>
        </section>
    );
}

function MiniStat({ label, value, detail }: { label: string; value: string; detail: string }) {
    return (
        <div className="rounded-md border border-stone-200 p-3">
            <p className="text-xs font-medium text-slate-500">{label}</p>
            <p className="mt-1 break-words text-base font-semibold text-slate-950">{value}</p>
            <p className="mt-1 text-xs text-slate-600">{detail}</p>
        </div>
    );
}

function StaffControls({
    spaces,
    selectedSensor,
    manualOccupied,
    actionBusy,
    actionStatus,
    actionEvents,
    onSensorChange,
    onOccupiedChange,
    onRunSimulation,
    onReset,
    onRecordManual,
}: {
    spaces: Space[];
    selectedSensor: string;
    manualOccupied: string;
    actionBusy: string | null;
    actionStatus: string | null;
    actionEvents: DetectionEvent[];
    onSensorChange: (value: string) => void;
    onOccupiedChange: (value: string) => void;
    onRunSimulation: (count: number) => Promise<void>;
    onReset: () => Promise<void>;
    onRecordManual: () => Promise<void>;
}) {
    return (
        <section className="rounded-lg border border-stone-300 bg-white p-4 shadow-sm">
            <div className="flex flex-col gap-1">
                <h2 className="text-lg font-semibold text-slate-950">Live bay updates</h2>
                <p className="text-sm text-slate-600">Apply bay readings to the live parking view.</p>
            </div>

            <div className="mt-4 flex flex-wrap gap-2">
                <button
                    type="button"
                    onClick={() => void onRunSimulation(1)}
                    disabled={Boolean(actionBusy)}
                    className="h-10 rounded-md bg-slate-950 px-4 text-sm font-semibold text-white transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-50"
                >
                    {actionBusy === "run-1" ? "Applying" : "Next event"}
                </button>
                <button
                    type="button"
                    onClick={() => void onRunSimulation(5)}
                    disabled={Boolean(actionBusy)}
                    className="h-10 rounded-md border border-stone-300 bg-white px-4 text-sm font-semibold text-slate-950 transition hover:bg-stone-100 disabled:cursor-not-allowed disabled:opacity-50"
                >
                    {actionBusy === "run-5" ? "Applying" : "Run 5"}
                </button>
                <button
                    type="button"
                    onClick={() => void onReset()}
                    disabled={Boolean(actionBusy)}
                    className="h-10 rounded-md border border-rose-200 bg-rose-50 px-4 text-sm font-semibold text-rose-800 transition hover:bg-rose-100 disabled:cursor-not-allowed disabled:opacity-50"
                >
                    {actionBusy === "reset" ? "Resetting" : "Reset"}
                </button>
            </div>

            <div className="mt-5 grid gap-3 sm:grid-cols-[minmax(0,1fr)_140px_auto]">
                <label className="flex min-w-0 flex-col gap-1 text-sm font-medium text-slate-700">
                    Bay sensor
                    <select
                        value={selectedSensor}
                        onChange={(event) => onSensorChange(event.target.value)}
                        className="h-10 rounded-md border border-stone-300 bg-white px-3 text-sm text-slate-950 outline-none focus:border-teal-500"
                    >
                        {spaces.map((space) => (
                            <option key={space.sensorId} value={space.sensorId}>
                                {space.sensorId}
                            </option>
                        ))}
                    </select>
                </label>
                <label className="flex flex-col gap-1 text-sm font-medium text-slate-700">
                    Status
                    <select
                        value={manualOccupied}
                        onChange={(event) => onOccupiedChange(event.target.value)}
                        className="h-10 rounded-md border border-stone-300 bg-white px-3 text-sm text-slate-950 outline-none focus:border-teal-500"
                    >
                        <option value="true">Occupied</option>
                        <option value="false">Free</option>
                    </select>
                </label>
                <button
                    type="button"
                    onClick={() => void onRecordManual()}
                    disabled={Boolean(actionBusy) || !selectedSensor}
                    className="mt-6 h-10 rounded-md bg-teal-700 px-4 text-sm font-semibold text-white transition hover:bg-teal-800 disabled:cursor-not-allowed disabled:opacity-50 sm:mt-auto"
                >
                    {actionBusy === "manual" ? "Saving" : "Record"}
                </button>
            </div>

            {actionStatus && <p className="mt-3 text-sm font-medium text-slate-700">{actionStatus}</p>}
            {actionEvents.length > 0 && (
                <div className="mt-3 divide-y divide-stone-200 rounded-md border border-stone-200 bg-stone-50">
                    {actionEvents.map((event) => (
                        <div key={event.id} className="grid gap-2 px-3 py-2 text-sm sm:grid-cols-[minmax(0,1fr)_auto]">
                            <div className="min-w-0">
                                <p className="truncate font-semibold text-slate-950">
                                    {event.sensorId} - {eventChangeText(event)}
                                </p>
                                <p className="truncate text-xs text-slate-600">
                                    {event.lotName} - {zoneName(event.zone)} - Bay {event.bayNumber}
                                </p>
                            </div>
                            <div className="flex items-center gap-2 sm:justify-end">
                                <OccupancyPill occupied={event.occupied} />
                                <span className="text-xs text-slate-500">{formatTime(event.detectedAt)}</span>
                            </div>
                        </div>
                    ))}
                </div>
            )}
        </section>
    );
}

function RecentEvents({ events, totalEvents }: { events: DetectionEvent[]; totalEvents: number }) {
    return (
        <section className="rounded-lg border border-stone-300 bg-white p-4 shadow-sm">
            <div className="flex items-center justify-between gap-3">
                <div>
                    <h2 className="text-lg font-semibold text-slate-950">Recent events</h2>
                    {totalEvents > events.length && <p className="text-xs text-slate-500">Showing latest {events.length} of {totalEvents}</p>}
                </div>
                <span className="rounded-md bg-slate-100 px-2 py-1 text-xs font-semibold text-slate-600">{totalEvents}</span>
            </div>
            <div className="mt-3 divide-y divide-stone-200">
                {events.length === 0 && <p className="py-6 text-sm text-slate-600">No bay updates yet.</p>}
                {events.map((event) => (
                    <div key={event.id} className="grid grid-cols-[minmax(0,1fr)_auto] gap-3 py-3">
                        <div className="min-w-0">
                            <p className="truncate text-sm font-semibold text-slate-950">
                                {event.lotName} - {zoneName(event.zone)} - Bay {event.bayNumber}
                            </p>
                            <p className="truncate text-xs text-slate-600">
                                {event.sensorId} - {event.source} - {eventChangeText(event)}
                            </p>
                        </div>
                        <div className="text-right">
                            <OccupancyPill occupied={event.occupied} />
                            <p className="mt-1 text-xs text-slate-500">{formatTime(event.detectedAt)}</p>
                        </div>
                    </div>
                ))}
            </div>
        </section>
    );
}

function PendingAccountApprovals({
    accounts,
    approvalBusyId,
    approvalBusyAction,
    approvalStatus,
    onApprove,
    onReject,
}: {
    accounts: Account[];
    approvalBusyId: number | null;
    approvalBusyAction: "approve" | "reject" | null;
    approvalStatus: string | null;
    onApprove: (account: Account) => Promise<void>;
    onReject: (account: Account) => Promise<void>;
}) {
    return (
        <section className="rounded-lg border border-stone-300 bg-white p-4 shadow-sm">
            <div className="flex items-center justify-between gap-3">
                <h2 className="text-lg font-semibold text-slate-950">Staff/admin approvals</h2>
                <span className="rounded-md bg-slate-100 px-2 py-1 text-xs font-semibold text-slate-600">{accounts.length}</span>
            </div>

            {approvalStatus && <p className="mt-3 text-sm font-medium text-slate-700">{approvalStatus}</p>}

            <div className="mt-3 divide-y divide-stone-200">
                {accounts.length === 0 && <p className="py-4 text-sm text-slate-600">No pending staff or admin accounts.</p>}
                {accounts.map((adminAccount) => (
                    <div key={adminAccount.id} className="grid gap-3 py-3 sm:grid-cols-[minmax(0,1fr)_auto] sm:items-center">
                        <div className="min-w-0">
                            <div className="flex flex-wrap items-center gap-2">
                                <p className="truncate text-sm font-semibold text-slate-950">{adminAccount.username}</p>
                                <span className="rounded-md bg-stone-100 px-2 py-1 text-xs font-semibold text-slate-600">{adminAccount.role}</span>
                            </div>
                            <p className="truncate text-xs text-slate-600">{adminAccount.email}</p>
                        </div>
                        <div className="flex flex-wrap gap-2">
                            <button
                                type="button"
                                onClick={() => void onApprove(adminAccount)}
                                disabled={approvalBusyId !== null}
                                className="h-10 rounded-md bg-teal-700 px-4 text-sm font-semibold text-white transition hover:bg-teal-800 disabled:cursor-not-allowed disabled:opacity-50"
                            >
                                {approvalBusyId === adminAccount.id && approvalBusyAction === "approve" ? "Approving" : "Approve"}
                            </button>
                            <button
                                type="button"
                                onClick={() => void onReject(adminAccount)}
                                disabled={approvalBusyId !== null}
                                className="h-10 rounded-md border border-rose-200 bg-rose-50 px-4 text-sm font-semibold text-rose-800 transition hover:bg-rose-100 disabled:cursor-not-allowed disabled:opacity-50"
                            >
                                {approvalBusyId === adminAccount.id && approvalBusyAction === "reject" ? "Rejecting" : "Reject"}
                            </button>
                        </div>
                    </div>
                ))}
            </div>
        </section>
    );
}

function ZoneOverview({ zones }: { zones: ZoneSummary[] }) {
    return (
        <section className="rounded-lg border border-stone-300 bg-white p-4 shadow-sm">
            <div className="flex items-center justify-between gap-3">
                <h2 className="text-lg font-semibold text-slate-950">Bay availability</h2>
                <span className="rounded-md bg-slate-100 px-2 py-1 text-xs font-semibold text-slate-600">{zones.length}</span>
            </div>
            <div className="mt-4 space-y-3">
                {zones.length === 0 && <p className="py-8 text-sm text-slate-600">No bays match the current filters.</p>}
                {zones.map((zone) => (
                    <div key={`${zone.lotName}-${zone.zone}`} className="rounded-md border border-stone-200 p-3">
                        <div className="flex items-center justify-between gap-3">
                            <div>
                                <p className="text-sm font-semibold text-slate-950">{zone.lotName} - {zoneName(zone.zone)}</p>
                                <p className="text-xs text-slate-600">{zone.availableSpaces} free of {zone.totalSpaces}</p>
                            </div>
                            <p className="text-sm font-semibold text-slate-700">{percent(zone.occupancyRate)}</p>
                        </div>
                        <div className="mt-3 h-2 overflow-hidden rounded-md bg-stone-100">
                            <div
                                className={`h-full rounded-md ${zone.occupancyRate > 80 ? "bg-rose-500" : zone.occupancyRate > 55 ? "bg-amber-500" : "bg-emerald-500"}`}
                                style={{ width: `${Math.min(zone.occupancyRate, 100)}%` }}
                            />
                        </div>
                    </div>
                ))}
            </div>
        </section>
    );
}

function SpaceList({ spaces, isStaff }: { spaces: Space[]; isStaff: boolean }) {
    return (
        <section className="rounded-lg border border-stone-300 bg-white p-4 shadow-sm">
            <div className="flex items-center justify-between gap-3">
                <h2 className="text-lg font-semibold text-slate-950">{isStaff ? "Bay status" : "Available bays"}</h2>
                <span className="rounded-md bg-slate-100 px-2 py-1 text-xs font-semibold text-slate-600">{spaces.length}</span>
            </div>
            <div className="mt-4 grid gap-3 sm:grid-cols-2">
                {spaces.length === 0 && <p className="py-8 text-sm text-slate-600">No bays match the current filters.</p>}
                {spaces.map((space) => (
                    <article key={space.id} className="rounded-md border border-stone-200 p-3">
                        <div className="flex items-start justify-between gap-3">
                            <div className="min-w-0">
                                <p className="truncate text-sm font-semibold text-slate-950">{space.displayName}</p>
                                {isStaff && <p className="truncate text-xs text-slate-600">{space.sensorId}</p>}
                            </div>
                            <OccupancyPill occupied={space.occupied} />
                        </div>
                        <div className="mt-3 grid grid-cols-3 gap-2 text-xs text-slate-600">
                            <div>
                                <p className="font-semibold text-slate-900">{space.lotName}</p>
                                <p>Lot</p>
                            </div>
                            <div>
                                <p className="font-semibold text-slate-900">{bayTypeLabel(space.zone)}</p>
                                <p>Bay type</p>
                            </div>
                            <div>
                                <p className="font-semibold text-slate-900">{space.maxParkingMinutes}m</p>
                                <p>Limit</p>
                            </div>
                        </div>
                        <div className="mt-3 flex flex-wrap items-center gap-2 text-xs text-slate-500">
                            {space.disabilityPermitRequired && (
                                <span className="rounded-md bg-sky-50 px-2 py-1 font-semibold text-sky-800">Accessible bay</span>
                            )}
                            {isStaff && <span>{Math.round(space.confidence * 100)}% confidence</span>}
                            <span>{formatDateTime(space.lastUpdated)}</span>
                        </div>
                    </article>
                ))}
            </div>
        </section>
    );
}

function OccupancyPill({ occupied }: { occupied: boolean }) {
    return (
        <span className={`whitespace-nowrap rounded-md px-2 py-1 text-xs font-semibold ${occupied ? "bg-rose-50 text-rose-800" : "bg-emerald-50 text-emerald-800"}`}>
            {occupied ? "Occupied" : "Free"}
        </span>
    );
}

function StatusPill({ status }: { status: ParkingBooking["status"] }) {
    const tones = {
        ACTIVE: "bg-emerald-50 text-emerald-800",
        RESERVED: "bg-sky-50 text-sky-800",
        EXPIRED: "bg-stone-100 text-stone-700",
        CANCELLED: "bg-rose-50 text-rose-800",
        COMPLETED: "bg-violet-50 text-violet-800",
    };

    return (
        <span className={`whitespace-nowrap rounded-md px-2 py-1 text-xs font-semibold ${tones[status]}`}>
            {status.toLowerCase()}
        </span>
    );
}
