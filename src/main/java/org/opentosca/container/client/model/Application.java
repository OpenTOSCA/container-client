package org.opentosca.container.client.model;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import io.swagger.client.model.CsarDTO;
import io.swagger.client.model.InterfaceDTO;
import io.swagger.client.model.NodeTemplateDTO;
import io.swagger.client.model.PlanDTO;
import io.swagger.client.model.PlanInstanceDTO;
import io.swagger.client.model.PropertiesDTO;
import io.swagger.client.model.ServiceTemplateDTO;
import lombok.Builder;

@Builder
public class Application {

    private final CsarDTO csar;

    private final ServiceTemplateDTO serviceTemplate;

    private final List<NodeTemplateDTO> nodeTemplates;

    private final PlanDTO buildPlan;

    private final List<PlanInstanceDTO> buildPlanInstances;

    private final List<InterfaceDTO> interfaces;

    private final List<String> fileLocations;

    private final PropertiesDTO boundaryDefinitionProperties;

    public String getId() {
        return csar.getId();
    }

    public String getVersion() {
        return csar.getVersion();
    }

    public List<String> getAuthors() {
        return csar.getAuthors();
    }

    public String getName() {
        return csar.getName();
    }

    public String getDisplayName() {
        return this.csar.getDisplayName();
    }

    public String getDescription() {
        return this.csar.getDescription();
    }

    public ServiceTemplate getServiceTemplate() {
        return new ServiceTemplate(serviceTemplate);
    }

    public List<NodeTemplate> getNodeTemplates() {
        Objects.requireNonNull(nodeTemplates);
        return nodeTemplates.stream().map(NodeTemplate::new).collect(Collectors.toList());
    }

    public Plan getBuildPlan() {
        return new Plan(buildPlan);
    }

    public List<PlanInstance> getBuildPlanInstances() {
        return buildPlanInstances.stream().map(PlanInstance::new).collect(Collectors.toList());
    }

    public List<Interface> getInterfaces() {
        return this.interfaces.stream().map(Interface::new).collect(Collectors.toList());
    }

    public List<String> getFileLocations() {
        return this.fileLocations;
    }

    public BoundaryDefinitionProperties getBoundaryDefinitionProperties() {
        return new BoundaryDefinitionProperties(this.boundaryDefinitionProperties);
    }
}
