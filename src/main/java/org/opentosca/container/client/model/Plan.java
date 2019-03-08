package org.opentosca.container.client.model;

import java.util.List;
import java.util.stream.Collectors;

import io.swagger.client.model.PlanDTO;
import io.swagger.client.model.TParameter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class Plan {

    private final PlanDTO plan;

    public String getId() {
        return plan.getId();
    }

    public String getName() {
        return plan.getName();
    }

    public PlanType getType() {
        return PlanType.fromString(plan.getPlanType());
    }

    public List<String> getInputParameters() {
        return plan.getInputParameters().stream().map(TParameter::getName).collect(Collectors.toList());
    }

    public boolean isBuildPlan() {
        return getType().equals(PlanType.BUILD);
    }

    public boolean isTerminationPlan() {
        return getType().equals(PlanType.TERMINATION);
    }

    public boolean isManagementPlan() {
        return getType().equals(PlanType.MANAGEMENT);
    }
}
