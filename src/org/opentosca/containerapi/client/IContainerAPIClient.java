package org.opentosca.containerapi.client;

import java.util.List;
import java.util.Map;

import org.json.JSONObject;

public interface IContainerAPIClient {

	String getContainerAPIUrl();

	void setContainerAPIUrl(String containerAPIUrl);

	/**
	 * gets a list of installed applications
	 * 
	 * @return
	 */
	List<Application> getApplications();

	/**
	 * Gets application metadata (i.e., display name, description, version, authors)
	 * @param csarName
	 * @return Application Metadata as a JSON object
	 */
	JSONObject getApplicationProperties(String csarName);

	String getDisplayName(String csarName);

	String getVersion(String csarName);

	String getDescription(String csarName);

	String getAuthor(String csarName);

	/**
	 * 
	 * deploys an Application (CSAR file) onto the OpenTosca ecosystem
	 * 
	 * @param filePath
	 * @return Application object or null if upload failed
	 * @throws Exception
	 */
	Application deployApplication(String filePath) throws Exception;

	/**
	 * deletes an Application (CSAR file) onto the OpenTosca ecosystem 
	 * 
	 * @param csarName application name
	 * @return
	 */
	String undeployApplication(Application application);

	/**
	 * gets required input parameters for creating an instance of the Application
	 * @param csarName
	 * @return list of the parameters
	 * 
	 */
	List<String> getInputParameters(String csarName);

	/**
	 * creates an instance of the application
	 * @param csarName
	 * @param params required parameters to provision the application
	 * @return an Instance object
	 */
	Instance createInstance(String csarName, Map<String, String> params);

	/**
	 * terminates the given instance
	 * 
	 * @param instance
	 * @return
	 */
	boolean terminateInstance(Instance instance);

	/**
	 * gets instance properties (e.g., IP address)
	 * 
	 * @param instanceID
	 * @return
	 */
	Map<String, String> getInstanceProperties(String instanceID);

}