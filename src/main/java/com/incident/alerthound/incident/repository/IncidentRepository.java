package com.incident.alerthound.incident.repository;

import com.incident.alerthound.incident.model.Incident;
import com.incident.alerthound.incident.model.IncidentStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IncidentRepository extends JpaRepository<Incident, UUID> {

    List<Incident> findByStatusInOrderByCreatedAtDesc(Collection<IncidentStatus> statuses);

    Optional<Incident> findFirstByServiceAndStatusInOrderByCreatedAtDesc(String service, Collection<IncidentStatus> statuses);
}
