package org.opentosca.containerapi.client.impl;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.core.MediaType;
import javax.xml.namespace.QName;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.opentosca.containerapi.client.IOpenTOSCAContainerAPIClient;
import org.opentosca.containerapi.client.model.Application;
import org.opentosca.containerapi.client.model.NodeInstance;
import org.opentosca.containerapi.client.model.RelationInstance;
import org.opentosca.containerapi.client.model.ServiceInstance;
import org.opentosca.containerapi.client.model.ServiceTemplate;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.WebResource.Builder;
import com.sun.jersey.multipart.FormDataMultiPart;
import com.sun.jersey.multipart.MultiPart;
import com.sun.jersey.multipart.file.FileDataBodyPart;

public class OpenTOSCAContainerLegacyAPIClient extends OpenTOSCAContainerInternalAPIClient
		implements IOpenTOSCAContainerAPIClient {

	public OpenTOSCAContainerLegacyAPIClient() {
		this.containerUrl = URI.create("http://localhost:1337/");
		this.setLegacyContainerAPIUrl("http://localhost:1337/containerapi");
	}

	public OpenTOSCAContainerLegacyAPIClient(final String containerHost, final String containerHostInternal) {
		this.containerHost = containerHost;
		this.containerHostInternal = containerHostInternal;
		this.containerUrl = URI.create("http://" + containerHost + ":1337/");
		this.setLegacyContainerAPIUrl("http://" + containerHost + ":1337/containerapi");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.opentosca.containerapi.client.IContainerAPIClient#createInstance(java
	 * .lang.String, java.util.Map)
	 */
	@Override
	public ServiceInstance createServiceInstance(final Application application, final Map<String, String> params) {
		JSONObject planInputJsonObj = this.getPlanAsJson(application.getId(), Constants.BUILD_PLAN_PATH)
				.getJSONObject("Plan");

		// fill up the planInput with the given values
		if (params != null && !params.isEmpty()) {
			JSONArray inputParamArray = planInputJsonObj.getJSONArray("InputParameters");
			for (int index = 0; index < inputParamArray.length(); index++) {

				JSONObject inputParam = inputParamArray.getJSONObject(index).getJSONObject("InputParameter");
				if (params.containsKey(inputParam.getString("Name"))) {
					inputParam.put("Value", params.get(inputParam.getString("Name")));
				} else if (this.isOpenTOSCAParam(inputParam.getString("Name"))) {
					if (inputParam.getString("Name").equals("instanceDataAPIUrl")) {
						// @hahnml: Resolve the external host in the URL to the internal one
						inputParam.put("Value",
								resolveUrl(
										this.getOpenTOSCAParamValue(inputParam.getString("Name"), application.getId()),
										this.containerHostInternal));
					} else {
						inputParam.put("Value",
								this.getOpenTOSCAParamValue(inputParam.getString("Name"), application.getId()));
					}
				}

			}
		}

		// http://192.168.209.199:1337/containerapi/CSARs/MyTinyToDo_Bare_Docker.csar/
		// ServiceTemplates/%257Bhttp%253A%252F%252Fopentosca.org%252Fservicetemplates%257DMyTinyToDo_Bare_Docker/Instances
		String mainServiceTemplateUrl = this.getLegacyMainServiceTemplateURL(application.getId());

		String mainServiceTemplateInstancesUrl = mainServiceTemplateUrl + "/Instances";
		String mainServiceTemplateName = mainServiceTemplateUrl.substring(mainServiceTemplateUrl.lastIndexOf("/") + 1);
		QName qname = QName.valueOf(URLDecoder.decode(URLDecoder.decode(mainServiceTemplateName)));

		// POST Request: Starts Plan
		System.out.println("input properties: " + planInputJsonObj);
		WebResource mainServiceTemplateInstancesResource = this.createWebResource(mainServiceTemplateInstancesUrl);
		ClientResponse response = mainServiceTemplateInstancesResource.accept(MediaType.APPLICATION_JSON)
				.post(ClientResponse.class, planInputJsonObj.toString());

		// POST Request: response / plan instance URL
		JSONObject respJsonObj = new JSONObject(response.getEntity(String.class));

		// http://localhost:1337/containerapi/CSARs/MyTinyToDo_Bare_Docker.csar/
		// ServiceTemplates/%7Bhttp%3A%2F%2Fopentosca.org%2Fservicetemplates%7DMyTinyToDo_Bare_Docker/
		// Instances?BuildPlanCorrelationId=1494237169621-0
		String serviceInstancesResourceUrl = respJsonObj.getString("PlanURL");
		String correlationId = serviceInstancesResourceUrl.split("BuildPlanCorrelationId=")[1];

		// Check if service instance is available
		boolean serviceInstanceIsAvailable = false;
		String serviceInstanceUrl = "";
		while (!serviceInstanceIsAvailable) {
			try {
				JSONObject jsonObj = this.getJSONResource(serviceInstancesResourceUrl);
				System.out.println(jsonObj.toString());
				int currentCount = jsonObj.getJSONArray("References").length();
				if (currentCount > 1) { // Self + service instance
					JSONArray jsonRefs = jsonObj.getJSONArray("References");

					for (int index = 0; index < jsonRefs.length(); index++) {
						JSONObject jsonRef = jsonRefs.getJSONObject(index);

						if (jsonRef.has("title") && !jsonRef.getString("title").equals("Self")) {
							// @hahnml: Resolve the internal host in the URL to the external one
							serviceInstanceUrl = resolveUrl(jsonRef.getString("href"), this.containerHost);
							serviceInstanceIsAvailable = true;
							System.out.println("Instance URL: " + serviceInstanceUrl);
							break;
						}
					}
				}

				try {
					Thread.sleep(10000); // 10 seconds
				} catch (InterruptedException e) {
				}

			} catch (JSONException e) {
				// catch JSONException which is thrown if the plan instance is not available yet
				System.out.println("Waiting for plan instance to be available");

				try {
					Thread.sleep(10000); // 10 seconds
				} catch (InterruptedException e2) {
				}
			}

			// FIXME timeout to break the loop
		}

		try {
			Thread.sleep(1000); // 10 seconds
		} catch (InterruptedException e) {
		}

		// /Instances/1/PlanInstances/1486950673724-0/State
		String planInstanceUrl = serviceInstanceUrl + "/PlanInstances/" + correlationId + "/State";
		System.out.println(planInstanceUrl);

		boolean instanceFinished = false;
		while (!instanceFinished) {
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			JSONObject planInstanceRespJson = this.getJSONResource(planInstanceUrl);
			System.out.println(planInstanceRespJson);

			if (planInstanceRespJson.getJSONObject("PlanInstance").getString("State").equals("finished")) {
				instanceFinished = true;
			}
			// FIXME timeout to break the loop
		}

		Map<String, String> planOutputs = new HashMap<String, String>();
		String planInstanceOutputUrl = serviceInstanceUrl + "/PlanInstances/" + correlationId + "/Output";

		JSONObject planInstanceOutputJson = this.getJSONResource(planInstanceOutputUrl);

		JSONArray planOutputParams = planInstanceOutputJson.getJSONArray("outputs");

		for (int index = 0; index < planOutputParams.length(); index++) {
			JSONObject outputParamJson = planOutputParams.getJSONObject(index);
			if (outputParamJson.has("name") & outputParamJson.has("value")) {
				String name = outputParamJson.getString("name");
				String value = outputParamJson.getString("value");
				planOutputs.put(name, value);
			}
		}

		Long id = Long.valueOf(serviceInstanceUrl.substring(serviceInstanceUrl.lastIndexOf("/") + 1));
		ServiceInstance createdInstance = new ServiceInstance(application.getId(),
				this.getServiceTemplateId(application), id, serviceInstanceUrl,
				this.getServiceInstanceProperties(serviceInstanceUrl), this.getServiceInstanceState(serviceInstanceUrl),
				planOutputs);

		return createdInstance;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.opentosca.containerapi.client.IContainerAPIClient#
	 * getApplicationProperties(java.lang.String)
	 */

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.opentosca.containerapi.client.IContainerAPIClient#deployApplication(
	 * java.lang.String)
	 */
	@Override
	public Application deployApplication(final String filePath) throws Exception {
		String url = this.getLegacyContainerAPIUrl() + "/CSARs";
		File fileObj = new File(filePath);
		String absPath = fileObj.getAbsolutePath();
		if (fileObj != null && fileObj.exists() && fileObj.isFile()) {
			WebResource webResource = createWebResource(url);
			// the file to upload, represented as FileDataBodyPart
			FileDataBodyPart fileDataBodyPart = new FileDataBodyPart("file", fileObj,
					MediaType.APPLICATION_OCTET_STREAM_TYPE);
			// fileDataBodyPart.setContentDisposition(FormDataContentDisposition.name("file").fileName(fileObj.getName()).build());

			@SuppressWarnings("resource")
			final MultiPart multiPart = new FormDataMultiPart().bodyPart(fileDataBodyPart);
			multiPart.setMediaType(MediaType.MULTIPART_FORM_DATA_TYPE);

			Builder builder = webResource.type(MediaType.MULTIPART_FORM_DATA).accept(MediaType.APPLICATION_JSON);

			ClientResponse response = builder.post(ClientResponse.class, multiPart);

			if (response.getStatus() >= 400) {
				throw new Exception("The request produced an error!");
			}
			String ret = response.getEntity(String.class);
			response.close();

			// parse response to get csar name
			JSONObject obj = new JSONObject(ret);
			String csarPath = obj.getString("csarPath");
			String csarName = csarPath.substring(csarPath.lastIndexOf("/") + 1);
			List<String> inputParams = this.getCreateInstanceInputParameters(csarName);

			// get Application properties: display name, author, ...
			JSONObject appProps = this.getApplicationProperties(csarName);
			String metadataStr = "";
			OutputStream byteOutputStream = this.getApplicationContent(
					csarName + "/" + Constants.OPENTOSCACONTAINERAPI_PATH_CONTENT_METADATA_SMARTSERVICESJSON);

			if (byteOutputStream != null) {
				metadataStr = byteOutputStream.toString();
				try {
					byteOutputStream.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			// String name, List<String> inputParameters, List<ServiceInstance>
			// instances, String displayName,
			// String version, String description, String author, List<Interface>
			// interfaces, String metadata
			Application deployedAplication = new Application(csarName, this.filterManagementParameters(inputParams),
					new ArrayList<String>(), this.getDisplayName(appProps), this.getVersion(appProps),
					this.getDescription(appProps), this.getAuthor(appProps), this.getInterfaces(csarName), metadataStr);

			while (!arePlansDeployed(deployedAplication)) {
				try {
					Thread.sleep(5000); // 5 seconds
				} catch (InterruptedException e) {
				}
			}

			return deployedAplication;

		} else {
			System.out.println("Upload not possible");
			return null;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.opentosca.containerapi.client.IContainerAPIClient#getApplications()
	 */
	@Override
	public List<Application> getApplications() {
		List<String> csarNames = new ArrayList<String>();
		String csarsUrl = this.getLegacyContainerAPIUrl() + "/CSARs";

		JSONObject respJsonObj = this.getJSONResource(csarsUrl);

		JSONArray refArrayJson = respJsonObj.getJSONArray("References");

		for (int index = 0; index < refArrayJson.length(); index++) {
			JSONObject refJsonObj = refArrayJson.getJSONObject(index);
			if (refJsonObj.has("title") && !refJsonObj.getString("title").equals("Self")) {
				csarNames.add(refJsonObj.getString("title"));
			}
		}

		List<Application> apps = new ArrayList<Application>();
		for (String csarName : csarNames) {
			List<String> inputParams = getCreateInstanceInputParameters(csarName);

			// String name, List<String> inputParameters, List<ServiceInstance>
			// instances, String displayName,
			// String version, String description, String author
			JSONObject appProps = this.getApplicationProperties(csarName);

			String metadataStr = "";
			OutputStream byteOutputStream = this.getApplicationContent(
					csarName + "/" + Constants.OPENTOSCACONTAINERAPI_PATH_CONTENT_METADATA_SMARTSERVICESJSON);

			if (byteOutputStream != null) {
				metadataStr = byteOutputStream.toString();
				try {
					byteOutputStream.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			apps.add(new Application(csarName, this.filterManagementParameters(inputParams), new ArrayList<String>(),
					this.getDisplayName(appProps), this.getVersion(appProps), this.getDescription(appProps),
					this.getAuthor(appProps), this.getInterfaces(csarName), metadataStr));
		}
		return apps;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.opentosca.containerapi.client.IOpenTOSCAContainerAPIClient#getApplication
	 * (java.lang.String)
	 */
	@Override
	public Application getApplication(final String csarName) {
		List<String> inputParams = getCreateInstanceInputParameters(csarName);
		// String name, List<String> inputParameters, List<ServiceInstance>
		// instances, String displayName,
		// String version, String description, String author
		JSONObject appProps = this.getApplicationProperties(csarName);

		String metadataStr = "";
		OutputStream byteOutputStream = this.getApplicationContent(
				csarName + "/" + Constants.OPENTOSCACONTAINERAPI_PATH_CONTENT_METADATA_SMARTSERVICESJSON);

		if (byteOutputStream != null) {
			metadataStr = byteOutputStream.toString();
			try {
				byteOutputStream.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return new Application(csarName, this.filterManagementParameters(inputParams), new ArrayList<String>(),
				this.getDisplayName(appProps), this.getVersion(appProps), this.getDescription(appProps),
				this.getAuthor(appProps), this.getInterfaces(csarName), metadataStr);
	}

	@Override
	public List<NodeInstance> getNodeInstances(ServiceInstance serviceInstance) {
		List<NodeInstance> nodeInstances = new ArrayList<NodeInstance>();
		List<String> nodeTemplateUrls = this.getNodeTemplateURLs(serviceInstance);

		for (String nodeTemplateUrl : nodeTemplateUrls) {
			nodeInstances.addAll(
					this.getNodeInstancesFromNodeTemplateUrl(serviceInstance.getApplicationId(), nodeTemplateUrl));

		}

		return nodeInstances;
	}

	@Override
	public List<RelationInstance> getRelationInstances(ServiceInstance serviceInstance) {
		List<RelationInstance> relationInstances = new ArrayList<RelationInstance>();
		List<String> relationshipTemplateUrls = this.getRelationshipTemplateUrls(serviceInstance);

		for (String nodeTemplateUrl : relationshipTemplateUrls) {
			relationInstances.addAll(this.getRelationInstancesFromRelationshipTemplateUrl(
					serviceInstance.getApplicationId(), nodeTemplateUrl));

		}

		return relationInstances;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.opentosca.containerapi.client.IContainerAPIClient#terminateInstance(
	 * org.opentosca.containerapi.client.Instance)
	 */
	@Override
	public boolean terminateServiceInstance(final ServiceInstance instance) {
		JSONObject planInputJsonObj = this.getPlanAsJson(instance.getApplicationId(), Constants.TERMINATE_PLAN_PATH)
				.getJSONObject("Plan");
		System.out.println(planInputJsonObj);

		// fill up the planInput with the given values
		JSONArray inputParamArray = planInputJsonObj.getJSONArray("InputParameters");
		for (int index = 0; index < inputParamArray.length(); index++) {
			JSONObject inputParam = inputParamArray.getJSONObject(index).getJSONObject("InputParameter");

			if (inputParam.getString("Name").equals("OpenTOSCAContainerAPIServiceInstanceID")) {
				// @hahnml: Resolve the external host in the URL to the internal one
				inputParam.put("Value",
						URI.create(resolveUrl(instance.getURL().toString(), this.containerHostInternal)));
			} else if (inputParam.getString("Name").equals("instanceDataAPIUrl")) {
				// @hahnml: Resolve the external host in the URL to the internal one
				inputParam.put("Value",
						resolveUrl(
								this.getOpenTOSCAParamValue(inputParam.getString("Name"), instance.getApplicationId()),
								this.containerHostInternal));
			} else if (inputParam.getString("Name").equals("OpenTOSCAContainerAPIServiceInstanceURL")) {
				// @hahnml: Resolve the external host in the URL to the internal one
				inputParam.put("Value",
						URI.create(resolveUrl(this.getContainerUrl() + instance.getServiceInstanceUrl().toString(),
								this.containerHostInternal)));
			} else if (this.isOpenTOSCAParam(inputParam.getString("Name"))) {
				inputParam.put("Value",
						this.getOpenTOSCAParamValue(inputParam.getString("Name"), instance.getApplicationId()));
			}
		}

		// http://192.168.209.199:1337/containerapi/CSARs/MyTinyToDo_Bare_Docker.csar/
		// ServiceTemplates/%257Bhttp%253A%252F%252Fopentosca.org%252Fservicetemplates%257DMyTinyToDo_Bare_Docker/Instances
		String mainServiceTemplateInstancesUrl = this.getLegacyMainServiceTemplateURL(instance.getApplicationId())
				+ "/Instances";

		// POST Request: Starts Plan
		System.out.println("input properties: " + planInputJsonObj);
		WebResource mainServiceTemplateInstancesResource = this.createWebResource(mainServiceTemplateInstancesUrl);
		ClientResponse response = mainServiceTemplateInstancesResource.accept(MediaType.APPLICATION_JSON)
				.post(ClientResponse.class, planInputJsonObj.toString());

		// POST Request: response / plan instance URL
		JSONObject respJsonObj = new JSONObject(response.getEntity(String.class));

		// http://localhost:1337/containerapi/CSARs/MyTinyToDo_Bare_Docker.csar/
		// ServiceTemplates/%7Bhttp%3A%2F%2Fopentosca.org%2Fservicetemplates%7DMyTinyToDo_Bare_Docker/
		// Instances?BuildPlanCorrelationId=1494237169621-0
		String serviceInstancesResourceUrl = respJsonObj.getString("PlanURL");

		try {
			Thread.sleep(5000); // 5 seconds
		} catch (InterruptedException e) {
		}

		// Check if service instance is available
		WebResource referencesResource = this.createWebResource(instance.getURL().toString());
		boolean serviceInstanceDeleted = false;
		while (!serviceInstanceDeleted) {

			// GET Request: check if plan instance was created
			ClientResponse serviceInstancesResponse = referencesResource.accept(MediaType.APPLICATION_JSON)
					.get(ClientResponse.class);
			String responseEntityBody = serviceInstancesResponse.getEntity(String.class);

			ServiceInstance instanceUpdate = this.updateServiceInstance(instance);

			if (instanceUpdate.getState().equals("DELETED")) {
				serviceInstanceDeleted = true;
			} else {
				try {
					Thread.sleep(5000); // 5 seconds
				} catch (InterruptedException e) {
				}

			}

		}
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.opentosca.containerapi.client.IContainerAPIClient#undeployApplication
	 * (org.opentosca.containerapi.client.Application)
	 */
	@Override
	public String undeployApplication(final Application application) {
		String url = this.getLegacyContainerAPIUrl() + "/CSARs/" + application.getId();
		WebResource webResource = createWebResource(url);
		ClientResponse response = webResource.accept(MediaType.TEXT_PLAIN).delete(ClientResponse.class);
		String ret = response.getEntity(String.class);
		response.close();
		return ret;
	}

	@Override
	public NodeInstance updateNodeInstance(NodeInstance nodeInstance) {
		return this.getNodeInstanceFromNodeInstanceUrl(nodeInstance.getServiceInstance(), nodeInstance.getId());
	}

	@Override
	public RelationInstance updateRelationInstance(RelationInstance relationInstance) {
		return this.getRelationInstanceFromRelationInstanceUrl(relationInstance.getServiceInstance(),
				relationInstance.getId());
	}

	@Override
	public ServiceInstance updateServiceInstance(ServiceInstance serviceInstance) {
		return this.getServiceInstance(serviceInstance.getApplicationId(), serviceInstance.getURL().toString());
	}

	@Override
	public String getContainerAPIUrl() {
		return this.containerUrl.toString();
	}

	@Override
	public Map<String, String> invokeServiceInstanceOperation(ServiceInstance serviceInstance, String interfaceName,
			String operationName, Map<String, String> params) {

		// Fetch invocation message for operation
		URI planOperationUri = URI
				.create(this.getOperationUrl(serviceInstance.getApplicationId(), interfaceName, operationName)
						+ Constants.OPENTOSCACONTAINERAPI_PATHS_PLAN);

		JSONObject respObj = this.getPlanAsJson(planOperationUri.toString());

		JSONObject planInputJsonObj = respObj
				.getJSONObject(Constants.OPENTOSCACONTAINERAPI_INTERFACERESOURCE_JSON_PLAN);

		// set input parameters
		JSONArray inputParamArray = planInputJsonObj.getJSONArray("InputParameters");
		for (int index = 0; index < inputParamArray.length(); index++) {
			JSONObject inputParam = inputParamArray.getJSONObject(index).getJSONObject("InputParameter");

			if (params.containsKey(inputParam.getString("Name"))) {
				inputParam.put("Value", params.get(inputParam.getString("Name")));
			} else if (inputParam.getString("Name").equals("instanceDataAPIUrl")) {
				// @hahnml: Resolve the external host in the URL to the internal one
				inputParam.put("Value", resolveUrl(
						this.getOpenTOSCAParamValue(inputParam.getString("Name"), serviceInstance.getApplicationId()),
						this.containerHostInternal));
			} else if (this.isOpenTOSCAParam(inputParam.getString("Name"))) {
				inputParam.put("Value",
						this.getOpenTOSCAParamValue(inputParam.getString("Name"), serviceInstance.getApplicationId()));
			} else if (inputParam.getString("Name").equals("OpenTOSCAContainerAPIServiceInstanceURL")) {
				// @hahnml: Resolve the external host in the URL to the internal one

				inputParam.put("Value",
						URI.create(
								resolveUrl(this.getContainerUrl() + serviceInstance.getServiceInstanceUrl().toString(),
										this.containerHostInternal)));
			}
		}

		// invoke (plan) operation
		String mainServiceTemplateInstancesUrl = this
				.getLegacyMainServiceTemplateURL(serviceInstance.getApplicationId()) + "/Instances";
		WebResource mainServiceTemplateInstancesResource = this.createWebResource(mainServiceTemplateInstancesUrl);
		ClientResponse response = mainServiceTemplateInstancesResource.accept(MediaType.APPLICATION_JSON)
				.post(ClientResponse.class, planInputJsonObj.toString());

		// POST Request: response / plan instance URL
		JSONObject respJsonObj = new JSONObject(response.getEntity(String.class));

		String serviceInstancesResourceUrl = respJsonObj.getString("PlanURL");
		String correlationId = serviceInstancesResourceUrl.split("BuildPlanCorrelationId=")[1];

		String planInstanceUrl = serviceInstance.getURL() + "/PlanInstances/" + correlationId + "/State";
		System.out.println(planInstanceUrl);

		// await operation completion
		boolean instanceFinished = false;
		while (!instanceFinished) {
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			JSONObject planInstanceRespJson = this.getJSONResource(planInstanceUrl);
			System.out.println(planInstanceRespJson);

			if (planInstanceRespJson.getJSONObject("PlanInstance").getString("State").equals("finished")) {
				instanceFinished = true;
			}

		}

		// get output
		Map<String, String> planOutputs = new HashMap<String, String>();
		String planInstanceOutputUrl = serviceInstance.getURL() + "/PlanInstances/" + correlationId + "/Output";

		JSONObject planInstanceOutputJson = this.getJSONResource(planInstanceOutputUrl);

		JSONArray planOutputParams = planInstanceOutputJson.getJSONArray("outputs");

		for (int index = 0; index < planOutputParams.length(); index++) {
			JSONObject outputParamJson = planOutputParams.getJSONObject(index);
			if (outputParamJson.has("name") & outputParamJson.has("value")) {
				String name = outputParamJson.getString("name");
				String value = outputParamJson.getString("value");
				planOutputs.put(name, value);
			}
		}

		return planOutputs;
	}

	@Override
	public List<ServiceInstance> getServiceInstances(Application application) {
		return this.getInstances(application.getId());
	}

	@Override
	public ServiceTemplate getServiceTemplate(Application application) {

		// we assume there is only one service template in an application
		QName serviceTemplateId = this.getServiceTemplateId(application);

		return new ServiceTemplate(serviceTemplateId, application.getId(),
				this.getServiceTemplateProperties(application), this.getNodeTemplates(application),
				this.getRelationshipTemplates(application));
	}

	@Override
	public ServiceTemplate getServiceTemplate(ServiceInstance serviceInstance) {
		return this.getServiceTemplate(this.getApplication(serviceInstance.getApplicationId()));
	}

}
