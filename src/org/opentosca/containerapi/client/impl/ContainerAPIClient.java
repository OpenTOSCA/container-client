package org.opentosca.containerapi.client.impl;

import java.io.File;
import java.io.IOException;
import java.io.StringBufferInputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.Node;

import org.json.JSONArray;
import org.json.JSONObject;
import org.opentosca.containerapi.client.IContainerAPIClient;
import org.opentosca.containerapi.client.model.Application;
import org.opentosca.containerapi.client.model.NodeInstance;
import org.opentosca.containerapi.client.model.RelationInstance;
import org.opentosca.containerapi.client.model.ServiceInstance;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.WebResource.Builder;
import com.sun.jersey.api.client.filter.LoggingFilter;
import com.sun.jersey.multipart.FormDataMultiPart;
import com.sun.jersey.multipart.MultiPart;
import com.sun.jersey.multipart.file.FileDataBodyPart;

/**
 * @author francoaa
 *
 */
public class ContainerAPIClient extends AbstractContainerAPIClient implements IContainerAPIClient {

	String containerAPIUrl = "";

	private String[] opentoscaParameters = { "instanceDataAPIUrl", "csarEntrypoint", "CorrelationID" };

	public ContainerAPIClient() {
		this.containerAPIUrl = "http://localhost:1337/containerapi";
	}

	public ContainerAPIClient(final String containerHost) {
		this.containerAPIUrl = "http://" + containerHost + ":1337/containerapi";
	}

