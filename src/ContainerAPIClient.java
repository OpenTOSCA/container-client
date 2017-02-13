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
	public static String CONTAINER_API_URL = "http://localhost:1337/containerapi";
	public static String BUILD_PLAN_PATH = "/BoundaryDefinitions/Interfaces/OpenTOSCA-Lifecycle-Interface/Operations/initiate/Plan";

	public List<String> getApplications() {
		List<String> csarNames = new ArrayList<String>();
		String url = CONTAINER_API_URL + "/CSARs";
		ClientResponse resp = this.createWebResource(url, null).accept(MediaType.APPLICATION_JSON)
				.get(ClientResponse.class);

		JSONObject respJsonObj = new JSONObject(resp.getEntity(String.class));

		JSONArray refArrayJson = respJsonObj.getJSONArray("References");

		for (int index = 0; index < refArrayJson.length(); index++) {
			JSONObject refJsonObj = refArrayJson.getJSONObject(index);
			if (refJsonObj.has("title") && !refJsonObj.getString("title").equals("Self")) {
				csarNames.add(refJsonObj.getString("title"));
			}
		}

		return csarNames;
	}

	public String getDisplayName(final String csarName) {
		return this.getCSARMetaData(csarName).getString("displayName");
	}

	public String getVersion(final String csarName) {
		return this.getCSARMetaData(csarName).getString("version");
	}

	public String getDescription(final String csarName) {
		return this.getCSARMetaData(csarName).getString("description");
	}

	public String getAuthor(final String csarName) {
		return this.getCSARMetaData(csarName).getString("authors");
	}

	public String uploadApplication(final String filePath) throws Exception {
		String url = CONTAINER_API_URL + "/CSARs";
		File fileObj = new File(filePath);
		if (fileObj != null && fileObj.exists() && fileObj.isFile()) {
			WebResource webResource = createWebResource(url, null);
			// the file to upload, represented as FileDataBodyPart
			FileDataBodyPart fileDataBodyPart = new FileDataBodyPart("file", fileObj,
					MediaType.APPLICATION_OCTET_STREAM_TYPE);
			// fileDataBodyPart.setContentDisposition(FormDataContentDisposition.name("file").fileName(fileObj.getName()).build());

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
			return csarPath.substring(csarPath.lastIndexOf("/") + 1);

		} else {
			System.out.println("Upload not possible");
			return null;
		}
	}
	
	

	public String deleteApplication(final String csarName) {
		String url = CONTAINER_API_URL + "/CSARs/" + csarName;
		WebResource webResource = createWebResource(url, null);
		ClientResponse response = webResource.accept(MediaType.TEXT_PLAIN).delete(ClientResponse.class);
		String ret = response.getEntity(String.class);
		response.close();
		return ret;
	}

	public List<String> getInputParameters(final String csarName) {
		List<String> paramNames = new ArrayList<String>();
		JSONObject obj = this.getBuildPlanAsJson(csarName);

		JSONObject planObj = obj.getJSONObject("Plan");

		JSONArray jsonArrayParams = planObj.getJSONArray("InputParameters");

		for (int index = 0; index < jsonArrayParams.length(); index++) {
			JSONObject inputParam = jsonArrayParams.getJSONObject(index);
			paramNames.add(inputParam.getString("Name"));
		}

		return paramNames;
	}

	public Map<String, String> provisionApplication(final String csarName, final Map<String, String> params) {

		Map<String, String> planOutputs = new HashMap<String, String>();

		JSONObject planInputJsonObj = this.getBuildPlanInputJsonObject(csarName);

		if (params != null) {
			// fill up the planInput with the given values
			JSONArray inputParamArray = planInputJsonObj.getJSONArray("InputParameters");
			for (int index = 0; index < inputParamArray.length(); index++) {

				JSONObject inputParam = inputParamArray.getJSONObject(index).getJSONObject("InputParameter");

				if (params.containsKey(inputParam.getString("Name"))) {
					inputParam.put("Value", params.get(inputParam.getString("Name")));
				}

			}

		}

		String mainServiceTemplateInstancesUrl = this.getMainServiceTemplateURL(csarName) + "/Instances";

		int serviceInstanceCount = this.getServiceInstanceCount(csarName);

		WebResource mainServiceTemplateInstancesResource = this.createWebResource(mainServiceTemplateInstancesUrl,
				null);

		ClientResponse response = mainServiceTemplateInstancesResource.accept(MediaType.APPLICATION_JSON)
				.post(ClientResponse.class, planInputJsonObj.toString());

		JSONObject respJsonObj = new JSONObject(response.getEntity(String.class));

		String serviceInstancesResourceUrl = respJsonObj.getString("PlanURL");

		String correlationId = serviceInstancesResourceUrl.split("BuildPlanCorrelationId=")[1];

		WebResource referencesResource = this.createWebResource(serviceInstancesResourceUrl, null);

		boolean serviceInstanceIsAvailable = false;

		String planInstanceUrl = "";
		while (!serviceInstanceIsAvailable) {
			ClientResponse serviceInstancesResponse = referencesResource.accept(MediaType.APPLICATION_JSON)
					.get(ClientResponse.class);

			JSONObject jsonObj = new JSONObject(serviceInstancesResponse.getEntity(String.class));
			if (jsonObj.getJSONArray("References").length() > serviceInstanceCount) {
				JSONArray jsonRefs = jsonObj.getJSONArray("References");
				// find highest instance id
				int maxIdCount = -1;
				String maxIdUrl = "";

				for (int index = 0; index < jsonRefs.length(); index++) {
					JSONObject jsonRef = jsonRefs.getJSONObject(index);
					if (!jsonRef.getString("title").equals("Self") && jsonRef.getInt("title") > maxIdCount) {
						maxIdCount = jsonRef.getInt("title");
						maxIdUrl = jsonRef.getString("href");
					}
				}

				planInstanceUrl = maxIdUrl;
			}
			

			
			//FIXME 
			serviceInstanceIsAvailable = true;
		}

		// /PlanInstances/1486950673724-0/State

		planInstanceUrl = planInstanceUrl + "/PlanInstances/" + correlationId + "/State";
		System.out.println(planInstanceUrl);
		WebResource planInstanceResource = this.createWebResource(planInstanceUrl, null);

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

			JSONObject planInstanceRespJson = new JSONObject(planInstanceResp.getEntity(String.class));

			if (planInstanceRespJson.getJSONObject("PlanInstance").getString("State").equals("finished")) {
				instanceFinished = true;
			}
		}

		String planInstanceOutputUrl = planInstanceUrl + "/PlanInstances/" + correlationId + "/Output";

		ClientResponse planInstanceOutput = this.createWebResource(planInstanceOutputUrl, null)
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

		return planOutputs;
	}

	private int getServiceInstanceCount(final String csarName) {
		String mainServiceTemplateInstancesUrl = this.getMainServiceTemplateURL(csarName) + "/Instances";
		WebResource mainServiceTemplateInstancesResource = this.createWebResource(mainServiceTemplateInstancesUrl,
				null);

		ClientResponse serviceInstancesResponse = mainServiceTemplateInstancesResource
				.accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);

		JSONObject jsonObj = new JSONObject(serviceInstancesResponse.getEntity(String.class));
		return jsonObj.getJSONArray("References").length() - 1;
	}

	private JSONObject getBuildPlanAsJson(final String csarName) {
		String url = this.getMainServiceTemplateURL(csarName);

		String planParameterUrl = url + BUILD_PLAN_PATH;

		WebResource planParameterResource = this.createWebResource(planParameterUrl, null);
		String jsonResponse = planParameterResource.accept(MediaType.APPLICATION_JSON).get(String.class);

		JSONObject jsonObj = new JSONObject(jsonResponse);

		return jsonObj;
	}

	private JSONObject getBuildPlanInputJsonObject(final String csarName) {
		return this.getBuildPlanAsJson(csarName).getJSONObject("Plan");
	}

	private String getMainServiceTemplateURL(final String csarName) {
		String url = CONTAINER_API_URL + "/CSARs/" + csarName + "/ServiceTemplates";

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
		return href;
	}

	private WebResource createWebResource(final String resourceName, final Map<String, String> queryParamsMap) {

		// log.debug("Execute method at container API: createWebResource");

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
		// System.out.println(client.getInstalledApplications());
		String csarName = "HomeAssistant_Bare_Docker.csar";

		try {
			//client.uploadApplication("/home/kalman/Downloads/HomeAssistant_Bare_Docker.csar");
			//client.uploadApplication("C://Users//francoaa//Desktop//test//HomeAssistant_Bare_Docker.csar");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		System.out.println("Fetch BuildPlan data: ");
		System.out.println(client.getBuildPlanAsJson(csarName).toString());
		System.out.println("Fetch BuildPlan Input data: ");
		System.out.println(client.getBuildPlanInputJsonObject(csarName));

		Map<String, String> inputs = new HashMap<String, String>();

		inputs.put("SensorType", "asd");

		client.provisionApplication(csarName, inputs);
		// client.deleteApplication(csarName);

		// System.out.println(client.getApplicationDescription("MyTinyToDo_Bare_Docker.csar"));
		// System.out.println(client.provisionApplication("HomeAssistant_Bare_Docker.csar",
		// null));
		// System.out.println(client.deleteApplication("MyTinyToDo_Bare_Docker.csar"));
	}

	private JSONObject getCSARMetaData(final String csarName) {
		// http://localhost:1337/containerapi/CSARs/HomeAssistant_Bare_Docker.csar/MetaData
		String url = CONTAINER_API_URL + "/CSARs/" + csarName + "/MetaData";
		ClientResponse resp = this.createWebResource(url, null).accept(MediaType.APPLICATION_JSON)
				.get(ClientResponse.class);
		return new JSONObject(resp.getEntity(String.class));
	}

}
