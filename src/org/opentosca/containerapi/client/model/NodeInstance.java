package org.opentosca.containerapi.client.model;

import java.util.Map;

public class NodeInstance {

	private final String serviceInstance;
	private final String id;
	private final Map<String, String> properties;
	private final String state;
	private final String nodeTemplateId;

	public NodeInstance(String serviceInstanceId, String id, Map<String, String> properties, String state, String nodeTemplateId) {
		this.serviceInstance = serviceInstanceId;
		this.id = id;
		this.properties = properties;
		this.state = state;
		this.nodeTemplateId = nodeTemplateId;
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
	
	public String getNodeTemplateId() {
		return this.nodeTemplateId;
	}
	
}
