package org.opentosca.container.client;

import java.util.concurrent.CompletableFuture;

import org.json.JSONObject;
import org.opentosca.container.client.model.Application;

public interface SmartServiceContainerClientAsync extends ContainerClientAsync {

	public CompletableFuture<JSONObject> getSmartServiceDescriptionAsync(Application app);
	
}
