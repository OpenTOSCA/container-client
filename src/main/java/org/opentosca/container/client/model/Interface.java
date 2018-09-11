package org.opentosca.container.client.model;

import java.util.List;
import java.util.Map;

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

	public Map<String, List<String>> getInputParameters() {
		return operation2InputParameters;
	}

	public Map<String, List<String>> getOutputParameters() {
		return operation2OutputParameters;
	}

}
