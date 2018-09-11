package org.opentosca.container.client.model;

import java.util.List;

/**
 * 
 * @author francoaa
 *
 */
public class Application {

	private final String id;
	private final List<String> inputParameters;
	private final List<String> serviceInstancIds;
	private final String displayName;
	private final String version;
	private final String description;
	private final String author;
	private final List<Interface> interfaces;
	private final String metadata;

	public Application(String id, List<String> inputParameters, List<String> serviceInstanceIds, String displayName,
			String version, String description, String author, List<Interface> interfaces, String metadata) {
		this.id = id;
		this.inputParameters = inputParameters;
		this.serviceInstancIds = serviceInstanceIds;
		this.displayName = displayName;
		this.version = version;
		this.description = description;
		this.author = author;
		this.interfaces = interfaces;
		this.metadata = metadata;
	}

	public String getId() {
		return id;
	}
	

	public List<String> getInputParameters() {
		return inputParameters;
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
	
	public List<Interface> getInterfaces(){
		return this.interfaces;
	}

	public String getMetadata() { //JSONObject
		return this.metadata;
	}
	
}
