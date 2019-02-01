package org.opentosca.container.client.model;

import io.swagger.client.model.NodeTemplateInstanceDTO;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class NodeInstance {

    private final NodeTemplateInstanceDTO nodeTemplateInstance;

    public String getId() {
        return String.valueOf(nodeTemplateInstance.getId());
    }

    public NodeTemplateInstanceDTO.StateEnum getState() {
        return nodeTemplateInstance.getState();
    }

    public String getTemplate() {
        return nodeTemplateInstance.getNodeTemplateId();
    }

    public String getTemplateType() {
        return nodeTemplateInstance.getNodeTemplateType();
    }
}
