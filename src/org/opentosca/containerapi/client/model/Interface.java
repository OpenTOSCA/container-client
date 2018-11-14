package org.opentosca.containerapi.client.model;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Interface {

	private final String name;
	private final Map<String, List<String>> operation2InputParameters;
	private final Map<String, List<String>> operation2OutputParameters;

	public Interface(final String name, final Map<String, List<String>> inputParameters,
			final Map<String, List<String>> outputParameters) {
		this.name = name;
		this.operation2InputParameters = inputParameters;
		this.operation2OutputParameters = outputParameters;
	}

	public String getName() {
		return name;
	}

	public List<String> getInputParameters(String operationName) {
		return operation2InputParameters.get(operationName);
	}

	public List<String> getOutputParameters(String operationName) {
		return operation2OutputParameters.get(operationName);
	}
	
	public Set<String> getOperationNames(){
		Set<String> opNames = new HashSet<String>();
		opNames.addAll(this.operation2InputParameters.keySet());
		opNames.addAll(this.operation2OutputParameters.keySet());
		return opNames;		
	}
	
	

}
