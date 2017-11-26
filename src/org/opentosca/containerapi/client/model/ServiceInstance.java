package org.opentosca.containerapi.client.model;

import java.net.URI;
import java.util.Map;

/**
 * 
 * @author francoaa
 *
 */
public class ServiceInstance {
	
	private String applicationId;
	private URI uri; // url
	private Map<String, String> properties; // 
	private Map<String, String> planOutputParameters = null; // instantiation output parameters
	private String state;
	
	public ServiceInstance(String applicationId, String uri, Map<String, String> properties, String state, Map<String,String> planOutputParameters) {
		this.uri = URI.create(uri);
		this.applicationId = applicationId;
		this.properties = properties;
		this.state = state;
		this.planOutputParameters = planOutputParameters;
	}

	public URI getURL() {
		return uri;
	}
	
	public int getId() {
		return Integer.valueOf(this.uri.getPath().substring(this.uri.getPath().lastIndexOf("/") + 1 ));
	}
	
	public Map<String,String> getPlanOutputParameters() {
		return this.planOutputParameters;
	}
	
	public Map<String, String> getProperties() {
		return properties;
	}

	public String getApplicationId() {
		return this.applicationId;
	}
	
	public String getState(){
		return this.state;
	}
 }
