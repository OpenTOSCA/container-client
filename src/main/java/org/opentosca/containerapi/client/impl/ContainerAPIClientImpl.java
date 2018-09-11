/**
 * 
 */
package org.opentosca.containerapi.client.impl;

import java.io.File;
import java.io.FileInputStream;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.opentosca.api.rest.client.api.DefaultApi;
import org.opentosca.api.rest.client.invoker.ApiClient;
import org.opentosca.api.rest.client.invoker.ApiException;
import org.opentosca.api.rest.client.model.CsarDTO;
import org.opentosca.api.rest.client.model.FormDataContentDisposition;
import org.opentosca.api.rest.client.model.InputStream;
import org.opentosca.api.rest.client.model.InterfaceDTO;
import org.opentosca.api.rest.client.model.PlanDTO;
import org.opentosca.api.rest.client.model.ServiceTemplateDTO;
import org.opentosca.api.rest.client.model.ServiceTemplateInstanceDTO;
import org.opentosca.api.rest.client.model.TParameter;
import org.opentosca.containerapi.client.IContainerAPIClient;
import org.opentosca.containerapi.client.model.Application;
import org.opentosca.containerapi.client.model.ModelUtils;
import org.opentosca.containerapi.client.model.NodeInstance;
import org.opentosca.containerapi.client.model.RelationInstance;
import org.opentosca.containerapi.client.model.ServiceInstance;
import org.opentosca.containerapi.client.model.ServiceTemplate;

/**
 * @author kalmankepes
 *
 */
public class ContainerAPIClientImpl implements IContainerAPIClient {

	private ApiClient clientConfig;
	private DefaultApi client;

