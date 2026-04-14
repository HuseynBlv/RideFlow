const STORAGE_VERSION = "2026-04-14-rideflow-demo-v1";

function clearPersistedDemoState() {
    [
        "rideflow.rider.token",
        "rideflow.rider.userId",
        "rideflow.rider.rideId",
        "rideflow.driver.token",
        "rideflow.driver.userId",
        "rideflow.driver.rideId"
    ].forEach((key) => localStorage.removeItem(key));
}

const savedVersion = localStorage.getItem("rideflow.storage.version");
if (savedVersion !== STORAGE_VERSION) {
    clearPersistedDemoState();
    localStorage.setItem("rideflow.storage.version", STORAGE_VERSION);
}

const state = {
    rider: {
        token: localStorage.getItem("rideflow.rider.token") || "",
        userId: localStorage.getItem("rideflow.rider.userId") || "",
        rideId: localStorage.getItem("rideflow.rider.rideId") || ""
    },
    driver: {
        token: localStorage.getItem("rideflow.driver.token") || "",
        userId: localStorage.getItem("rideflow.driver.userId") || "",
        rideId: localStorage.getItem("rideflow.driver.rideId") || ""
    }
};

const elements = {
    riderSessionBadge: document.querySelector("#rider-session-badge"),
    driverSessionBadge: document.querySelector("#driver-session-badge"),
    riderCurrentRide: document.querySelector("#rider-current-ride"),
    driverCurrentRide: document.querySelector("#driver-current-ride"),
    riderHistory: document.querySelector("#rider-history"),
    driverHistory: document.querySelector("#driver-history"),
    driverStatus: document.querySelector("#driver-status"),
    driverRideId: document.querySelector("#driver-ride-id"),
    eventLog: document.querySelector("#event-log"),
    flowRail: document.querySelector("#flow-rail"),
    flowStateLabel: document.querySelector("#flow-state-label"),
    heroPipeline: document.querySelector("#hero-pipeline"),
    pipelineStateLabel: document.querySelector("#pipeline-state-label")
};

const flowSteps = [
    { key: "accounts", title: "Accounts ready", detail: "Create a rider and a driver session." },
    { key: "location", title: "Driver location sent", detail: "Driver posts current coordinates." },
    { key: "online", title: "Driver online", detail: "Driver becomes available for matching." },
    { key: "requested", title: "Ride requested", detail: "Rider submits pickup and dropoff." },
    { key: "assigned", title: "Driver assigned", detail: "One driver accepts the offer." },
    { key: "progress", title: "Trip in progress", detail: "Driver starts the ride." },
    { key: "completed", title: "Trip completed", detail: "Fare is finalized and payment simulated." }
];

const pipelineSteps = [
    { key: "auth", tag: "Auth", title: "JWT sessions created", detail: "Rider and driver can call protected API routes." },
    { key: "geo", tag: "Geo", title: "Driver location indexed", detail: "Coordinates are stored and heartbeat freshness starts." },
    { key: "availability", tag: "Dispatch", title: "Driver marked available", detail: "The driver can now appear in nearby search." },
    { key: "ride", tag: "Ride", title: "Ride record created", detail: "Pickup, dropoff, and estimated fare are stored." },
    { key: "search", tag: "Matching", title: "Nearby drivers queried", detail: "The system filters by distance, freshness, and availability." },
    { key: "assign", tag: "Locking", title: "Assignment committed", detail: "One accept wins and the ride is locked to that driver." },
    { key: "trip", tag: "Realtime", title: "Trip status broadcast", detail: "Ride updates are pushed as the trip moves forward." },
    { key: "payment", tag: "Settlement", title: "Fare finalized", detail: "Final fare is computed and payment is marked captured." }
];

function rememberSession(role, authResponse) {
    const previousUserId = state[role].userId;
    state[role].token = authResponse.accessToken;
    state[role].userId = authResponse.userId;
    localStorage.setItem(`rideflow.${role}.token`, authResponse.accessToken);
    localStorage.setItem(`rideflow.${role}.userId`, authResponse.userId);
    if (previousUserId && previousUserId !== authResponse.userId) {
        clearRide(role);
    }
    renderSessionBadges();
}

