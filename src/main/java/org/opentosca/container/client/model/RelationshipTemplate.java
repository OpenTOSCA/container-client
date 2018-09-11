package org.opentosca.container.client.model;

import javax.xml.namespace.QName;

public class RelationshipTemplate {

	private String id;
	private QName serviceTemplateId;
	private QName type;
	private String sourceNodeId;
	private String targetNodeId;
	
	public RelationshipTemplate(final String id, final QName serviceTemplateId, final QName type, final String sourceNodeId, final String targetNodeId) {
		this.id = id;
		this.serviceTemplateId = serviceTemplateId;
		this.type = type;
		this.sourceNodeId = sourceNodeId;
		this.targetNodeId = targetNodeId;
	}

	public String getId() {
		return id;
	}

	public QName getServiceTemplateId() {
		return serviceTemplateId;
	}

	public QName getType() {
		return type;
	}

	public String getSourceNodeId() {
		return sourceNodeId;
	}

	public String getTargetNodeId() {
		return targetNodeId;
	}
	
}
