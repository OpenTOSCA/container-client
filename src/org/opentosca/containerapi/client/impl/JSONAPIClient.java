package org.opentosca.containerapi.client.impl;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
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

	protected OutputStream getFileResource(URI url) {
		WebResource instancePropertiesResource = this.createWebResource(url.toString());
	
		ClientResponse instancePropertiesResponse = instancePropertiesResource.accept(MediaType.APPLICATION_OCTET_STREAM)
				.get(ClientResponse.class);
		DataInputStream input = new DataInputStream (instancePropertiesResponse.getEntityInputStream());
		OutputStream output = null;
		if (instancePropertiesResponse.getStatus() == 200) {
			byte[] data = new byte[4096];
			int i;
			try {
			    output = new ByteArrayOutputStream();
				while ((i = input.read(data)) != -1) {
	                output.write(data, 0, i);
	            }
	
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return null;
			}
		}
		return output;
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