function rememberRide(role, rideId) {
    state[role].rideId = rideId;
    localStorage.setItem(`rideflow.${role}.rideId`, rideId);
    if (role === "rider") {
        state.driver.rideId = state.driver.rideId || rideId;
        if (!localStorage.getItem("rideflow.driver.rideId")) {
            localStorage.setItem("rideflow.driver.rideId", rideId);
        }
    }
    elements.driverRideId.value = rideId || "";
}

function clearRide(role) {
    state[role].rideId = "";
    localStorage.removeItem(`rideflow.${role}.rideId`);
    if (role === "rider") {
        renderRide(elements.riderCurrentRide, null, "No active rider ride yet.");
    } else {
        renderRide(elements.driverCurrentRide, null, "Driver ride details will appear here.");
    }
    elements.driverRideId.value = state.driver.rideId || state.rider.rideId || "";
}

function clearSession(role, reason = "") {
    state[role].token = "";
    state[role].userId = "";
    localStorage.removeItem(`rideflow.${role}.token`);
    localStorage.removeItem(`rideflow.${role}.userId`);
    clearRide(role);
    if (role === "driver") {
        renderDriverStatus(null);
    }
    renderSessionBadges();
    if (reason) {
        logEvent(`${role} session cleared`, reason);
    }
}

function formatMoney(value) {
    if (value === null || value === undefined || value === "") {
        return "Pending";
    }
    return `$${Number(value).toFixed(2)}`;
}

function formatDate(value) {
    return value ? new Date(value).toLocaleString() : "Pending";
}

function logEvent(title, detail, tone = "neutral") {
    const empty = elements.eventLog.querySelector(".empty");
    if (empty) {
        empty.remove();
    }
    const item = document.createElement("div");
    item.className = "event";
    item.innerHTML = `
        <strong>${title}</strong>
        <span>${detail}</span>
        <span class="event-time">${new Date().toLocaleTimeString()}</span>
    `;
    if (tone === "error") {
        item.style.borderColor = "rgba(143, 49, 23, 0.25)";
        item.style.background = "rgba(194, 77, 44, 0.08)";
    }
    elements.eventLog.prepend(item);
}

async function api(path, options = {}, token = "") {
    const response = await fetch(path, {
        ...options,
        headers: {
            "Content-Type": "application/json",
            ...(token ? { Authorization: `Bearer ${token}` } : {}),
            ...(options.headers || {})
        }
    });

    let payload = null;
    const text = await response.text();
    if (text) {
        try {
            payload = JSON.parse(text);
        } catch {
            payload = { raw: text };
        }
    }

    if (!response.ok) {
        const message = payload?.message || payload?.error || payload?.raw || `HTTP ${response.status}`;
        const error = new Error(message);
        error.status = response.status;
        error.payload = payload;
        throw error;
    }

    return payload;
}

function isSessionError(error) {
    const message = (error.message || "").toLowerCase();
    const details = Array.isArray(error.payload?.details)
        ? error.payload.details.join(" ").toLowerCase()
        : "";
    return error.status === 401
        || error.status === 403
        || message.includes("user not found")
        || details.includes("user not found");
}

function isStaleRideError(error) {
    return error.status === 404 || error.status === 409;
}

function renderSessionBadges() {
    elements.riderSessionBadge.textContent = state.rider.token ? "Rider ready" : "Not logged in";
    elements.driverSessionBadge.textContent = state.driver.token ? "Driver ready" : "Not logged in";
    elements.driverRideId.value = state.driver.rideId || state.rider.rideId || "";
}

