package org.opentosca.containerapi.client;

import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.opentosca.containerapi.client.model.Application;
import org.opentosca.containerapi.client.model.NodeInstance;
import org.opentosca.containerapi.client.model.RelationInstance;
import org.opentosca.containerapi.client.model.ServiceInstance;

public interface IOpenTOSCAContainerAPIClient {

	String getContainerAPIUrl();

	/**
	 * gets a list of installed applications
	 * 
	 * @return
	 */
	List<Application> getApplications();

	
	/**
	 * retrieves an Application by its CSAR name
	 * @param csarName
	 * @return
	 */
	Application getApplication(String csarName);
	
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
	 * Returns all {@link ServiceInstance}s of the given {@link Application}
	 * @param application An {@link Application} deployed on the container
	 * @return a {@link List} of {@link ServiceInstance}
	 */
	List<ServiceInstance> getServiceInstances(Application application);		
	
	/**
	 * terminates the given instance
	 * 
	 * @param instance
	 * @return
	 */
	boolean terminateServiceInstance(ServiceInstance instance);

	/**
	 * Invokes an operation defined in the interfaces of the boundary definition of
	 * the given application.
	 * 
	 * @param application
	 *            An {@link Application} to call its operation
	 * @param interfaceName
	 *            The name of the interface the operation belongs to as
	 *            {@link String}
	 * @param operationName
	 *            The name of the operation to call as {@link String}
	 * @param params
	 *            A {@link Map} of {@link String} to {@link String} containing a
	 *            mapping of input parameters and values for the operation to call
	 * 
	 * @return A {@link Map} from {@link String} to {@link String} containing a
	 *         mapping of output parameters and values of the called operation
	 */
	Map<String, String> invokeServiceInstanceOperation(ServiceInstance serviceInstance, String interfaceName, String operationName,
			Map<String, String> params);

	/**
	 * Updates the given ServiceInstance object
	 * 
	 * @param serviceInstance
	 *            a ServiceInstance available at the configured OpenTOSCA container
	 * @return a ServiceInstance
	 */
	ServiceInstance updateServiceInstance(ServiceInstance serviceInstance);

	/**
	 * Updates the given Node Instance object
	 * 
	 * @param nodeInstance
	 *            a Node Instance available at the configured OpenTOSCA container
	 * @return a Node Instance
	 */
	NodeInstance updateNodeInstance(NodeInstance nodeInstance);

	/**
	 * Updates the given Relation Instance object
	 * 
	 * @param relationInstance
	 *            a Relation Instance available at the configured OpenTOSCA
	 *            container
	 * @return a Relation Instance
	 */
	RelationInstance updateRelationInstance(RelationInstance relationInstance);

	/**
	 * Returns a list of all node instances of the given service instance
	 * 
	 * @param serviceInstance
	 *            an available service instance
	 * @return a list of node instances
	 */
	List<NodeInstance> getNodeInstances(ServiceInstance serviceInstance);

	/**
	 * Returns a list of all relation instances of the given service instance
	 * 
	 * @param serviceInstance
	 *            an available service instance
	 * @return a list of relation instances
	 */
	List<RelationInstance> getRelationInstances(ServiceInstance serviceInstance);

}