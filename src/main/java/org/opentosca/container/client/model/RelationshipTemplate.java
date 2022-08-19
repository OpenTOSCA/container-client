package org.opentosca.container.client.model;

import io.swagger.client.model.RelationshipTemplateDTO;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RelationshipTemplate {

    private final RelationshipTemplateDTO relationshipTemplate;

    public String getId() {
        return relationshipTemplate.getId();
    }

    public String getName() {
        return relationshipTemplate.getName();
    }

    public String getRelationshipType() {
        return relationshipTemplate.getRelationshipType();
    }
}