function deriveFlowState() {
    const rideStatus = state.currentRideStatus || "";
    const driverStatus = state.currentDriverStatus || "";
    const hasAccounts = Boolean(state.rider.token && state.driver.token);
    const hasLocation = Boolean(state.driver.hasLocation);
    const isOnline = driverStatus === "AVAILABLE" || driverStatus === "BUSY";

    if (rideStatus === "COMPLETED") {
        return "completed";
    }
    if (rideStatus === "IN_PROGRESS") {
        return "progress";
    }
    if (rideStatus === "DRIVER_ASSIGNED" || rideStatus === "DRIVER_ARRIVING") {
        return "assigned";
    }
    if (rideStatus === "MATCHING" || rideStatus === "REQUESTED") {
        return "requested";
    }
    if (isOnline) {
        return "online";
    }
    if (hasLocation) {
        return "location";
    }
    if (hasAccounts) {
        return "accounts";
    }
    return "idle";
}

function derivePipelineState() {
    const rideStatus = state.currentRideStatus || "";
    const driverStatus = state.currentDriverStatus || "";
    const hasAccounts = Boolean(state.rider.token && state.driver.token);
    const hasLocation = Boolean(state.driver.hasLocation);
    const isOnline = driverStatus === "AVAILABLE" || driverStatus === "BUSY";

    if (rideStatus === "COMPLETED") {
        return "payment";
    }
    if (rideStatus === "IN_PROGRESS") {
        return "trip";
    }
    if (rideStatus === "DRIVER_ASSIGNED" || rideStatus === "DRIVER_ARRIVING") {
        return "assign";
    }
    if (rideStatus === "MATCHING" || rideStatus === "REQUESTED") {
        return "search";
    }
    if (state.rider.rideId) {
        return "ride";
    }
    if (isOnline) {
        return "availability";
    }
    if (hasLocation) {
        return "geo";
    }
    if (hasAccounts) {
        return "auth";
    }
    return "idle";
}

function renderFlowState() {
    const current = deriveFlowState();
    const currentIndex = flowSteps.findIndex((step) => step.key === current);
    elements.flowStateLabel.textContent = current === "idle"
        ? "Waiting to start"
        : flowSteps[currentIndex].title;

    elements.flowRail.innerHTML = flowSteps.map((step, index) => {
        let tone = "pending";
        if (currentIndex >= 0 && index < currentIndex) {
            tone = "done";
        } else if (currentIndex === index) {
            tone = "active";
        }
        return `
            <article class="flow-step ${tone}">
                <div class="flow-step-index">${index + 1}</div>
                <div>
                    <strong>${step.title}</strong>
                    <span>${step.detail}</span>
                </div>
            </article>
        `;
    }).join("");

    const pipelineCurrent = derivePipelineState();
    const pipelineIndex = pipelineSteps.findIndex((step) => step.key === pipelineCurrent);
    elements.pipelineStateLabel.textContent = pipelineCurrent === "idle"
        ? "System idle"
        : pipelineSteps[pipelineIndex].tag;

    elements.heroPipeline.innerHTML = pipelineSteps.map((step, index) => {
        let tone = "pending";
        if (pipelineIndex >= 0 && index < pipelineIndex) {
            tone = "done";
        } else if (pipelineIndex === index) {
            tone = "active";
        }
        return `
            <article class="pipeline-step ${tone}">
                <div class="pipeline-step-head">
                    <strong>${step.title}</strong>
                    <span class="pipeline-step-tag">${step.tag}</span>
                </div>
                <p>${step.detail}</p>
            </article>
        `;
    }).join("");
}

function statusClass(status) {
    return (status || "unknown").toLowerCase();
}

