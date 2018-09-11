package org.opentosca.container.client.model;

import java.util.Map;

public class NodeInstance {

	private final String serviceInstance;
	private final String id;
	private final Map<String, String> properties;
	private final String state;

	public NodeInstance(String serviceInstanceId, String id, Map<String, String> properties, String state) {
		this.serviceInstance = serviceInstanceId;
		this.id = id;
		this.properties = properties;
		this.state = state;
	}

	public String getServiceInstance() {
		return serviceInstance;
	}

	public String getId() {
		return id;
	}

	public Map<String, String> getProperties() {
		return properties;
	}

	public String getState() {
		return state;
	}
	
	
}
