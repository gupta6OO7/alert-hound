package com.incident.alerthound.consumerfacing.api;

import com.incident.alerthound.consumerfacing.model.ConsumerIncidentView;
import com.incident.alerthound.consumerfacing.service.ConsumerIncidentViewService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/consumer/incidents")
public class ConsumerIncidentViewController {

    private final ConsumerIncidentViewService consumerIncidentViewService;

    public ConsumerIncidentViewController(ConsumerIncidentViewService consumerIncidentViewService) {
        this.consumerIncidentViewService = consumerIncidentViewService;
    }

    @GetMapping
    public List<ConsumerIncidentView> getIncidents(@RequestParam(required = false) String service) {
        if (service != null && !service.isBlank()) {
            return consumerIncidentViewService.getByService(service);
        }
        return consumerIncidentViewService.getAll();
    }

    @GetMapping("/{id}")
    public ConsumerIncidentView getIncident(@PathVariable UUID id) {
        ConsumerIncidentView view = consumerIncidentViewService.get(id);
        if (view == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "consumer incident view not found");
        }
        return view;
    }
}
