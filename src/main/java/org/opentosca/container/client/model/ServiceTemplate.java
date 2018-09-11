package org.opentosca.container.client.model;

import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

public class ServiceTemplate {

	private QName id;
	private String applicationId;
	private Map<String, String> properties;
	private List<NodeTemplate> nodeTemplates;
	private List<RelationshipTemplate> relationshipTemplate;

	public ServiceTemplate(final QName id, final String applicationId, final Map<String, String> properties,
			final List<NodeTemplate> nodeTemplates, final List<RelationshipTemplate> relationshipTemplates) {
		this.id = id;
		this.applicationId = applicationId;
		this.properties = properties;
		this.nodeTemplates = nodeTemplates;
		this.relationshipTemplate = relationshipTemplates;
	}

	public QName getId() {
		return this.id;
	}

	public String getApplicationId() {
		return this.applicationId;
	}

	public Map<String, String> getProperties() {
		return this.properties;
	}

	public List<NodeTemplate> getNodeTemplates() {
		return this.nodeTemplates;
	}

	public List<RelationshipTemplate> getRelationshipTemplates() {
		return this.relationshipTemplate;
	}

}
