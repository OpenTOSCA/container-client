package org.opentosca.containerapi.client.model;

import java.util.List;

/**
 * 
 * @author francoaa
 *
 */
public class Application {

	private String id;
	private List<String> inputParameters;
	private List<String> serviceInstancIds;
	private String displayName;
	private String version;
	private String description;
	private String author;

	public Application(String id, List<String> inputParameters, List<String> serviceInstanceIds, String displayName,
			String version, String description, String author) {
		this.id = id;
		this.inputParameters = inputParameters;
		this.serviceInstancIds = serviceInstanceIds;
		this.displayName = displayName;
		this.version = version;
		this.description = description;
		this.author = author;
	}

	public String getId() {
		return id;
	}

	public void setName(String name) {
		this.id = name;
	}

	public List<String> getInputParameters() {
		return inputParameters;
	}

	public void setInputParameters(List<String> inputParameters) {
		this.inputParameters = inputParameters;
	}

	public List<String> getServiceInstanceIds() {
		return this.serviceInstancIds;
	}

	public String getDisplayName() {
		return this.displayName;
	}

	public String getVersion() {
		return this.version;
	}

	public String getDescription() {
		return this.description;
	}

	public String getAuthor() {
		return this.author;
	}

}
