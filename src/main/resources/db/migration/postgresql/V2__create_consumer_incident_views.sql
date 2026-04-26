CREATE TABLE IF NOT EXISTS consumer_incident_views (
    incident_id UUID PRIMARY KEY,
    service VARCHAR(100) NOT NULL,
    status VARCHAR(32) NOT NULL,
    severity VARCHAR(32) NOT NULL,
    error_rate DOUBLE PRECISION NOT NULL,
    summary VARCHAR(4000),
    root_cause VARCHAR(4000),
    recommendations_json VARCHAR(4000),
    updated_at TIMESTAMPTZ NOT NULL,
    last_detected_at TIMESTAMPTZ NOT NULL,
    projection_updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_consumer_incident_views_service_projection_updated_at
    ON consumer_incident_views (service, projection_updated_at DESC);
