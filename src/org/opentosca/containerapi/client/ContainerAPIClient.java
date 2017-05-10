package org.opentosca.containerapi.client;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.json.JSONArray;
import org.json.JSONObject;

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
public class ContainerAPIClient {

	public static String BUILD_PLAN_PATH = "/BoundaryDefinitions/Interfaces/OpenTOSCA-Lifecycle-Interface/Operations/initiate/Plan";

	private String containerAPIUrl = "";

	public ContainerAPIClient() {
		this.containerAPIUrl = "http://localhost:1337/containerapi";
	}
	
	public ContainerAPIClient(final String containerHost) {
		this.containerAPIUrl = "http://" + containerHost + ":1337/containerapi";
	}
	
	public String getContainerAPIUrl() {
		return containerAPIUrl;
	}

	public void setContainerAPIUrl(final String containerAPIUrl) {
		this.containerAPIUrl = containerAPIUrl;
	}

	/**
	 * gets a list of installed applications
	 * 
	 * @return
	 */
	public List<Application> getApplications() {
		List<String> csarNames = new ArrayList<String>();
		String url = this.getContainerAPIUrl() + "/CSARs";
		ClientResponse resp = this.createWebResource(url).accept(MediaType.APPLICATION_JSON)
				.get(ClientResponse.class);

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
			apps.add(new Application(csarName, inputParams));
		}
		return apps;
	}
	
	/**
	 * Gets application metadata (i.e., display name, description, version, authors)
	 * @param csarName
	 * @return Application Metadata as a JSON object
	 */
	public JSONObject getApplicationProperties(final String csarName) {
		// http://localhost:1337/containerapi/CSARs/HomeAssistant_Bare_Docker.csar/MetaData
		String url = this.getContainerAPIUrl() + "/CSARs/" + csarName + "/MetaData";
		ClientResponse resp = this.createWebResource(url).accept(MediaType.APPLICATION_JSON)
				.get(ClientResponse.class);
		return new JSONObject(resp);
	}
	
	public String getDisplayName(final String csarName) {
		return this.getApplicationProperties(csarName).getString("displayName");
	}

	public String getVersion(final String csarName) {
		return this.getApplicationProperties(csarName).getString("version");
	}

	public String getDescription(final String csarName) {
		return this.getApplicationProperties(csarName).getString("description");
	}

	public String getAuthor(final String csarName) {
		return this.getApplicationProperties(csarName).getString("authors");
	}

	/**
	 * 
	 * deploys an Application (CSAR file) onto the OpenTosca ecosystem
	 * 
	 * @param filePath
	 * @return Application object or null if upload failed
	 * @throws Exception
	 */
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
			Application deployedAplication = new Application(csarName, inputParams);
			return deployedAplication;

		} else {
			System.out.println("Upload not possible");
			return null;
		}
	}
	
	

	/**
	 * deletes an Application (CSAR file) onto the OpenTosca ecosystem 
	 * 
	 * @param csarName application name
	 * @return
	 */
	public String undeployApplication(final Application application) {
		String url = this.getContainerAPIUrl() + "/CSARs/" + application.getName();
		WebResource webResource = createWebResource(url);
		ClientResponse response = webResource.accept(MediaType.TEXT_PLAIN).delete(ClientResponse.class);
		String ret = response.getEntity(String.class);
		response.close();
		return ret;
	}

	/**
	 * gets required input parameters for creating an instance of the Application
	 * @param csarName
	 * @return list of the parameters
	 * 
	 */
	public List<String> getInputParameters(final String csarName) {
		List<String> paramNames = new ArrayList<String>();
		JSONObject obj = this.getBuildPlanAsJson(csarName);
		JSONObject planObj = obj.getJSONObject("Plan");
		JSONArray jsonArrayParams = planObj.getJSONArray("InputParameters");

		for (int index = 0; index < jsonArrayParams.length(); index++) {
			JSONObject inputParam = jsonArrayParams.getJSONObject(index);
			
			paramNames.add(inputParam.getJSONObject("InputParameter").getString("Name"));
		}
		return paramNames;
	}

	/**
	 * creates an instance of the application
	 * @param csarName
	 * @param params required parameters to provision the application
	 * @return an Instance object
	 */
	public Instance createInstance(final String csarName, final Map<String, String> params) {

		JSONObject planInputJsonObj = this.getBuildPlanInputJsonObject(csarName);

		// fill up the planInput with the given values
		if (params != null && !params.isEmpty()) {
			JSONArray inputParamArray = planInputJsonObj.getJSONArray("InputParameters");
			for (int index = 0; index < inputParamArray.length(); index++) {

				JSONObject inputParam = inputParamArray.getJSONObject(index).getJSONObject("InputParameter");
				if (params.containsKey(inputParam.getString("Name"))) {
					inputParam.put("Value", params.get(inputParam.getString("Name")));
				}
			}
		}

		// http://192.168.209.199:1337/containerapi/CSARs/MyTinyToDo_Bare_Docker.csar/
		// ServiceTemplates/%257Bhttp%253A%252F%252Fopentosca.org%252Fservicetemplates%257DMyTinyToDo_Bare_Docker/Instances
		String mainServiceTemplateInstancesUrl = this.getMainServiceTemplateURL(csarName) + "/Instances";

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
			ClientResponse serviceInstancesResponse = referencesResource.accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);

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
		    
			//FIXME timeout to break the loop
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
			ClientResponse planInstanceResp = planInstanceResource.accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);

			JSONObject planInstanceRespJson = new JSONObject(planInstanceResp.getEntity(String.class));
			System.out.println(planInstanceRespJson);
			
			if (planInstanceRespJson.getJSONObject("PlanInstance").getString("State").equals("finished")) {
				instanceFinished = true;
			}
			//FIXME timeout to break the loop
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
		
		Instance createdInstance = new Instance(serviceInstanceUrl);
		//createdInstance.setInputParameters(inputParameters);
		createdInstance.setOutputParameters(planOutputs);
		
		return createdInstance;
	}

	
	/**
	 * terminates the given instance
	 * 
	 * @param instance
	 * @return
	 */
	public boolean terminateInstance (final Instance instance) {
		//FIXME code
		return false;
	}
	
	/**
	 * gets instance properties (e.g., IP address)
	 * 
	 * @param instanceID
	 * @return
	 */
	public Map<String, String> getInstanceProperties (String instanceID) {
		Map<String, String> properties = new HashMap<String, String>();
		
		String instancePropertiesURL = instanceID + "/Properties";
		WebResource instancePropertiesResource = this.createWebResource(instancePropertiesURL);
		
		ClientResponse  instancePropertiesResponse = instancePropertiesResource
				.accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);

		JSONObject jsonObj = new JSONObject(instancePropertiesResponse.getEntity(String.class));
		JSONArray jsonArrayProperties = jsonObj.getJSONArray("payload");
		
		for (int index = 0; index < jsonArrayProperties.length(); index++) {
			
			JSONObject propertyJsonObj = jsonArrayProperties.getJSONObject(index); // property name
			JSONArray jsonArrayPropertyNames = propertyJsonObj.names();
			for (int indexj = 0; indexj < jsonArrayPropertyNames.length(); indexj++) {
				String key = jsonArrayPropertyNames.getString(indexj);
				
				String value = propertyJsonObj.getJSONObject(key).getString("TextContent");
				properties.put(key, value);	
			}
		}
		return properties;
	}

	private JSONObject getBuildPlanAsJson(final String csarName) {
		String url = this.getMainServiceTemplateURL(csarName);

		String planParameterUrl = url + BUILD_PLAN_PATH;

		WebResource planParameterResource = this.createWebResource(planParameterUrl);
		String jsonResponse = planParameterResource.accept(MediaType.APPLICATION_JSON).get(String.class);

		JSONObject jsonObj = new JSONObject(jsonResponse);

		return jsonObj;
	}

	private JSONObject getBuildPlanInputJsonObject(final String csarName) {
		return this.getBuildPlanAsJson(csarName).getJSONObject("Plan");
	}

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
		// example of href: http://192.168.209.199:1337/containerapi/CSARs/MyTinyToDo_Bare_Docker.csar/
		//                  ServiceTemplates/%257Bhttp%253A%252F%252Fopentosca.org%252Fservicetemplates%257DMyTinyToDo_Bare_Docker
		return href;
	}

	private WebResource createWebResource(final String resourceName) {
		return createWebResource(resourceName, null);
	}
	
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

	public static void main(String[] args) {
		ContainerAPIClient client = new ContainerAPIClient();
		
		// Retrieve installed applications
		System.out.println(client.getApplications());
	}
}
