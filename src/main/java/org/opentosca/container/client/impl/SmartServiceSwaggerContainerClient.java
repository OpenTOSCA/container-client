package org.opentosca.container.client.impl;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;

import org.json.JSONObject;
import org.opentosca.container.client.SmartServiceContainerClient;
import org.opentosca.container.client.SmartServiceContainerClientAsync;
import org.opentosca.container.client.model.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SmartServiceSwaggerContainerClient extends SwaggerContainerClient implements SmartServiceContainerClient, SmartServiceContainerClientAsync {
	
	private static final Logger logger = LoggerFactory.getLogger(SmartServiceSwaggerContainerClient.class);
	
	public SmartServiceSwaggerContainerClient(String basePath, int timeout) {
		super(basePath, timeout);
	}
	
	public class NoSmartServiceException extends Exception {	
		private static final long serialVersionUID = -32945087659673295L;

		public NoSmartServiceException(String message) {
			super(message);
		}
	}
	
	@Override
	public JSONObject getSmartServiceDescription(Application app) {		
		try {
			return this.getSmartServiceDescriptionAsync(app).get();
		} catch (Exception e) {
			logger.error("Error while getting smart service description for {}", app.getId());
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public CompletableFuture<JSONObject> getSmartServiceDescriptionAsync(Application app) {
		CompletableFuture<JSONObject> future = new CompletableFuture<>();
		executor.submit(() -> {
			try {
				boolean found = false;
				for(String fileLocation : app.getFileLocations()) {
					if(fileLocation.endsWith("smartservice.json")) {
						found = true;
						Response response = this.client.getApiClient().getHttpClient().target(fileLocation).request().get();				
						int status = response.getStatus();
						
						
						if (status >= 200 && status < 400) {							
							future.complete(new JSONObject(response.readEntity(String.class)));
						} else {
							logger.error("HTTP response code {} while requesting smart service description", status);
							future.completeExceptionally(new RuntimeException("Failed to request smart service description: " + app.getId()));
						}										
					}
				}
				if(!found) {
					future.completeExceptionally(new NoSmartServiceException("The given application is not a smart service: " + app.getId()));
				}
			} catch (Exception e) {
				logger.error("Error executing request", e);
				future.completeExceptionally(e);
			}
		});
		return future;
	}

}
