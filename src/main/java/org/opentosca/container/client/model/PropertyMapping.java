package org.opentosca.container.client.model;

import io.swagger.client.model.PropertyMappingDTO;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PropertyMapping {
    private final PropertyMappingDTO propertyMapping;

    public String getServiceTemplatePropertyRef() {
        return this.propertyMapping.getServiceTemplatePropertyRef();
    }

    public String getTargetObjectRef() {
        return this.propertyMapping.getTargetObjectRef();
    }

    public String getTargetPropertyRef() {
        return this.propertyMapping.getTargetPropertyRef();
    }
}