function renderRide(target, ride, emptyText) {
    if (!ride) {
        target.innerHTML = `<div class="empty-state">${emptyText}</div>`;
        if (target === elements.riderCurrentRide || target === elements.driverCurrentRide) {
            if (!state.rider.rideId && !state.driver.rideId) {
                state.currentRideStatus = "";
            }
            renderFlowState();
        }
        return;
    }
    state.currentRideStatus = ride.status;
    const candidateLine = Array.isArray(ride.candidateDriverIds) && ride.candidateDriverIds.length
        ? `<div><strong>Candidates</strong>${ride.candidateDriverIds.join(", ")}</div>`
        : "";
    target.innerHTML = `
        <article class="ride-card">
            <div class="panel-heading">
                <div>
                    <div class="status-pill ${statusClass(ride.status)}">${ride.status.replaceAll("_", " ")}</div>
                    <p><strong>Ride ID</strong><br>${ride.rideId}</p>
                </div>
                <button class="button tertiary" type="button" data-copy-ride-id="${ride.rideId}">Copy Ride ID</button>
            </div>
            <div class="ride-meta">
                <div><strong>Estimated Fare</strong>${formatMoney(ride.estimatedFare)}</div>
                <div><strong>Final Fare</strong>${formatMoney(ride.finalFare)}</div>
                <div><strong>Pickup</strong>${ride.pickupLatitude}, ${ride.pickupLongitude}</div>
                <div><strong>Dropoff</strong>${ride.dropoffLatitude}, ${ride.dropoffLongitude}</div>
                <div><strong>Requested</strong>${formatDate(ride.requestedAt)}</div>
                <div><strong>Started</strong>${formatDate(ride.startedAt)}</div>
                ${candidateLine}
            </div>
        </article>
    `;
    renderFlowState();
}

function renderHistory(target, rides, emptyText) {
    if (!rides || rides.length === 0) {
        target.innerHTML = `<div class="empty-state">${emptyText}</div>`;
        return;
    }

    target.innerHTML = rides.map((ride) => `
        <article class="history-card">
            <div class="panel-heading">
                <strong>${ride.rideId}</strong>
                <span class="status-pill ${statusClass(ride.status)}">${ride.status.replaceAll("_", " ")}</span>
            </div>
            <div class="history-meta">
                <div><strong>Estimated</strong>${formatMoney(ride.estimatedFare)}</div>
                <div><strong>Final</strong>${formatMoney(ride.finalFare)}</div>
                <div><strong>Requested</strong>${formatDate(ride.requestedAt)}</div>
                <div><strong>Ended</strong>${formatDate(ride.endedAt)}</div>
            </div>
        </article>
    `).join("");
}

function renderDriverStatus(status) {
    if (!status) {
        elements.driverStatus.innerHTML = `<div class="empty-state">Driver status will appear here.</div>`;
        state.currentDriverStatus = "";
        state.driver.hasLocation = false;
        renderFlowState();
        return;
    }
    state.currentDriverStatus = status.status;
    state.driver.hasLocation = Boolean(status.latitude !== null && status.longitude !== null);
    elements.driverStatus.innerHTML = `
        <article class="status-card">
            <div class="panel-heading">
                <strong>${status.driverId}</strong>
                <span class="status-pill ${statusClass(status.status)}">${status.status}</span>
            </div>
            <div class="ride-meta">
                <div><strong>Latitude</strong>${status.latitude ?? "Missing"}</div>
                <div><strong>Longitude</strong>${status.longitude ?? "Missing"}</div>
                <div><strong>Updated</strong>${formatDate(status.lastLocationAt)}</div>
            </div>
        </article>
    `;
    renderFlowState();
}

async function refreshRide(role) {
    const token = state[role].token;
    const rideId = state[role].rideId;
    if (!token || !rideId) {
        renderRide(role === "rider" ? elements.riderCurrentRide : elements.driverCurrentRide, null, `No active ${role} ride yet.`);
        return;
    }
    try {
        const ride = await api(`/rides/${rideId}`, { method: "GET" }, token);
        renderRide(role === "rider" ? elements.riderCurrentRide : elements.driverCurrentRide, ride, `No active ${role} ride yet.`);
        rememberRide(role, ride.rideId);
    } catch (error) {
        if (isSessionError(error)) {
            clearSession(role, "Saved session expired. Please login again.");
            return;
        }
        if (isStaleRideError(error)) {
            clearRide(role);
            logEvent(`${role} ride cleared`, "Saved ride reference was stale and has been removed.");
            return;
        }
        logEvent(`${role} ride refresh failed`, error.message, "error");
    }
}

async function refreshDriverStatus() {
    if (!state.driver.token) {
        renderDriverStatus(null);
        return;
    }
    try {
        const status = await api("/drivers/me/status", { method: "GET" }, state.driver.token);
        renderDriverStatus(status);
    } catch (error) {
        if (isSessionError(error)) {
            clearSession("driver", "Saved driver session expired. Please login again.");
            return;
        }
        logEvent("driver status failed", error.message, "error");
    }
}

