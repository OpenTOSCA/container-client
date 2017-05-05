package org.opentosca.containerapi.client;

import java.util.Map;

/**
 * 
 * @author francoaa
 *
 */
public class Instance {
	
	private String id;
	private Map<String, String> inputParameters;
	
	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public Map<String, String> getInputParameters() {
		return inputParameters;
	}
	
	public void setInputParameters(Map<String, String> inputParameters) {
		this.inputParameters = inputParameters;
	}
	
}
