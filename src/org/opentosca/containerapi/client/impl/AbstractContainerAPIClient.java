package org.opentosca.containerapi.client.impl;


public class AbstractContainerAPIClient {

	public static String BUILD_PLAN_PATH = "/BoundaryDefinitions/Interfaces/OpenTOSCA-Lifecycle-Interface/Operations/initiate/Plan";
	public static String TERMINATE_PLAN_PATH = "/BoundaryDefinitions/Interfaces/OpenTOSCA-Lifecycle-Interface/Operations/terminate/Plan";
	
	String containerAPIUrl = "";

	String[] opentoscaParameters = { "instanceDataAPIUrl", "csarEntrypoint", "CorrelationID" };

	public String getContainerAPIUrl() {
		return containerAPIUrl;
	}

	
	public void setContainerAPIUrl(final String containerAPIUrl) {
		this.containerAPIUrl = containerAPIUrl;
	}

}