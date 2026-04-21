package com.incident.alerthound.incident.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.incident.alerthound.agent.model.AgentResultEvent;
import com.incident.alerthound.incident.api.IncidentResponse;
import com.incident.alerthound.incident.model.ActiveIncidentState;
import com.incident.alerthound.incident.model.Incident;
import com.incident.alerthound.incident.model.IncidentStatus;
import com.incident.alerthound.incident.model.IncidentUpdatedEvent;
import com.incident.alerthound.incident.repository.IncidentRepository;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IncidentEnrichmentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(IncidentEnrichmentService.class);

    private final IncidentRepository incidentRepository;
    private final ActiveIncidentCacheRepository activeIncidentCacheRepository;
    private final IncidentUpdateProducer incidentUpdateProducer;
    private final ObjectMapper objectMapper;

    public IncidentEnrichmentService(
            IncidentRepository incidentRepository,
            ActiveIncidentCacheRepository activeIncidentCacheRepository,
            IncidentUpdateProducer incidentUpdateProducer,
            ObjectMapper objectMapper
    ) {
        this.incidentRepository = incidentRepository;
        this.activeIncidentCacheRepository = activeIncidentCacheRepository;
        this.incidentUpdateProducer = incidentUpdateProducer;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public IncidentEnrichmentReport process(AgentResultEvent result) {
        return enrich(result, true);
    }

    @Transactional
    public IncidentEnrichmentReport inspect(AgentResultEvent result) {
        return enrich(result, false);
    }

    private IncidentEnrichmentReport enrich(AgentResultEvent result, boolean publishUpdate) {
        if (result == null || result.incidentId() == null) {
            LOGGER.warn("Ignoring agent result because incidentId is missing");
            return IncidentEnrichmentReport.ignored("agent result payload is missing incidentId");
        }

        LOGGER.info(
                "Applying agent result incidentId={} service={} publishUpdate={} usedFallback={} iterations={}",
                result.incidentId(),
                result.service(),
                publishUpdate,
                result.usedFallback(),
                result.iterations()
        );

        Optional<Incident> optionalIncident = incidentRepository.findById(result.incidentId());
        if (optionalIncident.isEmpty()) {
            LOGGER.warn("Ignoring agent result for unknown incident {}", result.incidentId());
            return IncidentEnrichmentReport.ignored("incident not found");
        }

        Incident incident = optionalIncident.get();
        if (incident.getStatus() == IncidentStatus.RESOLVED) {
            LOGGER.info("Ignoring stale agent result for resolved incident {}", incident.getId());
            return IncidentEnrichmentReport.ignored("incident already resolved", IncidentResponse.from(incident), activeIncidentCacheRepository.get(incident.getService()), null);
        }

        String recommendationsJson = serializeRecommendations(result.recommendations());
        boolean duplicate = incident.getStatus() == IncidentStatus.INVESTIGATED
                && Objects.equals(trimToNull(incident.getSummary()), trimToNull(result.summary()))
                && Objects.equals(trimToNull(incident.getRootCause()), trimToNull(result.rootCause()))
                && Objects.equals(trimToNull(incident.getRecommendationsJson()), trimToNull(recommendationsJson));
        if (duplicate) {
            LOGGER.info("Ignoring duplicate agent result for incident {}", incident.getId());
            return IncidentEnrichmentReport.ignored("duplicate agent result", IncidentResponse.from(incident), activeIncidentCacheRepository.get(incident.getService()), null);
        }

        incident.setSummary(trimToNull(result.summary()));
        incident.setRootCause(trimToNull(result.rootCause()));
        incident.setRecommendationsJson(recommendationsJson);
        incident.setStatus(IncidentStatus.INVESTIGATED);

        Incident savedIncident = incidentRepository.save(incident);
        ActiveIncidentState activeState = toActiveState(savedIncident, result.recommendations());
        activeIncidentCacheRepository.save(activeState);

        IncidentUpdatedEvent updateEvent = publishUpdate ? incidentUpdateProducer.publish(savedIncident) : incidentUpdateProducer.toEvent(savedIncident);
        LOGGER.info(
                "Incident enrichment applied incidentId={} status={} summaryPresent={} rootCausePresent={} recommendations={}",
                savedIncident.getId(),
                savedIncident.getStatus(),
                savedIncident.getSummary() != null,
                savedIncident.getRootCause() != null,
                result.recommendations() == null ? 0 : result.recommendations().size()
        );
        return IncidentEnrichmentReport.updated(IncidentResponse.from(savedIncident), activeState, updateEvent, result.usedFallback());
    }

    private ActiveIncidentState toActiveState(Incident incident, List<String> recommendations) {
        return new ActiveIncidentState(
                incident.getId(),
                incident.getService(),
                incident.getStatus(),
                incident.getSeverity(),
                incident.getErrorRate(),
                incident.getStartTime(),
                incident.getUpdatedAt(),
                incident.getSummary(),
                incident.getRootCause(),
                recommendations == null ? List.of() : List.copyOf(recommendations)
        );
    }

    private String serializeRecommendations(List<String> recommendations) {
        List<String> values = recommendations == null ? List.of() : recommendations.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
        if (values.isEmpty()) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(values);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to serialize incident recommendations", exception);
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    public record IncidentEnrichmentReport(
            boolean updated,
            boolean ignored,
            String reason,
            IncidentResponse incident,
            ActiveIncidentState activeState,
            IncidentUpdatedEvent updateEvent,
            boolean usedFallback
    ) {
        static IncidentEnrichmentReport ignored(String reason) {
            return new IncidentEnrichmentReport(false, true, reason, null, null, null, false);
        }

        static IncidentEnrichmentReport ignored(
                String reason,
                IncidentResponse incident,
                ActiveIncidentState activeState,
                IncidentUpdatedEvent updateEvent
        ) {
            return new IncidentEnrichmentReport(false, true, reason, incident, activeState, updateEvent, false);
        }

        static IncidentEnrichmentReport updated(
                IncidentResponse incident,
                ActiveIncidentState activeState,
                IncidentUpdatedEvent updateEvent,
                boolean usedFallback
        ) {
            return new IncidentEnrichmentReport(true, false, null, incident, activeState, updateEvent, usedFallback);
        }
    }
}
