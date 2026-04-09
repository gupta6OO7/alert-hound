package com.incident.alerthound.incident.service;

import com.incident.alerthound.detection.model.IncidentCreatedEvent;
import com.incident.alerthound.incident.model.ActiveIncidentState;
import com.incident.alerthound.incident.model.Incident;
import com.incident.alerthound.incident.model.IncidentStatus;
import com.incident.alerthound.incident.repository.IncidentRepository;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IncidentService {

    private static final Set<IncidentStatus> OPEN_STATUSES = Set.of(IncidentStatus.ACTIVE, IncidentStatus.INVESTIGATING);

    private final IncidentRepository incidentRepository;
    private final ActiveIncidentCacheRepository activeIncidentCacheRepository;
    private final AgentTaskProducer agentTaskProducer;

    public IncidentService(
            IncidentRepository incidentRepository,
            ActiveIncidentCacheRepository activeIncidentCacheRepository,
            AgentTaskProducer agentTaskProducer
    ) {
        this.incidentRepository = incidentRepository;
        this.activeIncidentCacheRepository = activeIncidentCacheRepository;
        this.agentTaskProducer = agentTaskProducer;
    }

    @Transactional
    public Incident handleIncidentCreated(IncidentCreatedEvent event) {
        Incident activeIncident = incidentRepository
                .findFirstByServiceAndStatusInOrderByCreatedAtDesc(event.service(), OPEN_STATUSES)
                .orElse(null);

        if (activeIncident != null) {
            refreshExistingIncident(activeIncident, event);
            return incidentRepository.save(activeIncident);
        }

        Incident incident = new Incident();
        incident.setId(UUID.fromString(event.incidentId()));
        incident.setService(event.service());
        incident.setStatus(IncidentStatus.ACTIVE);
        incident.setSeverity(event.severity());
        incident.setErrorRate(event.errorRate());
        incident.setStartTime(event.windowStart());
        incident.setLastDetectedAt(event.triggeredAt());

        Incident savedIncident = incidentRepository.save(incident);
        activeIncidentCacheRepository.save(toActiveState(savedIncident));
        agentTaskProducer.trigger(savedIncident);
        return savedIncident;
    }

    @Transactional(readOnly = true)
    public List<Incident> getActiveIncidents() {
        return incidentRepository.findByStatusInOrderByCreatedAtDesc(OPEN_STATUSES);
    }

    @Transactional(readOnly = true)
    public Incident getIncident(UUID incidentId) {
        return incidentRepository.findById(incidentId).orElseThrow(() -> new IncidentNotFoundException(incidentId));
    }

    @Transactional
    public Incident resolveIncident(UUID incidentId) {
        Incident incident = getIncident(incidentId);
        incident.setStatus(IncidentStatus.RESOLVED);
        incident.setEndTime(Instant.now());

        Incident savedIncident = incidentRepository.save(incident);
        activeIncidentCacheRepository.delete(savedIncident.getService());
        return savedIncident;
    }

    private void refreshExistingIncident(Incident incident, IncidentCreatedEvent event) {
        incident.setErrorRate(Math.max(incident.getErrorRate(), event.errorRate()));
        incident.setSeverity(higherSeverity(incident.getSeverity(), event.severity()));
        incident.setLastDetectedAt(event.triggeredAt());
        activeIncidentCacheRepository.save(toActiveState(incident));
    }

    private ActiveIncidentState toActiveState(Incident incident) {
        return new ActiveIncidentState(
                incident.getId(),
                incident.getService(),
                incident.getStatus(),
                incident.getSeverity(),
                incident.getErrorRate(),
                incident.getStartTime(),
                incident.getLastDetectedAt()
        );
    }

    private String higherSeverity(String left, String right) {
        return severityRank(right) > severityRank(left) ? right : left;
    }

    private int severityRank(String severity) {
        if (severity == null) {
            return 0;
        }
        return switch (severity) {
            case "CRITICAL" -> 3;
            case "HIGH" -> 2;
            case "MEDIUM" -> 1;
            default -> 0;
        };
    }
}