async function refreshHistory(role) {
    const token = state[role].token;
    if (!token) {
        return;
    }
    const path = role === "rider" ? "/riders/me/rides" : "/drivers/me/rides";
    const target = role === "rider" ? elements.riderHistory : elements.driverHistory;
    try {
        const rides = await api(path, { method: "GET" }, token);
        renderHistory(target, rides, `${role} history will appear here.`);
    } catch (error) {
        if (isSessionError(error)) {
            clearSession(role, "Saved session expired. Please login again.");
            return;
        }
        logEvent(`${role} history failed`, error.message, "error");
    }
}

async function register(role, payload) {
    const body = role === "rider"
        ? { ...payload, role: "RIDER" }
        : { ...payload, role: "DRIVER" };
    const response = await api("/auth/register", {
        method: "POST",
        body: JSON.stringify(body)
    });
    clearRide(role);
    rememberSession(role, response);
    logEvent(`${role} registered`, `${payload.name} is ready to use the demo.`);
}

async function login(role, payload) {
    const response = await api("/auth/login", {
        method: "POST",
        body: JSON.stringify(payload)
    });
    clearRide(role);
    rememberSession(role, response);
    logEvent(`${role} logged in`, `${payload.phone} authenticated successfully.`);
}

async function requestRide(payload) {
    const ride = await api("/rides/request", {
        method: "POST",
        body: JSON.stringify(payload)
    }, state.rider.token);
    rememberRide("rider", ride.rideId);
    renderRide(elements.riderCurrentRide, ride, "No active rider ride yet.");
    logEvent("ride requested", `Ride ${ride.rideId} entered ${ride.status}.`);
    await refreshHistory("rider");
}

async function updateDriverLocation(payload) {
    const status = await api("/drivers/me/location", {
        method: "POST",
        body: JSON.stringify(payload)
    }, state.driver.token);
    renderDriverStatus(status);
    logEvent("driver location updated", `${status.latitude}, ${status.longitude}`);
}

async function driverTransition(action) {
    const rideId = elements.driverRideId.value.trim() || state.driver.rideId || state.rider.rideId;
    if (!rideId) {
        throw new Error("Ride ID is required");
    }
    const ride = await api(`/rides/${rideId}/${action}`, {
        method: "POST"
    }, state.driver.token);
    rememberRide("driver", ride.rideId);
    rememberRide("rider", state.rider.rideId || ride.rideId);
    renderRide(elements.driverCurrentRide, ride, "Driver ride details will appear here.");
    await refreshRide("rider");
    await refreshDriverStatus();
    await refreshHistory("driver");
    logEvent(`driver ${action}`, `Ride ${ride.rideId} is now ${ride.status}.`);
}

async function changeDriverAvailability(path, label) {
    const status = await api(path, { method: "POST" }, state.driver.token);
    renderDriverStatus(status);
    logEvent(label, `Driver moved to ${status.status}.`);
}

function readForm(form) {
    const data = new FormData(form);
    return Object.fromEntries(data.entries());
}

