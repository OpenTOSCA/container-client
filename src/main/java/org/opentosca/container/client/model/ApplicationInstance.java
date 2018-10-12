package org.opentosca.container.client.model;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import io.swagger.client.model.PlanDTO;
import io.swagger.client.model.ServiceTemplateInstanceDTO;
import lombok.Builder;
import org.joda.time.DateTime;

@Builder
public class ApplicationInstance {

    private final Application application;

    private final ServiceTemplateInstanceDTO serviceTemplateInstance;

    private final List<PlanDTO> managementPlans;

    // TODO: List of management plan instances

    public String getId() {
        return serviceTemplateInstance.getId().toString();
    }

    public DateTime getCreatedAt() {
        return serviceTemplateInstance.getCreatedAt();
    }

    public ServiceTemplateInstanceDTO.StateEnum getState() {
        return serviceTemplateInstance.getState();
    }

    public List<Plan> getManagementPlans() {
        return managementPlans.stream()
                .map(Plan::new)
                .filter(Plan::isManagementPlan)
                .collect(Collectors.toList());
    }

    public Plan getTerminationPlan() {
        return managementPlans.stream()
                .map(Plan::new)
                .filter(Plan::isTerminationPlan)
                .findFirst().orElseThrow(IllegalStateException::new);
    }

    public Application getApplication() {
        Objects.requireNonNull(this.application);
        return this.application;
    }
}
