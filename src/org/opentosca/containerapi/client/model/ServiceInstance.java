package org.opentosca.containerapi.client.model;

import java.util.Map;

/**
 * 
 * @author francoaa
 *
 */
public class ServiceInstance {
	
	private String applicationId;
	private String id; // url
	private Map<String, String> properties; // 
	private Map<String, String> inputParameters; // instantiation input parameters
	private Map<String, String> outputParameters; // instantiation output parameters
	
	public ServiceInstance(String applicationId, String id) {
		this.id = id;
		this.applicationId = applicationId;
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

	public String getApplicationId() {
		return this.applicationId;
	}
}
