/*******************************************************************************
 * Copyright (c) 2018 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache Software License 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *******************************************************************************/
package org.opentosca.container.client.model;

import java.util.List;
import java.util.stream.Collectors;

import io.swagger.client.model.PropertiesDTO;
import lombok.RequiredArgsConstructor;

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
