package com.incident.alerthound.consumerfacing.repository;

import com.incident.alerthound.consumerfacing.model.ConsumerIncidentViewEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsumerIncidentViewRepository extends JpaRepository<ConsumerIncidentViewEntity, UUID> {

    List<ConsumerIncidentViewEntity> findAllByOrderByProjectionUpdatedAtDesc();

    List<ConsumerIncidentViewEntity> findByServiceIgnoreCaseOrderByProjectionUpdatedAtDesc(String service);
}
