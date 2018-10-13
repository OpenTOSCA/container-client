package org.opentosca.container.client.model;

import io.swagger.client.model.PlanDTO;
import io.swagger.client.model.PlanInstanceDTO;
import io.swagger.client.model.ServiceTemplateInstanceDTO;
import lombok.Builder;
import org.joda.time.DateTime;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Builder
public class ApplicationInstance {

    private final Application application;

    private final ServiceTemplateInstanceDTO serviceTemplateInstance;

    private final List<PlanDTO> managementPlans;

    private final List<PlanInstanceDTO> managementPlanInstances;

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

    public List<PlanInstance> getManagementPlanInstances() {
        return managementPlanInstances.stream()
                .map(PlanInstance::new)
                .filter(e -> e.getType().equals(PlanInstanceDTO.TypeEnum.MANAGEMENT))
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
