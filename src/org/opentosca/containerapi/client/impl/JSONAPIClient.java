package org.opentosca.containerapi.client.impl;

import java.net.URI;
import java.net.URL;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.json.JSONObject;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.LoggingFilter;

public abstract class JSONAPIClient {

	protected JSONObject getJSONResource(String url) {
		WebResource instancePropertiesResource = this.createWebResource(url);
	
		ClientResponse instancePropertiesResponse = instancePropertiesResource.accept(MediaType.APPLICATION_JSON)
				.get(ClientResponse.class);
		return new JSONObject(instancePropertiesResponse.getEntity(String.class));
	}
	
	protected JSONObject getJSONResource(URI url) {
		WebResource instancePropertiesResource = this.createWebResource(url.toString());
	
		ClientResponse instancePropertiesResponse = instancePropertiesResource.accept(MediaType.APPLICATION_JSON)
				.get(ClientResponse.class);
		return new JSONObject(instancePropertiesResponse.getEntity(String.class));
	}

	protected WebResource createWebResource(final String resourceName) {
		return createWebResource(resourceName, null, false);
	}
	

	protected WebResource createWebResource(final String resourceName, final Map<String, String> queryParamsMap, boolean logging) {
		Client client = Client.create();
	
		if (logging) {
			client.addFilter(new LoggingFilter(System.out));
		}
	
		WebResource webResource = client.resource(resourceName);
	
		if (queryParamsMap != null) {
			for (Map.Entry<String, String> entry : queryParamsMap.entrySet()) {
				webResource = webResource.queryParam(entry.getKey(), entry.getValue());
			}
		}
		return webResource;
	}

	protected String getLastPathSegment(URI uri) {
		System.out.println("Getting last path segment of: " + uri);
		return uri.getPath().substring(uri.getPath().lastIndexOf("/") + 1 );
	}
}