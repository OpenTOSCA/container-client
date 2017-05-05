package org.opentosca.containerapi.client;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;

public class ContainerAPIClientTestJUnit {
	private static ContainerAPIClient client;
	private static String testCsarDir;
	private static String testCsarName;
	
	@BeforeClass
	public static void configure() {
		client = new ContainerAPIClient("192.168.209.199");
		
//		try {
//			FileUtils.copyURLToFile(new URL("http://files.opentosca.org/csars/MyTinyToDo_Bare_Docker.csar"), testCsar);
//			System.out.println(testCsar.getName());
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}		

		testCsarDir = "C://Users//francoaa//Desktop//test//";
		testCsarName = "MyTinyToDo_Bare_Docker.csar";
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
		List<String> applications = client.getApplications();
		System.out.println("Installed Applications: " + applications.size());
		for (String name : applications) {
			System.out.println(name);
		}
		//assertEquals(0, applications.size());
	}
	
	@Test
	public void testUploadAndDeleteApplication() {

		try {
			String applicationName = client.uploadApplication(testCsarDir + testCsarName);
			assertNotNull(applicationName);
			
			// Get application metadata
			String metadata = client.getCSARMetaData(applicationName);
			System.out.println("Application Metadata: " + metadata);
			
			// Retrieve installed applications
			List<String> applications = client.getApplications();
			assertNotEquals(0, applications.size());
			assertEquals(testCsarName, applications.get(0));
			
			// Delete application
			client.deleteApplication(applicationName);
			
			// Retrieve installed applications and check if it was not deleted
			applications = client.getApplications();
			for (String name : applications) {
				assertNotEquals(testCsarName, name);
			}
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Test
	public void testUploadAndDeployApplication() {
		try {
			String applicationName = client.uploadApplication(testCsarDir + testCsarName);
			assertNotNull(applicationName);
			
			// Get application metadata
			String metadata = client.getCSARMetaData(applicationName);
			System.out.println("Application Metadata: " + metadata);
			
			// Retrieve installed applications
			List<String> applications = client.getApplications();
			assertNotEquals(0, applications.size());
			
			List<String> inputParams = client.getInputParameters(applicationName);
			System.out.println("input parameters: " + inputParams);
			
			// Provision application
			Map<String, String> inputs = new HashMap<String, String>();
			inputs.put("DockerEngineURL", "tcp://192.168.209.199:2375");
			inputs.put("DockerEngineCertificate", "");
			
			Map<String, String> outputs = client.provisionApplication(applicationName, inputs);
			System.out.println("output parameters: " + outputs);
			
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
		inputs.put("DockerEngineURL", "tcp://192.168.209.230:2375");
		inputs.put("DockerEngineCertificate", "");
		
		Map<String, String> outputs = client.provisionApplication(testCsarName, inputs);
		System.out.println("output parameters: " + outputs);
	}
	
}
