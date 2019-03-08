package org.opentosca.container.client.model;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import io.swagger.client.model.PlanDTO;
import io.swagger.client.model.PlanInstanceDTO;
import io.swagger.client.model.ServiceTemplateInstanceDTO;
import lombok.Builder;
import org.joda.time.DateTime;

@Builder
public class ApplicationInstance {

    private final Application application;

    private final ServiceTemplateInstanceDTO serviceTemplateInstance;

    private final List<NodeInstance> nodeInstances;

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

    public List<NodeInstance> getNodeInstances() {
        return nodeInstances;
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
        Objects.requireNonNull(application);
        return application;
    }
}