	public ContainerAPIClientImpl(final String host) {
		this.clientConfig = new ApiClient();
		this.clientConfig.setBasePath(host);
		this.client = new DefaultApi(this.clientConfig);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.opentosca.containerapi.client.IOpenTOSCAContainerAPIClient#
	 * getContainerAPIUrl()
	 */
	@Override
	public String getContainerAPIUrl() {
		return this.clientConfig.getBasePath();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.opentosca.containerapi.client.IOpenTOSCAContainerAPIClient#
	 * getApplications()
	 */
	@Override
	public List<Application> getApplications() {
		List<Application> apps = new ArrayList<Application>();
		try {
			for (CsarDTO csar : this.client.getCsars()) {
				// there MUST only be one serviceTemplate and buildPlan
				ServiceTemplateDTO servTemp = this.client.getServiceTemplates(csar.getId()).get(0);
				List<InterfaceDTO> ifaces = this.client.getInterfaces(csar.getId(), servTemp.getId());
				List<ServiceTemplateInstanceDTO> servInstances = this.client.getServiceTemplateInstances(csar.getId(),
						servTemp.getId());
				PlanDTO buildPlan = this.client.getBuildPlans(csar.getId(), servTemp.getId()).get(0);
				List<TParameter> inputParams = buildPlan.getInputParameters();

				// TODO METADATA FETCH IS MISSING!!!!!
				
				apps.add(ModelUtils.transform(csar, inputParams, servInstances, ifaces, ""));
			}
		} catch (ApiException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
	public Application getApplication(String csarName) {
		Application app = null;
		try {
			CsarDTO csar = this.client.getCsar(csarName);
			ServiceTemplateDTO servTemp = this.client.getServiceTemplates(csar.getId()).get(0);
			List<InterfaceDTO> ifaces = this.client.getInterfaces(csar.getId(), servTemp.getId());
			List<ServiceTemplateInstanceDTO> servInstances = this.client.getServiceTemplateInstances(csar.getId(),
					servTemp.getId());
			PlanDTO buildPlan = this.client.getBuildPlans(csar.getId(), servTemp.getId()).get(0);
			List<TParameter> inputParams = buildPlan.getInputParameters();

			// TODO METADATA FETCH IS MISSING!!!!!
			
			app = ModelUtils.transform(csar, inputParams, servInstances, ifaces, "");
			
		} catch (ApiException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return app;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.opentosca.containerapi.client.IOpenTOSCAContainerAPIClient#
	 * deployApplication(java.lang.String)
	 */
	@Override
	public Application deployApplication(String filePath) throws Exception {
		
		File file = new File(filePath);
		
		
		FormDataContentDisposition contentDisposition = new FormDataContentDisposition();
		contentDisposition.setFileName(file.getName());
		contentDisposition.setCreationDate();
		contentDisposition.setModificationDate(file.lastModified());
		
		
		this.client.uploadFile(null, file);
		
		
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.opentosca.containerapi.client.IOpenTOSCAContainerAPIClient#
	 * undeployApplication(org.opentosca.containerapi.client.model.Application)
	 */
	@Override
	public String undeployApplication(Application application) {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.opentosca.containerapi.client.IOpenTOSCAContainerAPIClient#
	 * getServiceTemplate(org.opentosca.containerapi.client.model.Application)
	 */
	@Override
	public ServiceTemplate getServiceTemplate(Application application) {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.opentosca.containerapi.client.IOpenTOSCAContainerAPIClient#
	 * getServiceTemplate(org.opentosca.containerapi.client.model.ServiceInstance)
	 */
	@Override
	public ServiceTemplate getServiceTemplate(ServiceInstance serviceInstance) {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.opentosca.containerapi.client.IOpenTOSCAContainerAPIClient#
	 * createServiceInstance(org.opentosca.containerapi.client.model.Application,
	 * java.util.Map)
	 */
	@Override
	public ServiceInstance createServiceInstance(Application application, Map<String, String> params) {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.opentosca.containerapi.client.IOpenTOSCAContainerAPIClient#
	 * getServiceInstances(org.opentosca.containerapi.client.model.Application)
	 */
	@Override
	public List<ServiceInstance> getServiceInstances(Application application) {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.opentosca.containerapi.client.IOpenTOSCAContainerAPIClient#
	 * terminateServiceInstance(org.opentosca.containerapi.client.model.
	 * ServiceInstance)
	 */
	@Override
	public boolean terminateServiceInstance(ServiceInstance instance) {
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.opentosca.containerapi.client.IOpenTOSCAContainerAPIClient#
	 * invokeServiceInstanceOperation(org.opentosca.containerapi.client.model.
	 * ServiceInstance, java.lang.String, java.lang.String, java.util.Map)
	 */
	@Override
	public Map<String, String> invokeServiceInstanceOperation(ServiceInstance serviceInstance, String interfaceName,
			String operationName, Map<String, String> params) {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.opentosca.containerapi.client.IOpenTOSCAContainerAPIClient#
	 * updateServiceInstance(org.opentosca.containerapi.client.model.
	 * ServiceInstance)
	 */
	@Override
	public ServiceInstance updateServiceInstance(ServiceInstance serviceInstance) {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.opentosca.containerapi.client.IOpenTOSCAContainerAPIClient#
	 * updateNodeInstance(org.opentosca.containerapi.client.model.NodeInstance)
	 */
	@Override
	public NodeInstance updateNodeInstance(NodeInstance nodeInstance) {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.opentosca.containerapi.client.IOpenTOSCAContainerAPIClient#
	 * updateRelationInstance(org.opentosca.containerapi.client.model.
	 * RelationInstance)
	 */
	@Override
	public RelationInstance updateRelationInstance(RelationInstance relationInstance) {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.opentosca.containerapi.client.IOpenTOSCAContainerAPIClient#
	 * getNodeInstances(org.opentosca.containerapi.client.model.ServiceInstance)
	 */
	@Override
	public List<NodeInstance> getNodeInstances(ServiceInstance serviceInstance) {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.opentosca.containerapi.client.IOpenTOSCAContainerAPIClient#
	 * getRelationInstances(org.opentosca.containerapi.client.model.ServiceInstance)
	 */
	@Override
	public List<RelationInstance> getRelationInstances(ServiceInstance serviceInstance) {
		// TODO Auto-generated method stub
		return null;
	}

}
