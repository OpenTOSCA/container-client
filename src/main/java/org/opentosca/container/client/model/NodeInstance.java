package org.opentosca.container.client.model;

import io.swagger.client.model.NodeTemplateInstanceDTO;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class NodeInstance {

    private final NodeTemplateInstanceDTO nodeTemplateInstance;

    private final Map<String, Object> properties;

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

    public Map<String, String> getProperties() {
        return properties.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue() == null ? "" : e.getValue().toString()));
    }
}
