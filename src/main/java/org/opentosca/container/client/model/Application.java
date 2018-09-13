package org.opentosca.container.client.model;

import java.util.List;
import java.util.stream.Collectors;

import io.swagger.client.model.CsarDTO;
import io.swagger.client.model.InterfaceDTO;
import io.swagger.client.model.PlanDTO;
import io.swagger.client.model.ServiceTemplateDTO;
import io.swagger.client.model.TParameter;
import lombok.Builder;
import lombok.RequiredArgsConstructor;

@Builder
@RequiredArgsConstructor
public class Application {

    private final CsarDTO csar;

    private final ServiceTemplateDTO serviceTemplate;

    private final PlanDTO buildPlan;

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

    @RequiredArgsConstructor
    public static class Plan {

        private final PlanDTO plan;

        public String getId() {
            return plan.getId();
        }

        public String getName() {
            return plan.getName();
        }

        public List<String> getInputParameters() {
            return plan.getInputParameters().stream().map(TParameter::getName).collect(Collectors.toList());
        }
    }
}
