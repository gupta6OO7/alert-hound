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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IncidentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(IncidentService.class);

    private static final Set<IncidentStatus> OPEN_STATUSES = Set.of(
            IncidentStatus.ACTIVE,
            IncidentStatus.INVESTIGATING,
            IncidentStatus.INVESTIGATED
    );

    private final IncidentRepository incidentRepository;
    private final ActiveIncidentCacheRepository activeIncidentCacheRepository;
    private final AgentTaskProducer agentTaskProducer;
    private final IncidentUpdateProducer incidentUpdateProducer;

    public IncidentService(
            IncidentRepository incidentRepository,
            ActiveIncidentCacheRepository activeIncidentCacheRepository,
            AgentTaskProducer agentTaskProducer,
            IncidentUpdateProducer incidentUpdateProducer
    ) {
        this.incidentRepository = incidentRepository;
        this.activeIncidentCacheRepository = activeIncidentCacheRepository;
        this.agentTaskProducer = agentTaskProducer;
        this.incidentUpdateProducer = incidentUpdateProducer;
    }

    @Transactional
    public Incident handleIncidentCreated(IncidentCreatedEvent event) {
        LOGGER.info(
                "Handling incident event incidentId={} service={} severity={} errorRate={}",
                event.incidentId(),
                event.service(),
                event.severity(),
                event.errorRate()
        );
        Incident activeIncident = incidentRepository
                .findFirstByServiceAndStatusInOrderByCreatedAtDesc(event.service(), OPEN_STATUSES)
                .orElse(null);

        if (activeIncident != null) {
            LOGGER.info(
                    "Refreshing existing incident incidentId={} service={} previousStatus={}",
                    activeIncident.getId(),
                    activeIncident.getService(),
                    activeIncident.getStatus()
            );
            refreshExistingIncident(activeIncident, event);
            Incident savedIncident = incidentRepository.save(activeIncident);
            if (shouldTriggerAgentOnRefresh(savedIncident)) {
                LOGGER.info(
                        "Triggering agent for refreshed incident incidentId={} service={} status={} summaryPresent={} rootCausePresent={}",
                        savedIncident.getId(),
                        savedIncident.getService(),
                        savedIncident.getStatus(),
                        StringUtils.hasText(savedIncident.getSummary()),
                        StringUtils.hasText(savedIncident.getRootCause())
                );
                agentTaskProducer.trigger(savedIncident);
            } else {
                LOGGER.info(
                        "Skipping agent trigger for refreshed incident incidentId={} service={} status={} summaryPresent={} rootCausePresent={}",
                        savedIncident.getId(),
                        savedIncident.getService(),
                        savedIncident.getStatus(),
                        StringUtils.hasText(savedIncident.getSummary()),
                        StringUtils.hasText(savedIncident.getRootCause())
                );
            }
            LOGGER.info(
                    "Refreshed incident incidentId={} service={} severity={} errorRate={}",
                    savedIncident.getId(),
                    savedIncident.getService(),
                    savedIncident.getSeverity(),
                    savedIncident.getErrorRate()
            );
            return savedIncident;
        }

        Incident incident = new Incident();
        incident.setId(UUID.fromString(event.incidentId()));
        incident.setService(event.service());
        incident.setStatus(IncidentStatus.INVESTIGATING);
        incident.setSeverity(event.severity());
        incident.setErrorRate(event.errorRate());
        incident.setStartTime(event.windowStart());
        incident.setLastDetectedAt(event.triggeredAt());

        Incident savedIncident = incidentRepository.save(incident);
        activeIncidentCacheRepository.save(toActiveState(savedIncident));
        agentTaskProducer.trigger(savedIncident);
        LOGGER.info(
                "Created incident incidentId={} service={} status={} severity={}",
                savedIncident.getId(),
                savedIncident.getService(),
                savedIncident.getStatus(),
                savedIncident.getSeverity()
        );
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
        incidentUpdateProducer.publish(savedIncident);
        LOGGER.info("Resolved incident incidentId={} service={}", savedIncident.getId(), savedIncident.getService());
        return savedIncident;
    }

    private void refreshExistingIncident(Incident incident, IncidentCreatedEvent event) {
        incident.setErrorRate(Math.max(incident.getErrorRate(), event.errorRate()));
        incident.setSeverity(higherSeverity(incident.getSeverity(), event.severity()));
        incident.setLastDetectedAt(event.triggeredAt());
        if (incident.getStatus() == IncidentStatus.ACTIVE) {
            incident.setStatus(IncidentStatus.INVESTIGATING);
        }
        activeIncidentCacheRepository.save(toActiveState(incident));
    }

    private boolean shouldTriggerAgentOnRefresh(Incident incident) {
        return incident.getStatus() == IncidentStatus.INVESTIGATING
                || !StringUtils.hasText(incident.getSummary())
                || !StringUtils.hasText(incident.getRootCause());
    }

    private ActiveIncidentState toActiveState(Incident incident) {
        return new ActiveIncidentState(
                incident.getId(),
                incident.getService(),
                incident.getStatus(),
                incident.getSeverity(),
                incident.getErrorRate(),
                incident.getStartTime(),
                incident.getLastDetectedAt(),
                incident.getSummary(),
                incident.getRootCause(),
                List.of()
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
