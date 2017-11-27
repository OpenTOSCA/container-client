package org.opentosca.containerapi.client.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.core.MediaType;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.Node;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.json.JSONArray;
import org.json.JSONObject;
import org.opentosca.containerapi.client.model.Application;
import org.opentosca.containerapi.client.model.Interface;
import org.opentosca.containerapi.client.model.NodeInstance;
import org.opentosca.containerapi.client.model.NodeTemplate;
import org.opentosca.containerapi.client.model.RelationInstance;
import org.opentosca.containerapi.client.model.RelationshipTemplate;
import org.opentosca.containerapi.client.model.ServiceInstance;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.WebResource.Builder;

/**
 * @author francoaa
 *
 */
public abstract class OpenTOSCAContainerInternalAPIClient extends JSONAPIClient {

	URI containerUrl;

	// @hahnml: Remember the external and internal container host to resolve correct
	// URLs in Docker-based
	// environments
	String containerHost = "localhost";
	String containerHostInternal = "localhost";

	@Deprecated
	String legacyContainerAPIUrl = "";

	List<String> filterManagementParameters(List<String> inputParams) {
		List<String> filteredParams = new ArrayList<String>();

		for (String inputParam : inputParams) {
			if (!Arrays.asList(Constants.OPENTOSCACONTAINERAPI_MANAGEMENT_PARAMETERS).contains(inputParam)) {
				filteredParams.add(inputParam);
			}
		}

		return filteredParams;
	}

	boolean arePlansDeployed(Application application) {
		WebResource csarControlResource = this.createWebResource(this.getLegacyContainerAPIUrl()
				+ Constants.OPENTOSCACONTAINERAPI_PATH_CSARCONTROL + application.getId());

		Builder builder = csarControlResource.accept(MediaType.TEXT_PLAIN);

		String result = builder.get(String.class);

		if (result.contains(Constants.OPENTOSCACONTAINERAPI_PLAN_STATE)) {
			return true;
		} else {
			return false;
		}
	}

