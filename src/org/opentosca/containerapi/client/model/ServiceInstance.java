package org.opentosca.containerapi.client.model;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.Map;

import javax.xml.namespace.QName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author francoaa
 *
 */
public class ServiceInstance {

	private Logger logger = LoggerFactory.getLogger(ServiceInstance.class);
	private String applicationId;
	private URI legacyUri; // url
	private Map<String, String> properties; //
	private Map<String, String> planOutputParameters = null; // instantiation output parameters
	private String state;
	private Long id;
	private QName serviceTemplateId;
	private Collection<Log> buildPlanLogs;

	public ServiceInstance(String applicationId, QName serviceTemplateId, Long id, String legacyUri,
			Map<String, String> properties, String state, Map<String, String> planOutputParameters,
			Collection<Log> buildPlanLogs) {
		this.legacyUri = URI.create(legacyUri);
		this.applicationId = applicationId;
		this.properties = properties;
		this.state = state;
		this.planOutputParameters = planOutputParameters;
		this.id = id;
		this.serviceTemplateId = serviceTemplateId;
		this.buildPlanLogs = buildPlanLogs;
	}

	public URI getURL() {
		return this.legacyUri;
	}

	public Long getId() {
		return this.id;
	}

	public Map<String, String> getPlanOutputParameters() {
		return this.planOutputParameters;
	}

	public Map<String, String> getProperties() {
		return this.properties;
	}

	public String getApplicationId() {
		return this.applicationId;
	}

	public String getState() {
		return this.state;
	}

	public URI getLegacyServiceInstanceUrl() {
		return this.legacyUri;
	}

	public Collection<Log> getBuildPlanLogs() {
		return this.buildPlanLogs;
	}

	public float getCreationProgress() {
		int stepCount = -1;
		int countedSteps = 0;
		// fetch first progress log
		for (Log log : this.buildPlanLogs) {
			if (log.getMessage().contains("overall topology with steps of")) {
				if (stepCount == -1) {
					String msg = log.getMessage();
					String stepCountString = msg.substring(msg.lastIndexOf(" ")).trim();
					stepCount = Integer.valueOf(stepCountString);
				}
				countedSteps++;
			}
		}

		if (stepCount == -1) return 0f;

		return ((float) countedSteps) / ((float) stepCount);
	}

	public URI getServiceInstanceUrl() {
		try {
			return new URI("csars/" + this.applicationId + "/servicetemplates/"
					+ URLEncoder.encode(URLEncoder.encode(this.serviceTemplateId.toString())) + "/instances/" + this.id);
		} catch (URISyntaxException e) {
			this.logger.error("Failed to get Service Instance url.", e);
			return null;
		}
	}

}
