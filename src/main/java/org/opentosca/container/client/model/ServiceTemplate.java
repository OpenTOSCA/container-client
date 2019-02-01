package org.opentosca.container.client.model;

import io.swagger.client.model.ServiceTemplateDTO;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ServiceTemplate {

    private final ServiceTemplateDTO serviceTemplate;

    public String getId() {
        return serviceTemplate.getId();
    }

    public String getName() {
        return serviceTemplate.getName();
    }
}
