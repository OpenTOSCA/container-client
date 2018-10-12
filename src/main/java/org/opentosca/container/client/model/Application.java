package org.opentosca.container.client.model;

import java.util.List;
import java.util.stream.Collectors;

import io.swagger.client.model.CsarDTO;
import io.swagger.client.model.InterfaceDTO;
import io.swagger.client.model.PlanDTO;
import io.swagger.client.model.PlanInstanceDTO;
import io.swagger.client.model.ServiceTemplateDTO;
import lombok.Builder;
import lombok.RequiredArgsConstructor;

@Builder
public class Application {

    private final CsarDTO csar;

    private final ServiceTemplateDTO serviceTemplate;

    private final PlanDTO buildPlan;

    private final List<PlanInstanceDTO> buildPlanInstances;

    private final List<InterfaceDTO> interfaces;

    public String getId() {
        return csar.getId();
    }

    public String getName() {
        return csar.getName();
    }

    public ServiceTemplate getServiceTemplate() {
        return new ServiceTemplate(serviceTemplate);
    }

    public Plan getBuildPlan() {
        return new Plan(buildPlan);
    }

    public List<PlanInstance> getBuildPlanInstances() {
        return buildPlanInstances.stream().map(PlanInstance::new).collect(Collectors.toList());
    }

    @RequiredArgsConstructor
    public static class ServiceTemplate {

        private final ServiceTemplateDTO serviceTemplate;

        public String getId() {
            return serviceTemplate.getId();
        }

        public String getName() {
            return serviceTemplate.getName();
        }
    }
}
