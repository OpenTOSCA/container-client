package org.opentosca.containerapi.client;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.opentosca.containerapi.client.impl.ContainerAPIClient;
import org.opentosca.containerapi.client.model.Application;
import org.opentosca.containerapi.client.model.ServiceInstance;

@FixMethodOrder(MethodSorters.NAME_ASCENDING) 
public class ContainerAPIClientTestJUnit {
	private static IContainerAPIClient client;
	private static String testCsarPath;
	private static String testCsarName;
	private static String containerHost;
	
	private static ServiceInstance instance;
	private static Application application;
	
	@BeforeClass
	public static void configure() {
		String testParams = null;
		try {
			testParams = FileUtils.readFileToString(new File ("resources/testParams.json"), "UTF-8");
				
			if (testParams != null) {
				testCsarPath = new JSONObject (testParams).getString("csarPath");
				testCsarName = new JSONObject (testParams).getString("csarName");
				containerHost = new JSONObject (testParams).getString("containerHost");

			} else {
				// Fill for testing if testParams.txt not existing
				testCsarPath = "";
				testCsarName = "";
				containerHost = "";
			}
			System.out.println("Running tests with following configuration:");
			System.out.println("Csar file: "+ testCsarPath);
			System.out.println("Container Host: "+ containerHost);	
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		client = new ContainerAPIClient(containerHost);	
	}
	
	@Test
	public void test1DeployApplication() {
		try {
			Application deployedApplication = client.deployApplication(testCsarPath);
			assertNotNull(deployedApplication);
			
			// Get application metadata

			System.out.println("Application DisplayName: " + deployedApplication.getDisplayName());
			System.out.println("Application Name: " + deployedApplication.getId());
			System.out.println("Application Author: " + deployedApplication.getAuthor());
			System.out.println("Application Version: " + deployedApplication.getVersion());
			System.out.println("Application Description: " + deployedApplication.getDescription());
		
			// Retrieve installed applications
			List<Application> applications = client.getApplications();
			assertNotEquals(0, applications.size());
			
			boolean foundApp = false;
			for (Application app : applications) {
				if (app.getId().equals(testCsarName)) {
					foundApp = true;
					break;
				}
			}
			assertTrue(foundApp);
			application = deployedApplication;
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Test
	public void test2GetApplications() {
		// Retrieve installed applications
		List<Application> applications = client.getApplications();
		System.out.println("Installed Applications: " + applications.size());
		
		for (Application app : applications) {
			System.out.println("Application name: " + app.getId() + " Application instantiation input: " + app.getInputParameters());
		}
		assertEquals(1, applications.size());
	}
	
	@Test
	public void test3GetInputParameters() {
		
		List<String> inputParams = application.getInputParameters();
		System.out.println("input parameters: " + inputParams);
		assertFalse(inputParams.isEmpty());
	}
	
	@Test
	public void test4CreateInstance() {
		Map<String, String> inputs = new HashMap<String, String>();
		inputs.put("DockerEngineURL", "tcp://localhost:2375");
		inputs.put("DockerEngineCertificate", "");
		inputs.put("ApplicationPort", "80");
		
		instance = client.createServiceInstance(application, inputs);
		assertNotNull(instance);
		System.out.println("output parameters: " + instance.getPlanOutputParameters());
	}
	
	@Test
	public void test5GetInstanceProperties() {
		Map<String, String> instanceProperties = instance.getProperties();
		System.out.println(instanceProperties);
	}
	
	@Test
	public void test6DeleteInstance() {		
		boolean result = client.terminateServiceInstance(instance);
		assertTrue(result);
	}
	
	@Test
	public void test7DeleteApplication() {
		// Delete application
		client.undeployApplication(application);
		
		// Retrieve installed applications and check if it was not deleted
		List<Application> applications = client.getApplications();
		
		boolean foundApp = false;
		for (Application application : applications) {
			if (application.getId().equals(application.getId())) {
				foundApp = true;
				break;
			}
		}
		assertFalse(foundApp);
	}	
}