function bindForms() {
    document.querySelector("#rider-register-form").addEventListener("submit", async (event) => {
        event.preventDefault();
        const form = event.currentTarget;
        try {
            await register("rider", readForm(form));
            form.reset();
            renderSessionBadges();
        } catch (error) {
            logEvent("rider register failed", error.message, "error");
        }
    });

    document.querySelector("#driver-register-form").addEventListener("submit", async (event) => {
        event.preventDefault();
        const form = event.currentTarget;
        try {
            await register("driver", readForm(form));
            form.reset();
            renderSessionBadges();
        } catch (error) {
            logEvent("driver register failed", error.message, "error");
        }
    });

    document.querySelector("#rider-login-form").addEventListener("submit", async (event) => {
        event.preventDefault();
        const form = event.currentTarget;
        try {
            await login("rider", readForm(form));
            renderSessionBadges();
            await refreshHistory("rider");
        } catch (error) {
            logEvent("rider login failed", error.message, "error");
        }
    });

    document.querySelector("#driver-login-form").addEventListener("submit", async (event) => {
        event.preventDefault();
        const form = event.currentTarget;
        try {
            await login("driver", readForm(form));
            renderSessionBadges();
            await refreshDriverStatus();
            await refreshHistory("driver");
        } catch (error) {
            logEvent("driver login failed", error.message, "error");
        }
    });

    document.querySelector("#ride-request-form").addEventListener("submit", async (event) => {
        event.preventDefault();
        try {
            const payload = readForm(event.currentTarget);
            await requestRide({
                pickupLatitude: Number(payload.pickupLatitude),
                pickupLongitude: Number(payload.pickupLongitude),
                dropoffLatitude: Number(payload.dropoffLatitude),
                dropoffLongitude: Number(payload.dropoffLongitude)
            });
        } catch (error) {
            logEvent("ride request failed", error.message, "error");
        }
    });

    document.querySelector("#driver-location-form").addEventListener("submit", async (event) => {
        event.preventDefault();
        try {
            const payload = readForm(event.currentTarget);
            await updateDriverLocation({
                latitude: Number(payload.latitude),
                longitude: Number(payload.longitude)
            });
        } catch (error) {
            logEvent("location update failed", error.message, "error");
        }
    });
}

function bindButtons() {
    document.querySelector("#rider-history-button").addEventListener("click", () => refreshHistory("rider"));
    document.querySelector("#driver-history-button").addEventListener("click", () => refreshHistory("driver"));
    document.querySelector("#rider-refresh-ride").addEventListener("click", () => refreshRide("rider"));
    document.querySelector("#driver-status-button").addEventListener("click", refreshDriverStatus);
    document.querySelector("#driver-go-online").addEventListener("click", async () => {
        try {
            await changeDriverAvailability("/drivers/me/online", "driver online");
        } catch (error) {
            logEvent("driver online failed", error.message, "error");
        }
    });
    document.querySelector("#driver-go-offline").addEventListener("click", async () => {
        try {
            await changeDriverAvailability("/drivers/me/offline", "driver offline");
        } catch (error) {
            logEvent("driver offline failed", error.message, "error");
        }
    });
    document.querySelectorAll("[data-driver-action]").forEach((button) => {
        button.addEventListener("click", async () => {
            try {
                await driverTransition(button.dataset.driverAction);
            } catch (error) {
                logEvent(`driver ${button.dataset.driverAction} failed`, error.message, "error");
            }
        });
    });
    document.querySelector("#clear-events").addEventListener("click", () => {
        elements.eventLog.innerHTML = `<div class="event empty">No events yet. Start with rider or driver registration.</div>`;
    });
    document.querySelector("#reset-sessions").addEventListener("click", () => {
        clearSession("rider");
        clearSession("driver");
        elements.riderHistory.innerHTML = `<div class="empty-state">History will appear here.</div>`;
        elements.driverHistory.innerHTML = `<div class="empty-state">History will appear here.</div>`;
        logEvent("demo state reset", "Saved sessions, ride IDs, and active views were cleared.");
    });

    document.addEventListener("click", async (event) => {
        const rideCopy = event.target.closest("[data-copy-ride-id]");
        if (!rideCopy) {
            return;
        }
        await navigator.clipboard.writeText(rideCopy.dataset.copyRideId);
        logEvent("ride id copied", rideCopy.dataset.copyRideId);
    });
}

function startPolling() {
    setInterval(() => {
        refreshRide("rider");
        refreshRide("driver");
        refreshDriverStatus();
    }, 4000);
}

async function boot() {
    renderFlowState();
    renderSessionBadges();
    bindForms();
    bindButtons();
    await refreshRide("rider");
    await refreshRide("driver");
    await refreshDriverStatus();
    await refreshHistory("rider");
    await refreshHistory("driver");
    startPolling();
    logEvent("demo loaded", "RideFlow demo console is ready.");
}

boot();
