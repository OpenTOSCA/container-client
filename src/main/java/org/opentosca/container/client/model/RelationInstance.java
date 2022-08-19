package org.opentosca.container.client.model;

import io.swagger.client.model.NodeTemplateInstanceDTO;
import io.swagger.client.model.RelationshipTemplateInstanceDTO;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class RelationInstance {
    private final RelationshipTemplateInstanceDTO relationshipTemplateInstance;

    public String getId() {
        return String.valueOf(relationshipTemplateInstance.getId());
    }

    public RelationshipTemplateInstanceDTO.StateEnum getState() {
        return relationshipTemplateInstance.getState();
    }

    public String getTemplate() {
        return relationshipTemplateInstance.getRelationshipTemplateId();
    }

    public String getTemplateType() {
        return relationshipTemplateInstance.getRelationshipTemplateType();
    }
}
