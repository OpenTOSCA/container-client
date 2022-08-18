package org.opentosca.container.client.model;

import io.swagger.client.model.PropertiesDTO;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class BoundaryDefinitionProperties {
    private final PropertiesDTO properties;

    public Object getXMLFragment() {
        return this.properties.getXmlFragment();
    }

    public List<PropertyMapping> getPropertyMappings() {
        return this.properties.getPropertyMappings().stream().map(PropertyMapping::new).collect(Collectors.toList());
    }
}
