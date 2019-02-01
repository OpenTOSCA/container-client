package org.opentosca.container.client.model;

import java.util.List;

import io.swagger.client.model.InterfaceDTO;
import io.swagger.client.model.NodeTemplateDTO;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class NodeTemplate {

    private final NodeTemplateDTO nodeTemplate;

    public String getId() {
        return nodeTemplate.getId();
    }

    public String getName() {
        return nodeTemplate.getName();
    }

    public String getNodeType() {
        return nodeTemplate.getNodeType();
    }

    public List<InterfaceDTO> getInterfaces() {
        return nodeTemplate.getInterfaces().getInterfaces();
    }
}
