package org.opentosca.container.client.model;

import javax.xml.namespace.QName;

public class NodeTemplate {

	private String id;
	private QName serviceTemplateId;
	private QName type;
	
	public NodeTemplate(final String id, final QName serviceTemplateId, final QName type) {
		this.id = id;
		this.serviceTemplateId = serviceTemplateId;
		this.type = type;
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
	
}
