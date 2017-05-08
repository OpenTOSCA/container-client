package org.opentosca.containerapi.client;
import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;

public class ContainerAPIClientTestJUnit {
	private static ContainerAPIClient client;
	private static String testCsarDir;
	private static String testCsarName;
	private static String containerHost;
	@BeforeClass
	public static void configure() {
		testCsarDir = "C://Users//francoaa//Desktop//test//";
		testCsarName = "MyTinyToDo_Bare_Docker.csar";
		containerHost = "192.168.209.230";
		
		client = new ContainerAPIClient(containerHost);
		
//		try {
//			FileUtils.copyURLToFile(new URL("http://files.opentosca.org/csars/MyTinyToDo_Bare_Docker.csar"), testCsar);
//			System.out.println(testCsar.getName());
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}		
	}
	
//	@AfterClass
//	public static void cleanApplications() {
//		List<String> applications = client.getApplications();
//		if (!applications.isEmpty()) {
//			// Delete application
//			client.deleteApplication(testCsarName);
//		}
//	}
	
	@Test
	public void testGetApplications() {
		// Retrieve installed applications
		List<Application> applications = client.getApplications();
		System.out.println("Installed Applications: " + applications.size());
		
		for (Application app : applications) {
			System.out.println("Application name: " + app.getName() + " Application instantiation input: " + app.getInputParameters());
		}
		//assertEquals(0, applications.size());
	}
	
	@Test
	public void testUploadAndDeleteApplication() {

		try {
			String applicationName = client.deployApplication(testCsarDir + testCsarName);
			assertNotNull(applicationName);
			
			// Get application metadata
			JSONObject metadata = client.getApplicationProperties(applicationName);
			System.out.println("Application Metadata: " + metadata);
			
			// Retrieve installed applications
			List<Application> applications = client.getApplications();
			assertNotEquals(0, applications.size());
			assertTrue(applications.contains(applicationName));
			
			// Delete application
			client.undeployApplication(applicationName);
			
			// Retrieve installed applications and check if it was not deleted
			applications = client.getApplications();
			assertFalse(applications.contains(applicationName));

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Test
	public void testDeployApplicationAndCreateInstance() {
		try {
			String applicationName = client.deployApplication(testCsarDir + testCsarName);
			assertNotNull(applicationName);
			
			// Get application metadata
			JSONObject metadata = client.getApplicationProperties(applicationName);
			System.out.println("Application Metadata: " + metadata);
			
			// Retrieve installed applications
			List<Application> applications = client.getApplications();
			assertNotEquals(0, applications.size());
			
			List<String> inputParams = client.getInputParameters(applicationName);
			System.out.println("input parameters: " + inputParams);
			
			// Provision application
			Map<String, String> inputs = new HashMap<String, String>();
			inputs.put("DockerEngineURL", "tcp://" + containerHost + ":2375");
			inputs.put("DockerEngineCertificate", "");
			
			Instance instance = client.createInstance(applicationName, inputs);
			assertNotNull(instance);
			System.out.println("output parameters: " + instance.getOutputParameters());
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	@Test
	public void testGetInputParameters() {
		List<String> inputParams = client.getInputParameters(testCsarName);
		System.out.println("input parameters: " + inputParams);
	}
	
	@Test
	public void testProvisionApplication() {
		Map<String, String> inputs = new HashMap<String, String>();
		inputs.put("DockerEngineURL", "tcp://" + containerHost + ":2375");
		inputs.put("DockerEngineCertificate", "");
		
		Instance instance = client.createInstance(testCsarName, inputs);
		assertNotNull(instance);
		System.out.println("output parameters: " + instance.getOutputParameters());
	}
	
}
