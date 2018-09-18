package org.opentosca.containerapi.client.impl;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.Date;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.LoggingFilter;

public abstract class JSONAPIClient {

	private static final Logger logger = LoggerFactory.getLogger(JSONAPIClient.class);

	private static final java.util.logging.Logger javaLogger = java.util.logging.Logger.getLogger(JSONAPIClient.class.getName());

	static {
		javaLogger.setFilter(record -> {
			JSONAPIClient.logger.info("{} {} {}", new Date(record.getMillis()), record.getLevel(), record.getMessage());
			return false;
		});
	}

	protected JSONObject getJSONResource(String url) {
		WebResource instancePropertiesResource = this.createWebResource(url);

		ClientResponse instancePropertiesResponse = instancePropertiesResource.accept(MediaType.APPLICATION_JSON)
				.get(ClientResponse.class);
		String jsonData = instancePropertiesResponse.getEntity(String.class);
		return new JSONObject(jsonData);
	}

	protected JSONArray getJSONArrayResource(String url) {
		WebResource instancePropertiesResource = this.createWebResource(url);

		ClientResponse instancePropertiesResponse = instancePropertiesResource.accept(MediaType.APPLICATION_JSON)
				.get(ClientResponse.class);
		return new JSONArray(instancePropertiesResponse.getEntity(String.class));
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
		DataInputStream input = new DataInputStream(instancePropertiesResponse.getEntityInputStream());
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
				JSONAPIClient.logger.error("IOException while getting file resource", e);
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
			client.addFilter(new LoggingFilter(JSONAPIClient.javaLogger));
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
		JSONAPIClient.logger.info("Getting last path segment of: {}.", uri);
		return uri.getPath().substring(uri.getPath().lastIndexOf("/") + 1);
	}
}