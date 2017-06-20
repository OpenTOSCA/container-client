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
	private Map<String, String> planOutputParameters = null; // instantiation output parameters
	
	public ServiceInstance(String applicationId, String id, Map<String, String> properties) {
		this.id = id;
		this.applicationId = applicationId;
		this.properties = properties;
	}

	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public Map<String,String> getPlanOutputParameters() {
		return this.planOutputParameters;
	}
	
	public void setPlanOutputParameters(Map<String, String> planOutputParameters) {
		this.planOutputParameters = planOutputParameters;
	}
	
	public Map<String, String> getProperties() {
		return properties;
	}

	public String getApplicationId() {
		return this.applicationId;
	}
}
