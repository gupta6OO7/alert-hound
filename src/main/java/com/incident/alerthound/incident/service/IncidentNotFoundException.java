package com.incident.alerthound.incident.service;

import java.util.UUID;

public class IncidentNotFoundException extends RuntimeException {

    public IncidentNotFoundException(UUID incidentId) {
        super("Incident not found: " + incidentId);
    }
}
