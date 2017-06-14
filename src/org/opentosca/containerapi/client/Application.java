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
	private List<Instance> instances;
	
	public Application(String name, List<String> inputParameters, List<Instance> instances) {
		this.name = name;
		this.inputParameters = inputParameters;
		this.instances = instances;
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
	
	public List<Instance> getInstances() {
		return this.instances;
	}
	
	
	
}