	private Element findPropertiesElement(Element element) {
		if (element.getLocalName().equals(Constants.OPENTOSCACONTAINERAPI_RESOURCE_XML_PROPERTIES)) {
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

	JSONObject getApplicationProperties(final String csarName) {
		String url = this.getLegacyContainerAPIUrl().replace(Constants.OPENTOSCACONTAINERAPI_PATH_LEGACYAPIROOT, "")
				+ Constants.OPENTOSCACONTAINERAPI_PATH_APIROOT + "/" + csarName;
		return this.getJSONResource(url);
	}

	String getAuthor(JSONObject appProps) {
		if (appProps.has(Constants.OPENTOSCACONTAINERAPI_APPLICATIONRESOURCE_JSON_AUTHORS)) {
			return appProps.getJSONArray(Constants.OPENTOSCACONTAINERAPI_APPLICATIONRESOURCE_JSON_AUTHORS).toString();
		} else {
			return null;
		}
	}

	URI getContainerUrl() {
		return this.containerUrl;
	}

	List<String> getCreateInstanceInputParameters(final String csarName) {
		return this.getInterfaceInputParameters(csarName, Constants.BUILD_PLAN_PATH);
	}

	String getDescription(JSONObject appProps) {
		if (appProps.has(Constants.OPENTOSCACONTAINERAPI_APPLICATIONRESOURE_JSON_DESCRIPTION)) {
			return appProps.getString(Constants.OPENTOSCACONTAINERAPI_APPLICATIONRESOURE_JSON_DESCRIPTION);
		} else {
			return null;
		}
	}

	String getDisplayName(JSONObject appProps) {
		if (appProps.has(Constants.OPENTOSCACONTAINERAPI_APPLICATIONRESOURCE_JSON_DESCRIPTION)) {
			return appProps.getString(Constants.OPENTOSCACONTAINERAPI_APPLICATIONRESOURCE_JSON_DESCRIPTION);
		} else {
			return null;
		}
	}

	String getDisplayName(final String csarName) {
		return this.getApplicationProperties(csarName)
				.getString(Constants.OPENTOSCACONTAINERAPI_APPLICATIONRESOURCE_JSON_DESCRIPTION);
	}

	Map<String, String> getInstanceProperties(String nodeInstanceUrl)
			throws SAXException, IOException, ParserConfigurationException {
		WebResource nodeInstancePropertiesResource = this
				.createWebResource(nodeInstanceUrl + Constants.OPENTOSCACONTAINERAPI_PATH_PROPERTIES);

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

	List<ServiceInstance> getInstances(String csarName) {
		List<ServiceInstance> instances = new ArrayList<ServiceInstance>();

		String serviceTemplateInstancesURL = this.getLegacyMainServiceTemplateURL(csarName)
				+ Constants.OPENTOSCACONTAINERAPI_PATHS_INSTANCES;

		JSONObject jsonObj = this.getJSONResource(serviceTemplateInstancesURL);

		Iterator iter = jsonObj.getJSONArray(Constants.OPENTOSCACONTAINERAPI_REFERENCESRESOURCE_JSON_REFERENCES)
				.iterator();

		while (iter.hasNext()) {
			JSONObject obj = (JSONObject) iter.next();
			if (!obj.getString(Constants.OPENTOSCACONTAINERAPI_REFERENCESRESOURCE_JSON_TITLE)
					.equals(Constants.OPENTOSCACONTAIENRAPI_REFERENCESRESOURCE_JSON_TITLE_SELF)) {

				// @hahnml: Resolve the internal host in the URL to the external one
				instances.add(this.getServiceInstance(csarName,
						resolveUrl(obj.getString(Constants.OPENTOSCACONTAINERAPI_REFERENCESRESOURCE_JSON_HREF),
								this.containerHost)));
			}
		}

		return instances;
	}

	String getInstanceState(String nodeInstanceUrl) {
		WebResource nodeInstanceStateResource = this
				.createWebResource(nodeInstanceUrl + Constants.OPENTOSCACONTAINERAPI_PATH_STATE);
		ClientResponse nodeInstanceStateResponse = nodeInstanceStateResource.accept(MediaType.APPLICATION_JSON)
				.get(ClientResponse.class);

		JSONObject obj = new JSONObject(nodeInstanceStateResponse.getEntity(String.class));

		return obj.getString(Constants.OPENTOSCACONTAINERAPI_SERVICEINSTANCERESOURCE_JSON_STATE);
	}

	private List<String> getInterfaceInputParameters(final String csarName, final String planPath) {
		List<String> paramNames = new ArrayList<String>();
		JSONObject obj = this.getPlanAsJson(csarName, planPath);
		JSONObject planObj = obj.getJSONObject(Constants.OPENTOSCACONTAINERAPI_INTERFACERESOURCE_JSON_PLAN);
		JSONArray jsonArrayParams = planObj
				.getJSONArray(Constants.OPENTOSCACONTAINERAPI_INTERFACERESOURCE_JSON_INPUTPARAMS);

		for (int index = 0; index < jsonArrayParams.length(); index++) {
			JSONObject inputParam = jsonArrayParams.getJSONObject(index);

			paramNames.add(inputParam.getJSONObject(Constants.OPENTOSCACONTAINERAPI_INTERFACERESOURCE_JSON_INPUTPARAM)
					.getString(Constants.OPENTOSCACONTAINERAPI_INTERFACERESOURCE_JSON_NAME));
		}
		return paramNames;
	}

	private List<String> getInterfaceInputParameters(URI uri) {
		List<String> paramNames = new ArrayList<String>();
		JSONObject obj = this.getJSONResource(uri);
		JSONObject planObj = obj.getJSONObject(Constants.OPENTOSCACONTAINERAPI_INTERFACERESOURCE_JSON_PLAN);
		JSONArray jsonArrayParams = planObj
				.getJSONArray(Constants.OPENTOSCACONTAINERAPI_INTERFACERESOURCE_JSON_INPUTPARAMS);

		for (int index = 0; index < jsonArrayParams.length(); index++) {
			JSONObject inputParam = jsonArrayParams.getJSONObject(index);

			paramNames.add(inputParam.getJSONObject(Constants.OPENTOSCACONTAINERAPI_INTERFACERESOURCE_JSON_INPUTPARAM)
					.getString(Constants.OPENTOSCACONTAINERAPI_INTERFACERESOURCE_JSON_NAME));
		}
		return paramNames;
	}

	List<String> getInterfaceNames(final String csarName) {
		String serviceTemplateInterfacesResourceUrl = this.getInterfacesUrl(csarName);
		List<String> names = new ArrayList<String>();
		JSONObject respJson = this.getJSONResource(serviceTemplateInterfacesResourceUrl);
		this.getReferencesFromLegacyApiResource(respJson,
				Constants.OPENTOSCACONTAIENRAPI_REFERENCESRESOURCE_JSON_TITLE_SELF)
				.forEach(x -> names.add(this.getLastPathSegment(URI.create(x))));
		return names;
	}

	private List<String> getInterfaceOutputParameters(URI uri) {
		List<String> paramNames = new ArrayList<String>();
		JSONObject obj = this.getJSONResource(uri);
		JSONObject planObj = obj.getJSONObject(Constants.OPENTOSCACONTAINERAPI_INTERFACERESOURCE_JSON_PLAN);
		JSONArray jsonArrayParams = planObj
				.getJSONArray(Constants.OPENTOSCACONTAINERAPI_INTERFACERESOURCE_JSON_OUTPUTPARAMS);

		for (int index = 0; index < jsonArrayParams.length(); index++) {
			JSONObject inputParam = jsonArrayParams.getJSONObject(index);

			paramNames.add(inputParam.getJSONObject(Constants.OPENTOSCACONTAINERAPI_INTERFACERESOURCE_JSON_OUTPUTPARAM)
					.getString(Constants.OPENTOSCACONTAINERAPI_INTERFACERESOURCE_JSON_NAME));
		}
		return paramNames;
	}

	List<Interface> getInterfaces(final String csarName) {
		List<Interface> interfaces = new ArrayList<Interface>();

		List<String> names = this.getInterfaceNames(csarName);

		for (String interfaceName : names) {
			Map<String, List<String>> operations2InputParameters = new HashMap<String, List<String>>();
			Map<String, List<String>> operations2OutputParameters = new HashMap<String, List<String>>();

			for (URI operationUrl : this.getOperationsUrls(csarName, interfaceName)) {
				// we'll check only for plans
				String operationName = this.getLastPathSegment(operationUrl);
				List<String> inputParams = this
						.getInterfaceInputParameters(this.getPlanOperationUrl(operationUrl.toString()));
				List<String> outputParams = this
						.getInterfaceOutputParameters(this.getPlanOperationUrl(operationUrl.toString()));

				operations2InputParameters.put(operationName, inputParams);
				operations2OutputParameters.put(operationName, outputParams);
			}

			interfaces.add(new Interface(interfaceName, operations2InputParameters, operations2OutputParameters));
		}
		return interfaces;
	}

	private String getInterfacesUrl(final String csarName) {
		return this.getLegacyMainServiceTemplateURL(csarName)
				+ Constants.OPENTOSCACONTAINERAPI_PATHS_BOUNDARYINTERFACES;
	}

	protected String getInterfaceUrl(final String csarName, final String interfaceName) {
		return this.getInterfacesUrl(csarName) + "/" + interfaceName;
	}

	@Deprecated
	protected String getLegacyContainerAPIUrl() {
		return legacyContainerAPIUrl;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.opentosca.containerapi.client.IContainerAPIClient#
	 * getInstanceProperties(java.lang.String)
	 */
	@Deprecated
	String getLegacyMainServiceTemplateURL(final String csarName) {
		String url = this.getLegacyContainerAPIUrl() + Constants.OPENTOSCACONTAINERAPI_PATH_CSARS + "/" + csarName
				+ Constants.OPENTOSCACONTAIENRAPI_PATH_SERVICETEMPLATES;

		Map<String, String> queryParams = new HashMap<String, String>();
		queryParams.put("main", "true");
		WebResource webResource = createWebResource(url, queryParams, true);
		ClientResponse response = webResource.accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
		String ret = response.getEntity(String.class);
		response.close();

		JSONObject obj = new JSONObject(ret);
		JSONArray referencesArray = obj
				.getJSONArray(Constants.OPENTOSCACONTAINERAPI_REFERENCESRESOURCE_JSON_REFERENCES);
		String href = null;
		for (int i = 0; i < referencesArray.length(); i++) {
			String title = referencesArray.getJSONObject(i)
					.getString(Constants.OPENTOSCACONTAINERAPI_REFERENCESRESOURCE_JSON_TITLE);
			if (!title.equalsIgnoreCase(Constants.OPENTOSCACONTAIENRAPI_REFERENCESRESOURCE_JSON_TITLE_SELF)) {
				href = referencesArray.getJSONObject(i)
						.getString(Constants.OPENTOSCACONTAINERAPI_REFERENCESRESOURCE_JSON_HREF);
				System.out.println(href);
				break;
			}
		}
		// example of href:
		// http://192.168.209.199:1337/containerapi/CSARs/MyTinyToDo_Bare_Docker.csar/
		// ServiceTemplates/%257Bhttp%253A%252F%252Fopentosca.org%252Fservicetemplates%257DMyTinyToDo_Bare_Docker
		return href;
	}

	String createServiceTemplatesUrl(final String csarName) {
		return this.getContainerUrl().toString() + Constants.OPENTOSCACONTAINERAPI_PATHS_CSARS + csarName
				+ Constants.OPENTOSCACONTAINERAPI_PATH_SERVICETEMPLATES;
	}

	String getMainServiceTemplateURL(final String csarName) {
		String serviceTemplatesResourceUrl = this.createServiceTemplatesUrl(csarName);
		return this.getJSONResource(serviceTemplatesResourceUrl)
				.getJSONArray(Constants.OPENTOSCACONTAINERAPI_SERVICETEMPLATESRESOURCE_JSON_SERVICETEMPLATES)
				.getJSONObject(0).getJSONObject(Constants.OPENTOSCACONTAINERAPI_SERVICETEMAPLTESRESOURCE_JSON_LINKS)
				.getJSONObject(Constants.OPENTOSCACONTAIENRAPI_REFERENCESRESOURCE_JSON_TITLE_SELF_LOWER)
				.getString(Constants.OPENTOSCACONTAINERAPI_REFERENCESRESOURCE_JSON_HREF);
	}

	NodeInstance getNodeInstanceFromNodeInstanceUrl(String serviceInstanceId, String nodeInstanceUrl) {
		try {
			return new NodeInstance(serviceInstanceId, nodeInstanceUrl, this.getInstanceProperties(nodeInstanceUrl),
					this.getInstanceState(nodeInstanceUrl));
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
		return new NodeInstance(serviceInstanceId, nodeInstanceUrl, new HashMap<String, String>(),
				this.getInstanceState(nodeInstanceUrl));
	}

	List<NodeInstance> getNodeInstancesFromNodeTemplateUrl(String serviceInstanceId, String nodeTemplateUrl) {
		List<NodeInstance> nodeInstances = new ArrayList<NodeInstance>();

		WebResource nodeTemplateInstancesResource = this
				.createWebResource(nodeTemplateUrl + Constants.OPENTOSCACONTAINERAPI_PATHS_INSTANCES);
		ClientResponse nodeTemplateInstancesResponse = nodeTemplateInstancesResource.accept(MediaType.APPLICATION_JSON)
				.get(ClientResponse.class);
		JSONObject jsonObj = new JSONObject(nodeTemplateInstancesResponse.getEntity(String.class));

		Iterator iter = jsonObj.getJSONArray(Constants.OPENTOSCACONTAINERAPI_REFERENCESRESOURCE_JSON_REFERENCES)
				.iterator();

		while (iter.hasNext()) {
			JSONObject obj = (JSONObject) iter.next();
			if (!obj.getString(Constants.OPENTOSCACONTAINERAPI_REFERENCESRESOURCE_JSON_TITLE)
					.equals(Constants.OPENTOSCACONTAIENRAPI_REFERENCESRESOURCE_JSON_TITLE_SELF)) {
				nodeInstances.add(this.getNodeInstanceFromNodeInstanceUrl(serviceInstanceId,
						obj.getString(Constants.OPENTOSCACONTAINERAPI_REFERENCESRESOURCE_JSON_HREF)));
			}
		}

		return nodeInstances;
	}

	List<String> getNodeTemplateURLs(ServiceInstance serviceInstance) {
		List<String> urls = new ArrayList<String>();

		WebResource serviceInstanceNodeTemplatesResource = this
				.createWebResource(serviceInstance.getURL() + Constants.OPENTOSCACONTAINERAPI_PATHS_NODETEMPLATES);

		ClientResponse serviceInstanceNodeTemplatesResponse = serviceInstanceNodeTemplatesResource
				.accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);

		JSONObject jsonObj = new JSONObject(serviceInstanceNodeTemplatesResponse.getEntity(String.class));

		Iterator iter = jsonObj.getJSONArray(Constants.OPENTOSCACONTAINERAPI_REFERENCESRESOURCE_JSON_REFERENCES)
				.iterator();

		while (iter.hasNext()) {
			JSONObject obj = (JSONObject) iter.next();
			if (!obj.getString(Constants.OPENTOSCACONTAINERAPI_REFERENCESRESOURCE_JSON_TITLE)
					.equals(Constants.OPENTOSCACONTAIENRAPI_REFERENCESRESOURCE_JSON_TITLE_SELF)) {
				urls.add(obj.getString(Constants.OPENTOSCACONTAINERAPI_REFERENCESRESOURCE_JSON_HREF));
			}
		}

		return urls;
	}

	String getOpenTOSCAParamValue(String paramName, String csarName) {
		// <instanceDataAPIUrl>http://192.168.59.3:1337/containerapi/CSARs/MyTinyToDo_Bare_Docker.csar/ServiceTemplates/%257Bhttp%253A%252F%252Fopentosca.org%252Fservicetemplates%257DMyTinyToDo_Bare_Docker/Instances/</instanceDataAPIUrl>
		// <CorrelationID>1497362548773-0</CorrelationID>
		// <csarEntrypoint>http://192.168.59.3:1337/containerapi/CSARs/MyTinyToDo_Bare_Docker.csar</csarEntrypoint>
		switch (paramName) {
		case "CorrelationID":
			return String.valueOf(System.currentTimeMillis());
		case "csarEntrypoint":
			return this.legacyContainerAPIUrl + Constants.OPENTOSCACONTAINERAPI_PATHS_LEGACYCSARS + csarName;
		case "instanceDataAPIUrl":
			return this.getLegacyMainServiceTemplateURL(csarName) + Constants.OPENTOSCACONTAINERAPI_PATHS_INSTANCES
					+ "/";
		default:
			return null;
		}
	}

	protected URI getOperationsUri(final String csarName, final String interfaceName) {
		return URI.create(
				this.getInterfaceUrl(csarName, interfaceName) + Constants.OPENTOSCACONTAINERAPI_PATHS_OPERATIONS);
	}

	protected URI getOperationUrl(final String csarName, final String interfaceName, final String operationName) {
		for (URI opUri : this.getOperationsUrls(csarName, interfaceName)) {
			if (this.getLastPathSegment(opUri).equals(operationName)) {
				return opUri;
			}
		}
		return null;
	}

	protected List<URI> getOperationsUrls(final String csarName, final String interfaceName) {
		URI interfaceOperationsResourceUrl = this.getOperationsUri(csarName, interfaceName);
		JSONObject jsonResp = this.getJSONResource(interfaceOperationsResourceUrl);
		List<URI> result = new ArrayList<>();
		this.getReferencesFromLegacyApiResource(jsonResp,
				Constants.OPENTOSCACONTAIENRAPI_REFERENCESRESOURCE_JSON_TITLE_SELF)
				.forEach(x -> result.add((URI.create(x))));
		return result;
	}

	JSONObject getPlanAsJson(final String csarName, final String planPath) {
		String url = this.getLegacyMainServiceTemplateURL(csarName);

		String planParameterUrl = url + planPath;

		WebResource planParameterResource = this.createWebResource(planParameterUrl);
		String jsonResponse = planParameterResource.accept(MediaType.APPLICATION_JSON).get(String.class);

		JSONObject jsonObj = new JSONObject(jsonResponse);

		return jsonObj;
	}

	JSONObject getPlanAsJson(final String planUrl) {
		WebResource planParameterResource = this.createWebResource(planUrl);
		String jsonResponse = planParameterResource.accept(MediaType.APPLICATION_JSON).get(String.class);

		JSONObject jsonObj = new JSONObject(jsonResponse);

		return jsonObj;

	}

	private URI getPlanOperationUrl(String operationUrl) {
		return URI.create(operationUrl + Constants.OPENTOSCACONTAINERAPI_PATHS_PLAN);
	}

	@Deprecated
	private List<String> getReferencesFromLegacyApiResource(final JSONObject jsonObject, final String... titleExcept) {
		List<String> references = new ArrayList<>();
		JSONArray jsonArray = jsonObject
				.getJSONArray(Constants.OPENTOSCACONTAINERAPI_REFERENCESRESOURCE_JSON_REFERENCES);

		if (titleExcept == null || titleExcept.length == 0) {
			// return all
			jsonArray.forEach(o -> references
					.add(((JSONObject) o).getString(Constants.OPENTOSCACONTAINERAPI_REFERENCESRESOURCE_JSON_TITLE)));
			return references;
		}

		for (int index = 0; index < jsonArray.length(); index++) {
			String title = jsonArray.getJSONObject(index)
					.getString(Constants.OPENTOSCACONTAINERAPI_REFERENCESRESOURCE_JSON_TITLE);
			if (!Arrays.asList(titleExcept).contains(title)) {
				references.add(jsonArray.getJSONObject(index)
						.getString(Constants.OPENTOSCACONTAINERAPI_REFERENCESRESOURCE_JSON_HREF));
			}
		}
		return references;
	}

	RelationInstance getRelationInstanceFromRelationInstanceUrl(String serviceInstanceId, String relationInstanceUrl) {

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

	List<RelationInstance> getRelationInstancesFromRelationshipTemplateUrl(String serviceInstanceId,
			String relationshipTemplateUrl) {
		List<RelationInstance> relationInstances = new ArrayList<RelationInstance>();

		WebResource nodeTemplateInstancesResource = this
				.createWebResource(relationshipTemplateUrl + Constants.OPENTOSCACONTAINERAPI_PATHS_INSTANCES);
		ClientResponse nodeTemplateInstancesResponse = nodeTemplateInstancesResource.accept(MediaType.APPLICATION_JSON)
				.get(ClientResponse.class);
		JSONObject jsonObj = new JSONObject(nodeTemplateInstancesResponse.getEntity(String.class));

		Iterator iter = jsonObj.getJSONArray(Constants.OPENTOSCACONTAINERAPI_REFERENCESRESOURCE_JSON_REFERENCES)
				.iterator();

		while (iter.hasNext()) {
			JSONObject obj = (JSONObject) iter.next();
			if (!obj.getString(Constants.OPENTOSCACONTAINERAPI_REFERENCESRESOURCE_JSON_TITLE)
					.equals(Constants.OPENTOSCACONTAIENRAPI_REFERENCESRESOURCE_JSON_TITLE_SELF)) {
				relationInstances.add(this.getRelationInstanceFromRelationInstanceUrl(serviceInstanceId,
						obj.getString(Constants.OPENTOSCACONTAINERAPI_REFERENCESRESOURCE_JSON_HREF)));
			}
		}

		return relationInstances;
	}

	List<String> getRelationshipTemplateUrls(ServiceInstance serviceInstance) {

		List<String> urls = new ArrayList<String>();

		WebResource serviceInstanceNodeTemplatesResource = this.createWebResource(
				serviceInstance.getURL() + Constants.OPENTOSCACONTAINERAPI_PATHS_RELATIONSHIPTEMPLATES);

		ClientResponse serviceInstanceNodeTemplatesResponse = serviceInstanceNodeTemplatesResource
				.accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);

		JSONObject jsonObj = new JSONObject(serviceInstanceNodeTemplatesResponse.getEntity(String.class));

		Iterator iter = jsonObj.getJSONArray(Constants.OPENTOSCACONTAINERAPI_REFERENCESRESOURCE_JSON_REFERENCES)
				.iterator();

		while (iter.hasNext()) {
			JSONObject obj = (JSONObject) iter.next();
			if (!obj.getString(Constants.OPENTOSCACONTAINERAPI_REFERENCESRESOURCE_JSON_TITLE)
					.equals(Constants.OPENTOSCACONTAIENRAPI_REFERENCESRESOURCE_JSON_TITLE_SELF)) {
				urls.add(obj.getString(Constants.OPENTOSCACONTAINERAPI_REFERENCESRESOURCE_JSON_HREF));
			}
		}

		return urls;

	}

	ServiceInstance getServiceInstance(String applicationId, String serviceInstanceUrl) {
		return new ServiceInstance(applicationId, serviceInstanceUrl,
				this.getServiceInstanceProperties(serviceInstanceUrl), this.getServiceInstanceState(serviceInstanceUrl),
				new HashMap<String, String>());
	}

	Map<String, String> getServiceTemplateProperties(Application application) {
		Map<String, String> properties = new HashMap<String, String>();

		String serviceTemplatesUrl = this.createServiceTemplatesUrl(application.getId());

		String propertiesUrl = null;
		try {
			propertiesUrl = serviceTemplatesUrl + "/"
					+ URLEncoder.encode(URLEncoder.encode(this.getServiceTemplateId(application).toString(), "UTF-8"))
					+ Constants.OPENTOSCACONTAIENRAPI_PATH_BOUNDARYDEFS
					+ Constants.OPENTOSCACONTAIENRAPI_PATH_PROPERTIES;
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		JSONObject jsonObj = this.getJSONResource(propertiesUrl);

		if (jsonObj.has("xml_fragment")) {

			String xmlFragment = this.getJSONResource(propertiesUrl).getString("xml_fragment");

			Element propRootElement = this.parseStringToDom(xmlFragment);

			if (propRootElement != null && propRootElement.getLocalName().equals("Properties")) {
				NodeList childNodes = propRootElement.getChildNodes();

				for (int index = 0; index < childNodes.getLength(); index++) {
					if (childNodes.item(index).getNodeType() == Node.ELEMENT_NODE
							&& !childNodes.item(index).getLocalName().equals("PropertyMappings")) {
						Element propertyElement = (Element) childNodes.item(index);
						properties.put(propertyElement.getLocalName(), propertyElement.getTextContent());
					}
				}
			}
		}

		return properties;
	}

	Element parseStringToDom(String xmlString) {
		Document doc = null;
		try {
			DocumentBuilderFactory docFac = DocumentBuilderFactory.newInstance();
			docFac.setNamespaceAware(true);
			doc = docFac.newDocumentBuilder().parse(new InputSource(new StringReader(xmlString)));
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
		if (doc != null) {
			return doc.getDocumentElement();
		} else {
			return null;
		}
	}

	Map<String, String> getServiceInstanceProperties(String serviceInstanceId) {
		Map<String, String> properties = new HashMap<String, String>();

		String instancePropertiesURL = serviceInstanceId + Constants.OPENTOSCACONTAINERAPI_PATHS_PROPERTIES;

		JSONObject jsonObj = this.getJSONResource(instancePropertiesURL);

		JSONArray jsonArrayProperties = jsonObj
				.getJSONArray(Constants.OPENTOSCACONTAINERAPI_PROPERTIESRESOURCE_PAYLOAD);

		for (int index = 0; index < jsonArrayProperties.length(); index++) {

			JSONObject propertyJsonObj = jsonArrayProperties.getJSONObject(index); // property
																					// name
			JSONArray jsonArrayPropertyNames = propertyJsonObj.names();
			for (int indexj = 0; indexj < jsonArrayPropertyNames.length(); indexj++) {
				String key = jsonArrayPropertyNames.getString(indexj);

				String value = propertyJsonObj.getJSONObject(key)
						.getString(Constants.OPENTOSCACONTAINERAPI_PROPERTIESRESOURCE_TEXTCONTENT);
				properties.put(key, value);
			}
		}
		return properties;
	}

	String getServiceInstanceState(String serviceInstanceUrl) {
		String instanceStateURL = serviceInstanceUrl + Constants.OPENTOSCACONTAINERAPI_PATH_STATE;

		JSONObject jsonObj = this.getJSONResource(instanceStateURL);

		return jsonObj.getString(Constants.OPENTOSCACONTAINERAPI_SERVICEINSTANCERESOURCE_JSON_STATE);
	}

	String getVersion(JSONObject appProps) {
		if (appProps.has(Constants.OPENTOSCACONTAINERAPI_APPLICATIONRESOURCE_VERSION)) {
			return appProps.getString(Constants.OPENTOSCACONTAINERAPI_APPLICATIONRESOURCE_VERSION);
		} else {
			return null;
		}
	}

	boolean isOpenTOSCAParam(String paramName) {
		return Arrays.asList(Constants.OPENTOSCACONTAINERAPI_MANAGEMENT_PARAMETERS).contains(paramName);
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

	List<RelationshipTemplate> getRelationshipTemplates(Application application) {
		QName serviceTemplateId = this.getServiceTemplateId(application);

		List<RelationshipTemplate> relationshipTemplates = new ArrayList<RelationshipTemplate>();

		String definitionsXmlString = this.getServiceTemplateXML(application);
		Element definitionsRootElement = this.parseStringToDom(definitionsXmlString);

		try {
			NodeList relationshipTemplateElementList = this.queryNodes(definitionsRootElement,
					"//*[local-name()='RelationshipTemplate']");

			for (int index = 0; index < relationshipTemplateElementList.getLength(); index++) {
				if (relationshipTemplateElementList.item(index).getNodeType() == Node.ELEMENT_NODE) {
					Element relationshipTemplateElement = (Element) relationshipTemplateElementList.item(index);

					String type = relationshipTemplateElement.getAttribute("type");
					String[] typeSplit = type.split(":");
					String prefix = typeSplit[0];
					String localName = typeSplit[1];
					String namespace = relationshipTemplateElement.getAttribute("xmlns:" + prefix);

					NodeList childNodes = relationshipTemplateElement.getChildNodes();

					String sourceNode = null;
					String targetNode = null;
					for (int index2 = 0; index2 < childNodes.getLength(); index2++) {
						if (childNodes.item(index2).getNodeType() == Node.ELEMENT_NODE) {
							Element childNode = (Element) childNodes.item(index2);
							switch (childNode.getLocalName()) {
							case "SourceElement":
								sourceNode = childNode.getAttribute("ref");
								break;
							case "TargetElement":
								targetNode = childNode.getAttribute("ref");
								break;
							}
						}
					}

					relationshipTemplates.add(new RelationshipTemplate(relationshipTemplateElement.getAttribute("id"),
							serviceTemplateId, new QName(namespace, localName), sourceNode, targetNode));
				}
			}
		} catch (XPathExpressionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return relationshipTemplates;

	}

	List<NodeTemplate> getNodeTemplates(Application application) {
		QName serviceTemplateId = this.getServiceTemplateId(application);

		List<NodeTemplate> nodeTemplates = new ArrayList<NodeTemplate>();

		String definitionsXmlString = this.getServiceTemplateXML(application);
		Element definitionsRootElement = this.parseStringToDom(definitionsXmlString);

		try {
			NodeList nodeTemplateElementList = this.queryNodes(definitionsRootElement,
					"//*[local-name()='NodeTemplate']");

			for (int index = 0; index < nodeTemplateElementList.getLength(); index++) {
				if (nodeTemplateElementList.item(index).getNodeType() == Node.ELEMENT_NODE) {
					Element nodeTemplateElement = (Element) nodeTemplateElementList.item(index);

					String type = nodeTemplateElement.getAttribute("type");
					String[] typeSplit = type.split(":");
					String prefix = typeSplit[0];
					String localName = typeSplit[1];

					String namespace = nodeTemplateElement.getAttribute("xmlns:" + prefix);
					nodeTemplates.add(new NodeTemplate(nodeTemplateElement.getAttribute("id"), serviceTemplateId,
							new QName(namespace, localName)));
				}
			}
		} catch (XPathExpressionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return nodeTemplates;
	}

	QName getServiceTemplateId(Application application) {
		JSONObject serviceTemplatesJsonResp = this.getJSONResource(this.createServiceTemplatesUrl(application.getId()));
		JSONArray serviceTemplatesJsonArray = serviceTemplatesJsonResp
				.getJSONArray(Constants.OPENTOSCACONTAINERAPI_SERVICETEMPLATESRESOURCE_JSON_SERVICETEMPLATES);
		return QName.valueOf(serviceTemplatesJsonArray.getJSONObject(0).getString("id"));
	}

	String getServiceTemplateXML(Application application) {
		String definitionsUrl = this.createServiceTemplateDefinitionsUrl(application);
		String definitionsContent = this.getOctetStreamResource(definitionsUrl);
		return definitionsContent;
	}

	Node queryNode(Element rootElement, String query) throws XPathExpressionException {
		XPath xPath = XPathFactory.newInstance().newXPath();
		XPathExpression expression = xPath.compile(query);
		Node node = (Node) expression.evaluate(rootElement, XPathConstants.NODE);
		return node;
	}

	NodeList queryNodes(Element rootElement, String query) throws XPathExpressionException {
		XPath xPath = XPathFactory.newInstance().newXPath();
		XPathExpression expression = xPath.compile(query);
		NodeList nodes = (NodeList) expression.evaluate(rootElement, XPathConstants.NODESET);
		return nodes;
	}

	String getOctetStreamResource(String url) {
		WebResource octetStreamResource = this.createWebResource(url);

		ClientResponse instancePropertiesResponse = octetStreamResource.accept(MediaType.APPLICATION_OCTET_STREAM)
				.get(ClientResponse.class);
		return instancePropertiesResponse.getEntity(String.class);
	}

	String createServiceTemplateDefinitionsUrl(Application application) {
		QName serviceTemplateId = this.getServiceTemplateId(application);
		return this.getContainerUrl().toString() + Constants.OPENTOSCACONTAINERAPI_PATHS_CSARS + application.getId()
				+ Constants.OPENTOSCACONTAIENRAPI_PATH_CONTENT
				+ Constants.OPENTOSCACONTAIENRAPI_PATH_CONTENT_DEFINITIONS + "/servicetemplates__"
				+ serviceTemplateId.getLocalPart() + ".tosca";

	}

	// @hahnml: Replace the container host in an URL with the actual host name. In
	// Docker-based environments it depends from where the URL is resolved, inside a
	// network of docker containers (use "containerHostInternal" as hostName) or
	// from the outside ((use "containerHost" as hostName)), e.g., by this client.
	String resolveUrl(String url, String hostName) {
		String result = url;

		// Support resolution of both DNS and IP-based host names, e.g.,
		// "http://container:1337/containerapi/..." or
		// "http://192.168.132.104:1337/containerapi/..."
		Pattern pattern = Pattern.compile("http://(\\w+|\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}):1337.*");
		Matcher matcher = pattern.matcher(url);

		if (matcher.find()) {
			result = result.replaceFirst(matcher.group(1), hostName);
		}

		return result;
	}

	@Deprecated
	protected void setLegacyContainerAPIUrl(final String containerAPIUrl) {
		this.legacyContainerAPIUrl = containerAPIUrl;
	}

	OutputStream getApplicationContent(final String path) {
		String url = this.getLegacyContainerAPIUrl().replace(Constants.OPENTOSCACONTAINERAPI_PATH_LEGACYAPIROOT, "")
				+ Constants.OPENTOSCACONTAINERAPI_PATH_APIROOT + "/" + path;
		return this.getFileResource(URI.create(url));
	}
}