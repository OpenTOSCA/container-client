package org.opentosca.container.client.model;

import io.swagger.client.model.PlanInstanceDTO;
import lombok.RequiredArgsConstructor;

/**
 * Wrapper class for PlanInstanceDTO
 */
@RequiredArgsConstructor
public class PlanInstance {

    private final PlanInstanceDTO plan;

    public Long getServiceTemplateInstanceId() {
        return plan.getServiceTemplateInstanceId();
    }

    public String getCorrelationId() {
        return plan.getCorrelationId();
    }

    public PlanInstanceDTO.StateEnum getState() {
        return plan.getState();
    }

    public PlanInstanceDTO.TypeEnum getType() {
        return plan.getType();
    }

    // TODO convert to PlanInstanceOutput before returning.
    // public List<PlanInstanceOutput> getOutputs() { return plan.getOutputs();}
}
