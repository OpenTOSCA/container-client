package org.opentosca.containerapi.client.model;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.Map;

import javax.xml.namespace.QName;

/**
 * 
 * @author francoaa
 *
 */
public class ServiceInstance {
	
	private String applicationId;
	private URI legacyUri; // url
	private Map<String, String> properties; // 
	private Map<String, String> planOutputParameters = null; // instantiation output parameters
	private String state;	
	private Long id;
	private QName serviceTemplateId;
	
	public ServiceInstance(String applicationId, QName serviceTemplateId,  Long id, String legacyUri, Map<String, String> properties, String state, Map<String,String> planOutputParameters) {
		this.legacyUri = URI.create(legacyUri);
		this.applicationId = applicationId;
		this.properties = properties;
		this.state = state;
		this.planOutputParameters = planOutputParameters;
		this.id = id;
		this.serviceTemplateId = serviceTemplateId;
	}

	public URI getURL() {
		return legacyUri;
	}
	
	public Long getId() {
		return this.id; 
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
	
	public URI getLegacyServiceInstanceUrl() {
		return this.legacyUri;
	}
	
	public URI getServiceInstanceUrl() {
		try {
			return new URI("csars/" + applicationId + "/servicetemplates/" + URLEncoder.encode(URLEncoder.encode(serviceTemplateId.toString())) + "/instances/" + this.id);
		} catch (URISyntaxException e) {
			e.printStackTrace();
			return null;
		}
	}
		
 }
