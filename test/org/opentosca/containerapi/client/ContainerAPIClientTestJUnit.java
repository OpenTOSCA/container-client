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

@FixMethodOrder(MethodSorters.NAME_ASCENDING) 
public class ContainerAPIClientTestJUnit {
	private static ContainerAPIClient client;
	private static String testCsarPath;
	private static String testCsarName;
	private static String containerHost;
	
	private static Instance instance;
	private static Application application;
	
	@BeforeClass
	public static void configure() {
		String testParams = null;
		try {
			testParams = FileUtils.readFileToString(new File ("testParams.txt"), "UTF-8");
				
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
			JSONObject metadata = client.getApplicationProperties(deployedApplication.getName());
			System.out.println("Application Metadata: " + metadata);
			
			// Retrieve installed applications
			List<Application> applications = client.getApplications();
			assertNotEquals(0, applications.size());
			
			boolean foundApp = false;
			for (Application app : applications) {
				if (app.getName().equals(testCsarName)) {
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
			System.out.println("Application name: " + app.getName() + " Application instantiation input: " + app.getInputParameters());
		}
		assertEquals(1, applications.size());
	}
	
	@Test
	public void test3GetInputParameters() {
		List<String> inputParams = client.getInputParameters(testCsarName);
		System.out.println("input parameters: " + inputParams);
		assertFalse(inputParams.isEmpty());
	}
	
	@Test
	public void test4CreateInstance() {
		Map<String, String> inputs = new HashMap<String, String>();
		inputs.put("DockerEngineURL", "tcp://" + containerHost + ":2375");
		inputs.put("DockerEngineCertificate", "");
		
		instance = client.createInstance(testCsarName, inputs);
		assertNotNull(instance);
		System.out.println("output parameters: " + instance.getOutputParameters());
	}
	
	@Test
	public void test5GetInstanceProperties() {
		String instanceID = instance.getId();
		Map<String, String> instanceProperties = client.getInstanceProperties(instanceID);
		System.out.println(instanceProperties);
	}
	
	@Test
	public void test6DeleteInstance() {
		//FIXME
		boolean result = client.terminateInstance(new Instance("http://192.168.209.160:1337/containerapi/CSARs/MyTinyToDo_Bare_Docker.csar/ServiceTemplates/%257Bhttp%253A%252F%252Fopentosca.org%252Fservicetemplates%257DMyTinyToDo_Bare_Docker/Instances/1", testCsarName));
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
			if (application.getName().equals(application.getName())) {
				foundApp = true;
				break;
			}
		}
		assertFalse(foundApp);
	}	
}
