package org.opentosca.container.client.model;

import io.swagger.client.model.PlanInstanceOutputDTO;
import lombok.RequiredArgsConstructor;

/**
 * Wrapper class for PlanInstanceOutputDTO
 */
@RequiredArgsConstructor
public class PlanInstanceOutput {

    private PlanInstanceOutputDTO planInstanceOutputDTO;

    public String getName() {
        return planInstanceOutputDTO.getName();
    }

    public void setName(String name) {
        planInstanceOutputDTO.setName(name);
    }

    public String getValue() {
        return planInstanceOutputDTO.getValue();
    }

    public void setValue(String value) {
        planInstanceOutputDTO.setValue(value);
    }

    public PlanInstanceOutputType getType() {
        return PlanInstanceOutputType.fromString(planInstanceOutputDTO.getType());
    }

    public void setType(PlanInstanceOutputType type) {
        planInstanceOutputDTO.setType(type.name());
    }

}
