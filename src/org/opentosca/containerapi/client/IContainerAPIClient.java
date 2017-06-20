package org.opentosca.containerapi.client;

import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.opentosca.containerapi.client.model.Application;
import org.opentosca.containerapi.client.model.NodeInstance;
import org.opentosca.containerapi.client.model.ServiceInstance;

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
	 * @param csarName
	 *            application name
	 * @return
	 */
	String undeployApplication(Application application);

	/**
	 * creates an instance of the application
	 * 
	 * @param application
	 * @param params
	 *            required parameters to provision the application
	 * @return an Instance object
	 */
	ServiceInstance createServiceInstance(Application application, Map<String, String> params);

	/**
	 * terminates the given instance
	 * 
	 * @param instance
	 * @return
	 */
	boolean terminateServiceInstance(ServiceInstance instance);

	/**
	 * Updates the given ServiceInstance object
	 * 
	 * @param serviceInstance
	 *            a ServiceInstance available at the configured OpenTOSCA
	 *            container
	 * @return a ServiceInstance
	 */
	ServiceInstance updateServiceInstance(ServiceInstance serviceInstance);

	/**
	 * Returns a list of all node instances of given service instance
	 * 
	 * @param serviceInstance
	 *            an available service instance
	 * @return a list of node instances
	 */
	List<NodeInstance> getNodeInstances(ServiceInstance serviceInstance);

}