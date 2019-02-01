package org.opentosca.container.client.model;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.swagger.client.model.PlanInstanceDTO;
import io.swagger.client.model.PlanInstanceEventDTO;
import io.swagger.client.model.PlanInstanceInputDTO;
import io.swagger.client.model.PlanInstanceOutputDTO;
import lombok.RequiredArgsConstructor;

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

    public List<PlanInstanceInputDTO> getInputs() {
        return plan.getInputs();
    }

    public List<PlanInstanceOutputDTO> getOutputs() {
        return plan.getOutputs();
    }

    public List<PlanInstanceEventDTO> getLogs() {
        return plan.getLogs();
    }

    public Map<String, String> getOutputMappings() {
        List<PlanInstanceOutputDTO> outputs = plan.getOutputs();
        return outputs.stream().collect(Collectors.toMap(PlanInstanceOutputDTO::getName, PlanInstanceOutputDTO::getValue));
    }
}
