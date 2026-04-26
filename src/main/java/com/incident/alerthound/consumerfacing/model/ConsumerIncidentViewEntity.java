package com.incident.alerthound.consumerfacing.model;

import com.incident.alerthound.incident.model.IncidentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "consumer_incident_views")
public class ConsumerIncidentViewEntity {

    @Id
    private UUID incidentId;

    @Column(nullable = false, length = 100)
    private String service;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private IncidentStatus status;

    @Column(nullable = false, length = 32)
    private String severity;

    @Column(nullable = false)
    private double errorRate;

    @Column(length = 4000)
    private String summary;

    @Column(length = 4000)
    private String rootCause;

    @Column(length = 4000)
    private String recommendationsJson;

    @Column(nullable = false)
    private Instant updatedAt;

    @Column(nullable = false)
    private Instant lastDetectedAt;

    @Column(nullable = false)
    private Instant projectionUpdatedAt;

    public UUID getIncidentId() {
        return incidentId;
    }

    public void setIncidentId(UUID incidentId) {
        this.incidentId = incidentId;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public IncidentStatus getStatus() {
        return status;
    }

    public void setStatus(IncidentStatus status) {
        this.status = status;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public double getErrorRate() {
        return errorRate;
    }

    public void setErrorRate(double errorRate) {
        this.errorRate = errorRate;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getRootCause() {
        return rootCause;
    }

    public void setRootCause(String rootCause) {
        this.rootCause = rootCause;
    }

    public String getRecommendationsJson() {
        return recommendationsJson;
    }

    public void setRecommendationsJson(String recommendationsJson) {
        this.recommendationsJson = recommendationsJson;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Instant getLastDetectedAt() {
        return lastDetectedAt;
    }

    public void setLastDetectedAt(Instant lastDetectedAt) {
        this.lastDetectedAt = lastDetectedAt;
    }

    public Instant getProjectionUpdatedAt() {
        return projectionUpdatedAt;
    }

    public void setProjectionUpdatedAt(Instant projectionUpdatedAt) {
        this.projectionUpdatedAt = projectionUpdatedAt;
    }
}
