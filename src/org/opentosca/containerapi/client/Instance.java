package org.opentosca.containerapi.client;

import java.util.Map;

/**
 * 
 * @author francoaa
 *
 */
public class Instance {
	
	private String id; // url
	private Map<String, String> properties; // 
	private Map<String, String> inputParameters; // instantiation input parameters
	private Map<String, String> outputParameters; // instantiation output parameters

	
	public Instance(String id) {
		super();
		this.id = id;
	}

	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public Map<String, String> getProperties() {
		return properties;
	}
	
	public void setProperties(Map<String, String> properties) {
		this.properties = properties;
	}

	public Map<String, String> getInputParameters() {
		return inputParameters;
	}

	public void setInputParameters(Map<String, String> inputParameters) {
		this.inputParameters = inputParameters;
	}

	public Map<String, String> getOutputParameters() {
		return outputParameters;
	}

	public void setOutputParameters(Map<String, String> outputParameters) {
		this.outputParameters = outputParameters;
	}
	
	
}
