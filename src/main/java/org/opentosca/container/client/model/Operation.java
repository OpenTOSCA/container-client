package org.opentosca.container.client.model;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import io.swagger.client.model.OperationDTO;
import io.swagger.client.model.PlanDTO;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class Operation {

	private final OperationDTO operation;
	
	public String getName() {
		return this.operation.getName();
	}
	
	public List<String> getInputParameters() {
		List<String> inputParams = new ArrayList<String>();
		
		if(this.operation.getNodeOperation() != null) {
			inputParams.addAll(this.operation.getNodeOperation().getInputParameters().stream().map(x -> x.getName()).collect(Collectors.toList()));
		} else if (this.operation.getRelationshipOperation() != null) {
			// TODO/FIXME relationshipOperations have no parameters ?
		} else {
			inputParams.addAll(this.operation.getPlan().getInputParameters().stream().map(x -> x.getName()).collect(Collectors.toList()));
		}
		return inputParams;
	}
	
	public List<String> getOutputParameters() {
		List<String> outputParams = new ArrayList<String>();
		
		if(this.operation.getNodeOperation() != null) {
			outputParams.addAll(this.operation.getNodeOperation().getOutputParameters().stream().map(x -> x.getName()).collect(Collectors.toList()));
		} else if(this.operation.getRelationshipOperation() != null) {
			// TODO/FIXME relationshipOperations have no parameters ?
		} else {
			outputParams.addAll(this.operation.getPlan().getOutputParameters().stream().map(x -> x.getName()).collect(Collectors.toList()));
		}
		
		return outputParams;
	}
	
}
