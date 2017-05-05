package org.opentosca.containerapi.client;

import java.util.List;

/**
 * 
 * @author francoaa
 *
 */
public class Application {
	
	private String name;
	private List<String> inputParameters;
	
	public Application(String name, List<String> inputParameters) {
		this.name = name;
		this.inputParameters = inputParameters;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public List<String> getInputParameters() {
		return inputParameters;
	}
	
	public void setInputParameters(List<String> inputParameters) {
		this.inputParameters = inputParameters;
	}
	
}
