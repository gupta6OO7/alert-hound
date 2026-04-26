const state = {
    incidents: [],
    selectedIncidentId: null
};

const elements = {
    metricTotal: document.getElementById("metric-total"),
    metricInvestigated: document.getElementById("metric-investigated"),
    metricResolved: document.getElementById("metric-resolved"),
    serviceFilter: document.getElementById("service-filter"),
    refreshButton: document.getElementById("refresh-button"),
    lastSync: document.getElementById("last-sync"),
    listState: document.getElementById("list-state"),
    incidentList: document.getElementById("incident-list"),
    detailEmpty: document.getElementById("detail-empty"),
    detailContent: document.getElementById("detail-content"),
    detailStatus: document.getElementById("detail-status"),
    detailId: document.getElementById("detail-id"),
    detailService: document.getElementById("detail-service"),
    detailSeverity: document.getElementById("detail-severity"),
    detailErrorRate: document.getElementById("detail-error-rate"),
    detailUpdatedAt: document.getElementById("detail-updated-at"),
    detailLastDetected: document.getElementById("detail-last-detected"),
    detailSummary: document.getElementById("detail-summary"),
    detailRootCause: document.getElementById("detail-root-cause"),
    detailRecommendations: document.getElementById("detail-recommendations")
};

elements.refreshButton.addEventListener("click", () => loadIncidents());
elements.serviceFilter.addEventListener("keydown", (event) => {
    if (event.key === "Enter") {
        loadIncidents();
    }
});

loadIncidents();

async function loadIncidents() {
    const service = elements.serviceFilter.value.trim();
    const query = service ? `?service=${encodeURIComponent(service)}` : "";
    elements.listState.textContent = "Loading projected incidents...";
    elements.listState.hidden = false;
    elements.incidentList.innerHTML = "";

    try {
        const response = await fetch(`/consumer/incidents${query}`);
        if (!response.ok) {
            throw new Error(`Request failed with status ${response.status}`);
        }

        state.incidents = await response.json();
        if (state.selectedIncidentId && !state.incidents.some((incident) => incident.incidentId === state.selectedIncidentId)) {
            state.selectedIncidentId = null;
        }
        renderMetrics();
        renderList();
        renderDetail();
        elements.lastSync.textContent = `Synced ${new Date().toLocaleTimeString()}`;
    } catch (error) {
        elements.listState.textContent = `Failed to load projection: ${error.message}`;
        renderMetrics();
        clearDetail();
    }
}

function renderMetrics() {
    elements.metricTotal.textContent = String(state.incidents.length);
    elements.metricInvestigated.textContent = String(state.incidents.filter((incident) => incident.status === "INVESTIGATED").length);
    elements.metricResolved.textContent = String(state.incidents.filter((incident) => incident.status === "RESOLVED").length);
}

function renderList() {
    if (state.incidents.length === 0) {
        elements.listState.textContent = "No projected incidents found for the current filter.";
        elements.listState.hidden = false;
        elements.incidentList.innerHTML = "";
        return;
    }

    elements.listState.hidden = true;
    elements.incidentList.innerHTML = state.incidents.map((incident) => `
        <article class="incident-card ${incident.incidentId === state.selectedIncidentId ? "active" : ""}" data-incident-id="${incident.incidentId}">
            <div class="incident-card-head">
                <h3>${escapeHtml(incident.service)}</h3>
                <span class="badge ${badgeClass(incident.status)}">${escapeHtml(incident.status)}</span>
            </div>
            <div class="incident-meta">
                <span><strong>Severity:</strong> ${escapeHtml(incident.severity)}</span>
                <span><strong>Error Rate:</strong> ${formatPercent(incident.errorRate)}</span>
                <span><strong>Updated:</strong> ${formatDateTime(incident.updatedAt)}</span>
            </div>
        </article>
    `).join("");

    document.querySelectorAll(".incident-card").forEach((card) => {
        card.addEventListener("click", () => {
            state.selectedIncidentId = card.dataset.incidentId;
            renderList();
            renderDetail();
        });
    });

    if (!state.selectedIncidentId && state.incidents.length > 0) {
        state.selectedIncidentId = state.incidents[0].incidentId;
        renderList();
        renderDetail();
    }
}

function renderDetail() {
    const incident = state.incidents.find((candidate) => candidate.incidentId === state.selectedIncidentId);
    if (!incident) {
        clearDetail();
        return;
    }

    elements.detailEmpty.classList.add("hidden");
    elements.detailContent.classList.remove("hidden");
    elements.detailStatus.className = `badge ${badgeClass(incident.status)}`;
    elements.detailStatus.textContent = incident.status;
    elements.detailId.textContent = incident.incidentId;
    elements.detailService.textContent = incident.service;
    elements.detailSeverity.textContent = incident.severity;
    elements.detailErrorRate.textContent = formatPercent(incident.errorRate);
    elements.detailUpdatedAt.textContent = formatDateTime(incident.updatedAt);
    elements.detailLastDetected.textContent = formatDateTime(incident.lastDetectedAt);
    elements.detailSummary.textContent = incident.summary || "No summary available.";
    elements.detailRootCause.textContent = incident.rootCause || "No root cause available.";
    elements.detailRecommendations.innerHTML = (incident.recommendations || [])
        .map((recommendation) => `<li>${escapeHtml(recommendation)}</li>`)
        .join("") || "<li>No recommendations available.</li>";
}

function clearDetail() {
    elements.detailEmpty.classList.remove("hidden");
    elements.detailContent.classList.add("hidden");
    elements.detailStatus.className = "badge neutral";
    elements.detailStatus.textContent = "No selection";
}

function badgeClass(status) {
    return (status || "").toLowerCase() || "neutral";
}

function formatPercent(value) {
    return `${(Number(value || 0) * 100).toFixed(2)}%`;
}

function formatDateTime(value) {
    if (!value) {
        return "N/A";
    }
    return new Date(value).toLocaleString();
}

function escapeHtml(value) {
    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll("\"", "&quot;")
        .replaceAll("'", "&#39;");
}