	private boolean arePlansDeployed(Application application) {
		WebResource csarControlResource = this
				.createWebResource(this.getContainerAPIUrl() + "/CSARControl/" + application.getId());

		Builder builder = csarControlResource.accept(MediaType.TEXT_PLAIN);

		String result = builder.get(String.class);

		if (result.contains("PLANS_DEPLOYED")) {
			return true;
		} else {
			return false;
		}
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
		JSONObject planInputJsonObj = this.getPlanAsJson(application.getId(), BUILD_PLAN_PATH).getJSONObject("Plan");

		// fill up the planInput with the given values
		if (params != null && !params.isEmpty()) {
			JSONArray inputParamArray = planInputJsonObj.getJSONArray("InputParameters");
			for (int index = 0; index < inputParamArray.length(); index++) {

				JSONObject inputParam = inputParamArray.getJSONObject(index).getJSONObject("InputParameter");
				if (params.containsKey(inputParam.getString("Name"))) {
					inputParam.put("Value", params.get(inputParam.getString("Name")));
				} else if (this.isOpenTOSCAParam(inputParam.getString("Name"))) {
					inputParam.put("Value",
							this.getOpenTOSCAParamValue(inputParam.getString("Name"), application.getId()));
				}

			}
		}

		// http://192.168.209.199:1337/containerapi/CSARs/MyTinyToDo_Bare_Docker.csar/
		// ServiceTemplates/%257Bhttp%253A%252F%252Fopentosca.org%252Fservicetemplates%257DMyTinyToDo_Bare_Docker/Instances
		String mainServiceTemplateInstancesUrl = this.getMainServiceTemplateURL(application.getId()) + "/Instances";

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
		WebResource referencesResource = this.createWebResource(serviceInstancesResourceUrl);
		boolean serviceInstanceIsAvailable = false;
		String serviceInstanceUrl = "";
		while (!serviceInstanceIsAvailable) {

			// GET Request: check if plan instance was created
			ClientResponse serviceInstancesResponse = referencesResource.accept(MediaType.APPLICATION_JSON)
					.get(ClientResponse.class);

			JSONObject jsonObj = new JSONObject(serviceInstancesResponse.getEntity(String.class));
			int currentCount = jsonObj.getJSONArray("References").length();
			if (currentCount > 1) { // Self + service instance
				JSONArray jsonRefs = jsonObj.getJSONArray("References");

				for (int index = 0; index < jsonRefs.length(); index++) {
					JSONObject jsonRef = jsonRefs.getJSONObject(index);

					if (jsonRef.has("title") && !jsonRef.getString("title").equals("Self")) {
						serviceInstanceUrl = jsonRef.getString("href");
						serviceInstanceIsAvailable = true;
						break;
					}
				}
			}

			try {
				Thread.sleep(10000); // 10 seconds
			} catch (InterruptedException e) {
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
		WebResource planInstanceResource = this.createWebResource(planInstanceUrl);

		boolean instanceFinished = false;
		while (!instanceFinished) {
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			ClientResponse planInstanceResp = planInstanceResource.accept(MediaType.APPLICATION_JSON)
					.get(ClientResponse.class);

			String responseString = planInstanceResp.getEntity(String.class);
			JSONObject planInstanceRespJson = new JSONObject(responseString);
			System.out.println(planInstanceRespJson);

			if (planInstanceRespJson.getJSONObject("PlanInstance").getString("State").equals("finished")) {
				instanceFinished = true;
			}
			// FIXME timeout to break the loop
		}

		Map<String, String> planOutputs = new HashMap<String, String>();
		String planInstanceOutputUrl = serviceInstanceUrl + "/PlanInstances/" + correlationId + "/Output";

		ClientResponse planInstanceOutput = this.createWebResource(planInstanceOutputUrl)
				.accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);

		JSONObject planInstanceOutputJson = new JSONObject(planInstanceOutput.getEntity(String.class));

		JSONArray planOutputParams = planInstanceOutputJson.getJSONArray("OutputParameters");

		for (int index = 0; index < planOutputParams.length(); index++) {
			JSONObject outputParamJson = planOutputParams.getJSONObject(index).getJSONObject("OutputParameter");
			if (outputParamJson.has("Name") & outputParamJson.has("Value")) {
				String name = outputParamJson.getString("Name");
				String value = outputParamJson.getString("Value");
				planOutputs.put(name, value);
			}
		}

		ServiceInstance createdInstance = new ServiceInstance(application.getId(), serviceInstanceUrl,
				this.getServiceInstanceProperties(serviceInstanceUrl));
		// createdInstance.setInputParameters(inputParameters); //FIXME
		createdInstance.setPlanOutputParameters(planOutputs);

		return createdInstance;
	}

	private WebResource createWebResource(final String resourceName) {
		return createWebResource(resourceName, null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.opentosca.containerapi.client.IContainerAPIClient#
	 * getApplicationProperties(java.lang.String)
	 */

	private WebResource createWebResource(final String resourceName, final Map<String, String> queryParamsMap) {
		Client client = Client.create();
		client.addFilter(new LoggingFilter(System.out));

		WebResource webResource = client.resource(resourceName);

		if (queryParamsMap != null) {
			for (Map.Entry<String, String> entry : queryParamsMap.entrySet()) {
				webResource = webResource.queryParam(entry.getKey(), entry.getValue());
			}
		}
		return webResource;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.opentosca.containerapi.client.IContainerAPIClient#deployApplication(
	 * java.lang.String)
	 */
	@Override
	public Application deployApplication(final String filePath) throws Exception {
		String url = this.getContainerAPIUrl() + "/CSARs";
		File fileObj = new File(filePath);
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
			List<String> inputParams = this.getInputParameters(csarName);

			// String name, List<String> inputParameters, List<ServiceInstance>
			// instances, String displayName,
			// String version, String description, String author
			JSONObject appProps = this.getApplicationProperties(csarName);
			Application deployedAplication = new Application(csarName, inputParams, new ArrayList<String>(),
					this.getDisplayName(appProps), this.getVersion(appProps), this.getDescription(appProps),
					this.getAuthor(appProps));

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

	private Element findPropertiesElement(Element element) {
		if (element.getLocalName().equals("Properties")) {
			return element;
		} else {
			NodeList nodeList = element.getChildNodes();

			for (int i = 0; i < nodeList.getLength(); i++) {
				if (nodeList.item(i).getNodeType() == Node.ELEMENT_NODE) {
					Element childElem = (Element) nodeList.item(i);
					return this.findPropertiesElement(element);
				}
			}
		}
		return null;
	}

	private JSONObject getApplicationProperties(final String csarName) {
		// http://localhost:1337/containerapi/CSARs/HomeAssistant_Bare_Docker.csar/MetaData
		String url = this.getContainerAPIUrl() + "/CSARs/" + csarName + "/MetaData";
		ClientResponse resp = this.createWebResource(url).accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
		return new JSONObject(resp);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.opentosca.containerapi.client.IContainerAPIClient#getApplications()
	 */
	@Override
	public List<Application> getApplications() {
		List<String> csarNames = new ArrayList<String>();
		String url = this.getContainerAPIUrl() + "/CSARs";
		ClientResponse resp = this.createWebResource(url).accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);

		JSONObject respJsonObj = new JSONObject(resp.getEntity(String.class));

		JSONArray refArrayJson = respJsonObj.getJSONArray("References");

		for (int index = 0; index < refArrayJson.length(); index++) {
			JSONObject refJsonObj = refArrayJson.getJSONObject(index);
			if (refJsonObj.has("title") && !refJsonObj.getString("title").equals("Self")) {
				csarNames.add(refJsonObj.getString("title"));
			}
		}

		List<Application> apps = new ArrayList<Application>();
		for (String csarName : csarNames) {
			List<String> inputParams = getInputParameters(csarName);

			// String name, List<String> inputParameters, List<ServiceInstance>
			// instances, String displayName,
			// String version, String description, String author
			JSONObject appProps = this.getApplicationProperties(csarName);

			apps.add(new Application(csarName, inputParams, new ArrayList<String>(), this.getDisplayName(appProps),
					this.getVersion(appProps), this.getDescription(appProps), this.getAuthor(appProps)));
		}
		return apps;
	}

	private String getAuthor(JSONObject appProps) {
		if (appProps.has("authors")) {
			return appProps.getString("authors");
		} else {
			return null;
		}
	}

	private String getDescription(JSONObject appProps) {
		if (appProps.has("description")) {
			return appProps.getString("description");
		} else {
			return null;
		}
	}

	private String getDisplayName(JSONObject appProps) {
		if (appProps.has("displayName")) {
			return appProps.getString("displayName");
		} else {
			return null;
		}
	}

	private String getDisplayName(final String csarName) {
		return this.getApplicationProperties(csarName).getString("displayName");
	}

	private List<String> getInputParameters(final String csarName) {
		return this.getPlanInputParameters(csarName, BUILD_PLAN_PATH);
	}

	private Map<String, String> getServiceInstanceProperties(String serviceInstanceId) {
		Map<String, String> properties = new HashMap<String, String>();

		String instancePropertiesURL = serviceInstanceId + "/Properties";
		WebResource instancePropertiesResource = this.createWebResource(instancePropertiesURL);

		ClientResponse instancePropertiesResponse = instancePropertiesResource.accept(MediaType.APPLICATION_JSON)
				.get(ClientResponse.class);

		JSONObject jsonObj = new JSONObject(instancePropertiesResponse.getEntity(String.class));
		JSONArray jsonArrayProperties = jsonObj.getJSONArray("payload");

		for (int index = 0; index < jsonArrayProperties.length(); index++) {

			JSONObject propertyJsonObj = jsonArrayProperties.getJSONObject(index); // property
																					// name
			JSONArray jsonArrayPropertyNames = propertyJsonObj.names();
			for (int indexj = 0; indexj < jsonArrayPropertyNames.length(); indexj++) {
				String key = jsonArrayPropertyNames.getString(indexj);

				String value = propertyJsonObj.getJSONObject(key).getString("TextContent");
				properties.put(key, value);
			}
		}
		return properties;
	}

	private List<ServiceInstance> getInstances(String csarName) {
		List<ServiceInstance> instances = new ArrayList<ServiceInstance>();

		String serviceTemplateURL = this.getMainServiceTemplateURL(csarName);
		WebResource serviceTemplateInstancesResource = this.createWebResource(serviceTemplateURL + "/Instances");

		ClientResponse serviceTemplateInstancesResponse = serviceTemplateInstancesResource
				.accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);

		JSONObject jsonObj = new JSONObject(serviceTemplateInstancesResponse.getEntity(String.class));

		Iterator iter = jsonObj.getJSONArray("References").iterator();

		while (iter.hasNext()) {
			JSONObject obj = (JSONObject) iter.next();
			if (!obj.getString("title").equals("Self")) {
				instances.add(this.getServiceInstance(csarName, obj.getString("href")));
			}
		}

		return instances;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.opentosca.containerapi.client.IContainerAPIClient#
	 * getInstanceProperties(java.lang.String)
	 */

	private String getMainServiceTemplateURL(final String csarName) {
		String url = this.getContainerAPIUrl() + "/CSARs/" + csarName + "/ServiceTemplates";

		Map<String, String> queryParams = new HashMap<String, String>();
		queryParams.put("main", "true");
		WebResource webResource = createWebResource(url, queryParams);
		ClientResponse response = webResource.accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
		String ret = response.getEntity(String.class);
		response.close();

		JSONObject obj = new JSONObject(ret);
		JSONArray referencesArray = obj.getJSONArray("References");
		String href = null;
		for (int i = 0; i < referencesArray.length(); i++) {
			String title = referencesArray.getJSONObject(i).getString("title");
			if (!title.equalsIgnoreCase("Self")) {
				href = referencesArray.getJSONObject(i).getString("href");
				System.out.println(href);
				break;
			}
		}
		// example of href:
		// http://192.168.209.199:1337/containerapi/CSARs/MyTinyToDo_Bare_Docker.csar/
		// ServiceTemplates/%257Bhttp%253A%252F%252Fopentosca.org%252Fservicetemplates%257DMyTinyToDo_Bare_Docker
		return href;
	}

	private NodeInstance getNodeInstanceFromNodeInstanceUrl(String serviceInstanceId, String nodeInstanceUrl) {

		try {
			return new NodeInstance(serviceInstanceId, nodeInstanceUrl, this.getInstanceProperties(nodeInstanceUrl),
					this.getInstanceState(nodeInstanceUrl));
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return new NodeInstance(serviceInstanceId, nodeInstanceUrl, new HashMap<String, String>(),
				this.getInstanceState(nodeInstanceUrl));
	}

	private Map<String, String> getInstanceProperties(String nodeInstanceUrl)
			throws SAXException, IOException, ParserConfigurationException {
		WebResource nodeInstancePropertiesResource = this.createWebResource(nodeInstanceUrl + "/Properties");

		ClientResponse nodeInstancePropertiesResponse = nodeInstancePropertiesResource.accept(MediaType.APPLICATION_XML)
				.get(ClientResponse.class);

		String responseBody = nodeInstancePropertiesResponse.getEntity(String.class);

		DocumentBuilderFactory fac = DocumentBuilderFactory.newInstance();
		fac.setNamespaceAware(true);
		fac.setIgnoringComments(true);

		InputSource sorce = new InputSource(new StringReader(responseBody));

		Document doc = fac.newDocumentBuilder().parse(sorce);

		Element properties = this.findPropertiesElement(doc.getDocumentElement());

		Map<String, String> propMap = this.readPropertiesElementToMap(properties);

		return propMap;
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

	private List<NodeInstance> getNodeInstancesFromNodeTemplateUrl(String serviceInstanceId, String nodeTemplateUrl) {
		List<NodeInstance> nodeInstances = new ArrayList<NodeInstance>();

		WebResource nodeTemplateInstancesResource = this.createWebResource(nodeTemplateUrl + "/Instances");
		ClientResponse nodeTemplateInstancesResponse = nodeTemplateInstancesResource.accept(MediaType.APPLICATION_JSON)
				.get(ClientResponse.class);
		JSONObject jsonObj = new JSONObject(nodeTemplateInstancesResponse.getEntity(String.class));

		Iterator iter = jsonObj.getJSONArray("References").iterator();

		while (iter.hasNext()) {
			JSONObject obj = (JSONObject) iter.next();
			if (!obj.getString("title").equals("Self")) {
				nodeInstances.add(this.getNodeInstanceFromNodeInstanceUrl(serviceInstanceId, obj.getString("href")));
			}
		}

		return nodeInstances;
	}

	private String getInstanceState(String nodeInstanceUrl) {
		WebResource nodeInstanceStateResource = this.createWebResource(nodeInstanceUrl + "/State");
		ClientResponse nodeInstanceStateResponse = nodeInstanceStateResource.accept(MediaType.APPLICATION_JSON)
				.get(ClientResponse.class);

		JSONObject obj = new JSONObject(nodeInstanceStateResponse.getEntity(String.class));

		return obj.getString("state");
	}

	private List<String> getNodeTemplateURLs(ServiceInstance serviceInstance) {
		List<String> urls = new ArrayList<String>();

		WebResource serviceInstanceNodeTemplatesResource = this
				.createWebResource(serviceInstance.getId() + "/NodeTemplates");

		ClientResponse serviceInstanceNodeTemplatesResponse = serviceInstanceNodeTemplatesResource
				.accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);

		JSONObject jsonObj = new JSONObject(serviceInstanceNodeTemplatesResponse.getEntity(String.class));

		Iterator iter = jsonObj.getJSONArray("References").iterator();

		while (iter.hasNext()) {
			JSONObject obj = (JSONObject) iter.next();
			if (!obj.getString("title").equals("Self")) {
				urls.add(obj.getString("href"));
			}
		}

		return urls;
	}

	private String getOpenTOSCAParamValue(String paramName, String csarName) {
		// <instanceDataAPIUrl>http://192.168.59.3:1337/containerapi/CSARs/MyTinyToDo_Bare_Docker.csar/ServiceTemplates/%257Bhttp%253A%252F%252Fopentosca.org%252Fservicetemplates%257DMyTinyToDo_Bare_Docker/Instances/</instanceDataAPIUrl>
		// <CorrelationID>1497362548773-0</CorrelationID>
		// <csarEntrypoint>http://192.168.59.3:1337/containerapi/CSARs/MyTinyToDo_Bare_Docker.csar</csarEntrypoint>
		switch (paramName) {
		case "CorrelationID":
			return String.valueOf(System.currentTimeMillis());
		case "csarEntrypoint":
			return this.containerAPIUrl + "/CSARs/" + csarName;
		case "instanceDataAPIUrl":
			return this.getMainServiceTemplateURL(csarName) + "/Instances/";
		default:
			return null;
		}
	}

	private JSONObject getPlanAsJson(final String csarName, final String planPath) {
		String url = this.getMainServiceTemplateURL(csarName);

		String planParameterUrl = url + planPath;

		WebResource planParameterResource = this.createWebResource(planParameterUrl);
		String jsonResponse = planParameterResource.accept(MediaType.APPLICATION_JSON).get(String.class);

		JSONObject jsonObj = new JSONObject(jsonResponse);

		return jsonObj;
	}

	private List<String> getPlanInputParameters(final String csarName, final String planPath) {
		List<String> paramNames = new ArrayList<String>();
		JSONObject obj = this.getPlanAsJson(csarName, planPath);
		JSONObject planObj = obj.getJSONObject("Plan");
		JSONArray jsonArrayParams = planObj.getJSONArray("InputParameters");

		for (int index = 0; index < jsonArrayParams.length(); index++) {
			JSONObject inputParam = jsonArrayParams.getJSONObject(index);

			paramNames.add(inputParam.getJSONObject("InputParameter").getString("Name"));
		}
		return paramNames;
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

	private List<RelationInstance> getRelationInstancesFromRelationshipTemplateUrl(String serviceInstanceId,
			String relationshipTemplateUrl) {
		List<RelationInstance> relationInstances = new ArrayList<RelationInstance>();

		WebResource nodeTemplateInstancesResource = this.createWebResource(relationshipTemplateUrl + "/Instances");
		ClientResponse nodeTemplateInstancesResponse = nodeTemplateInstancesResource.accept(MediaType.APPLICATION_JSON)
				.get(ClientResponse.class);
		JSONObject jsonObj = new JSONObject(nodeTemplateInstancesResponse.getEntity(String.class));

		Iterator iter = jsonObj.getJSONArray("References").iterator();

		while (iter.hasNext()) {
			JSONObject obj = (JSONObject) iter.next();
			if (!obj.getString("title").equals("Self")) {
				relationInstances
						.add(this.getRelationInstanceFromRelationInstanceUrl(serviceInstanceId, obj.getString("href")));
			}
		}

		return relationInstances;
	}

	private RelationInstance getRelationInstanceFromRelationInstanceUrl(String serviceInstanceId,
			String relationInstanceUrl) {

		try {
			return new RelationInstance(serviceInstanceId, relationInstanceUrl,
					this.getInstanceProperties(relationInstanceUrl), this.getInstanceState(relationInstanceUrl));
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return new RelationInstance(serviceInstanceId, relationInstanceUrl, new HashMap<String, String>(),
				this.getInstanceState(relationInstanceUrl));
	}

	private List<String> getRelationshipTemplateUrls(ServiceInstance serviceInstance) {

		List<String> urls = new ArrayList<String>();

		WebResource serviceInstanceNodeTemplatesResource = this
				.createWebResource(serviceInstance.getId() + "/RelationshipTemplates");

		ClientResponse serviceInstanceNodeTemplatesResponse = serviceInstanceNodeTemplatesResource
				.accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);

		JSONObject jsonObj = new JSONObject(serviceInstanceNodeTemplatesResponse.getEntity(String.class));

		Iterator iter = jsonObj.getJSONArray("References").iterator();

		while (iter.hasNext()) {
			JSONObject obj = (JSONObject) iter.next();
			if (!obj.getString("title").equals("Self")) {
				urls.add(obj.getString("href"));
			}
		}

		return urls;

	}

	private ServiceInstance getServiceInstance(String applicationId, String serviceInstanceUrl) {

		return new ServiceInstance(applicationId, serviceInstanceUrl,
				this.getServiceInstanceProperties(serviceInstanceUrl));
	}

	private String getVersion(JSONObject appProps) {
		if (appProps.has("version")) {
			return appProps.getString("version");
		} else {
			return null;
		}
	}

	private boolean isOpenTOSCAParam(String paramName) {
		return Arrays.asList(this.opentoscaParameters).contains(paramName);
	}

	private Map<String, String> readPropertiesElementToMap(Element propertiesElement) {
		Map<String, String> propMap = new HashMap<String, String>();

		NodeList childNodes = propertiesElement.getChildNodes();

		for (int i = 0; i < childNodes.getLength(); i++) {
			if (childNodes.item(i).getNodeType() == Node.ELEMENT_NODE) {
				Element propChildElement = (Element) childNodes.item(i);

				String key = propChildElement.getLocalName();
				String value = propChildElement.getTextContent();

				propMap.put(key, value);
			}
		}

		return propMap;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.opentosca.containerapi.client.IContainerAPIClient#terminateInstance(
	 * org.opentosca.containerapi.client.Instance)
	 */
	@Override
	public boolean terminateServiceInstance(final ServiceInstance instance) {
		JSONObject planInputJsonObj = this.getPlanAsJson(instance.getApplicationId(), TERMINATE_PLAN_PATH)
				.getJSONObject("Plan");
		System.out.println(planInputJsonObj);

		// fill up the planInput with the given values
		JSONArray inputParamArray = planInputJsonObj.getJSONArray("InputParameters");
		for (int index = 0; index < inputParamArray.length(); index++) {
			JSONObject inputParam = inputParamArray.getJSONObject(index).getJSONObject("InputParameter");

			if (inputParam.getString("Name").equals("OpenTOSCAContainerAPIServiceInstanceID")) {
				inputParam.put("Value", instance.getId());
			} else if (this.isOpenTOSCAParam(inputParam.getString("Name"))) {
				inputParam.put("Value",
						this.getOpenTOSCAParamValue(inputParam.getString("Name"), instance.getApplicationId()));
			}
		}

		// http://192.168.209.199:1337/containerapi/CSARs/MyTinyToDo_Bare_Docker.csar/
		// ServiceTemplates/%257Bhttp%253A%252F%252Fopentosca.org%252Fservicetemplates%257DMyTinyToDo_Bare_Docker/Instances
		String mainServiceTemplateInstancesUrl = this.getMainServiceTemplateURL(instance.getApplicationId())
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
		WebResource referencesResource = this.createWebResource(instance.getId());
		boolean serviceInstanceDeleted = false;
		while (!serviceInstanceDeleted) {

			// GET Request: check if plan instance was created
			ClientResponse serviceInstancesResponse = referencesResource.accept(MediaType.APPLICATION_JSON)
					.get(ClientResponse.class);

			if (serviceInstancesResponse.getStatus() > 400) {
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
		String url = this.getContainerAPIUrl() + "/CSARs/" + application.getId();
		WebResource webResource = createWebResource(url);
		ClientResponse response = webResource.accept(MediaType.TEXT_PLAIN).delete(ClientResponse.class);
		String ret = response.getEntity(String.class);
		response.close();
		return ret;
	}

	@Override
	public ServiceInstance updateServiceInstance(ServiceInstance serviceInstance) {
		return this.getServiceInstance(serviceInstance.getApplicationId(), serviceInstance.getId());
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

}