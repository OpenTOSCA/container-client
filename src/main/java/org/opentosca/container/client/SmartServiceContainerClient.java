package org.opentosca.container.client;

import org.json.JSONObject;
import org.opentosca.container.client.impl.SmartServiceSwaggerContainerClient.NoSmartServiceException;
import org.opentosca.container.client.model.Application;

public interface SmartServiceContainerClient extends ContainerClient {
	
	public JSONObject getSmartServiceDescription(Application app) throws NoSmartServiceException;
}
