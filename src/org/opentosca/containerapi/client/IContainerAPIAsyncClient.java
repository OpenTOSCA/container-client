package org.opentosca.containerapi.client;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import org.json.JSONObject;
import org.opentosca.containerapi.client.model.Application;
import org.opentosca.containerapi.client.model.NodeInstance;
import org.opentosca.containerapi.client.model.RelationInstance;
import org.opentosca.containerapi.client.model.ServiceInstance;

public interface IContainerAPIAsyncClient {

	String getLegacyContainerAPIUrl();

	void setLegacyContainerAPIUrl(String containerAPIUrl);

	/**
	 * gets a list of installed applications
	 * 
	 * @return
	 */
	Future<List<Application>> getApplications();

	/**
	 * 
	 * deploys an Application (CSAR file) onto the OpenTosca ecosystem
	 * 
	 * @param filePath
	 * @return Application object or null if upload failed
	 * @throws Exception
	 */
	Future<Application> deployApplication(String filePath) throws Exception;

	/**
	 * deletes an Application (CSAR file) onto the OpenTosca ecosystem
	 * 
	 * @param csarName
	 *            application name
	 * @return
	 */
	Future<String> undeployApplication(Application application);

	/**
	 * creates an instance of the application
	 * 
	 * @param application
	 * @param params
	 *            required parameters to provision the application
	 * @return an Instance object
	 */
	Future<ServiceInstance> createServiceInstance(Application application, Map<String, String> params);

	/**
	 * terminates the given instance
	 * 
	 * @param instance
	 * @return
	 */
	Future<Boolean> terminateServiceInstance(ServiceInstance instance);

	/**
	 * Updates the given ServiceInstance object
	 * 
	 * @param serviceInstance
	 *            a ServiceInstance available at the configured OpenTOSCA
	 *            container
	 * @return a ServiceInstance
	 */
	Future<ServiceInstance> updateServiceInstance(ServiceInstance serviceInstance);
	
	
	/**
	 * Updates the given Node Instance object
	 * 
	 * @param nodeInstance
	 *            a Node Instance available at the configured OpenTOSCA
	 *            container
	 * @return a Node Instance
	 */
	Future<NodeInstance> updateNodeInstance(NodeInstance nodeInstance);
	
	/**
	 * Updates the given Relation Instance object
	 * 
	 * @param relationInstance
	 *            a Relation Instance available at the configured OpenTOSCA
	 *            container
	 * @return a Relation Instance
	 */
	Future<RelationInstance> updateRelationInstance(RelationInstance relationInstance);
	
	/**
	 * Returns a list of all node instances of the given service instance
	 * 
	 * @param serviceInstance
	 *            an available service instance
	 * @return a list of node instances
	 */
	Future<List<NodeInstance>> getNodeInstances(ServiceInstance serviceInstance);
	
	/**
	 * Returns a list of all relation instances of the given service instance
	 * 
	 * @param serviceInstance
	 *            an available service instance
	 * @return a list of relation instances
	 */
	Future<List<RelationInstance>> getRelationInstances(ServiceInstance serviceInstance);

}