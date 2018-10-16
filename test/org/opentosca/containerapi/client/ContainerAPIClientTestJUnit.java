package org.opentosca.containerapi.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runners.Parameterized;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.opentosca.containerapi.client.TestRunConfiguration.TestInstanceConfiguration;
import org.opentosca.containerapi.client.impl.OpenTOSCAContainerLegacyAPIClient;
import org.opentosca.containerapi.client.model.Application;
import org.opentosca.containerapi.client.model.NodeInstance;
import org.opentosca.containerapi.client.model.RelationInstance;
import org.opentosca.containerapi.client.model.ServiceInstance;
import org.opentosca.containerapi.client.model.ServiceTemplate;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(Parameterized.class)
public class ContainerAPIClientTestJUnit {

	private final TestRunConfiguration runConfiguration; // updated for each csar
	private final IOpenTOSCAContainerAPIClient client;

	private ServiceInstance instance;
	private Application application;

	@Parameterized.Parameters
	public static List<TestRunConfiguration> parameters() {
		List<TestRunConfiguration> params = new ArrayList<TestRunConfiguration>();

		File resourcesRoot = new File("resources/");

		File[] filesInResourcesRoot = resourcesRoot.listFiles();

		for (File file : filesInResourcesRoot) {

			if (!file.getName().endsWith("json")) {
				continue;
			}

			String testParams = null;
			try {
				testParams = FileUtils.readFileToString(file, "UTF-8");

				if (testParams != null) {
					JSONObject jTestParams = new JSONObject(testParams);
					String testCsarPath = jTestParams.getString("csarPath");
					String containerHost = jTestParams.getString("containerHost");
					String containerHostInternal = jTestParams.optString("containerHostInternal", containerHost);
					JSONArray csarsTests = jTestParams.getJSONArray("csarsTests");

					for (int i = 0, size = csarsTests.length(); i < size; i++) {

						JSONObject csarTestData = csarsTests.getJSONObject(i);
						String testCsarName = csarTestData.getString("csarName");

						JSONArray inputParams = csarTestData.getJSONArray("inputParams");
						Map<String, String> testInputParams = new HashMap<String, String>();
						for (int j = 0, sizej = inputParams.length(); j < sizej; j++) {
							JSONObject input = inputParams.getJSONObject(j);

							Iterator<String> it = input.keys();
							while (it.hasNext()) {
								String inputName = (String) it.next();
								testInputParams.put(inputName, input.getString(inputName));
							}
						}

						if (csarTestData.has("instanceRun")) {
							List<TestInstanceConfiguration> run = new ArrayList<TestInstanceConfiguration>();
							JSONArray instanceRuns = csarTestData.getJSONArray("instanceRun");
							for (int index = 0; index < instanceRuns.length(); index++) {
								JSONObject instanceRunJsonObj = instanceRuns.getJSONObject(index);
								String interfaceName = instanceRunJsonObj.getString("interfaceName");
								String operationName = instanceRunJsonObj.getString("operationName");
								JSONArray runInstanceInputParams = instanceRunJsonObj.getJSONArray("inputParams");
								Map<String, String> runInstanceInputParamMap = new HashMap<String, String>();
								for (int j = 0, sizej = runInstanceInputParams.length(); j < sizej; j++) {
									JSONObject input = runInstanceInputParams.getJSONObject(j);

									Iterator<String> it = input.keys();
									while (it.hasNext()) {
										String inputName = (String) it.next();
										runInstanceInputParamMap.put(inputName, input.getString(inputName));
									}
								}

								run.add(new TestInstanceConfiguration(interfaceName, operationName,
										runInstanceInputParamMap));

							}

							params.add(new TestRunConfiguration(testCsarPath, containerHost, containerHostInternal,
									testCsarName, testInputParams, run));
						} else {
							params.add(new TestRunConfiguration(testCsarPath, containerHost, containerHostInternal,
									testCsarName, testInputParams));
						}
					}

				}
				System.out.println("Running tests with following configurations:");
				for (TestRunConfiguration runParam : params) {
					System.out.println(runParam);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return params;
	}

	public ContainerAPIClientTestJUnit(TestRunConfiguration params) {
		runConfiguration = params;
		client = new OpenTOSCAContainerLegacyAPIClient(runConfiguration.containerHost,
				runConfiguration.containerHostInternal);
	}

	@Rule
	public TestName testName = new TestName();

	@Before
	public void before() {
		System.out.println(testName.getMethodName());

		// should always be one application and service instance for this test class
		switch (testName.getMethodName().split("\\[")[0]) {
		case "testDGetInputParameters":
		case "testECreateInstance":
		case "testJDeleteApplication":
			this.application = client.getApplications().get(0);
			break;
		case "testFGetInstanceProperties":
		case "testGTestInstanceRuns":
		case "testHDeleteInstance":
		case "testEGetServiceInstance":
			this.application = client.getApplications().get(0);
			// just makes the the testing more robust
			this.instance = this.getInstanceBasedOnHighestId(client.getServiceInstances(application));
			break;
		}
	}

	private ServiceInstance getInstanceBasedOnHighestId(List<ServiceInstance> serviceInstances) {
		ServiceInstance highestId = null;
		for (ServiceInstance serviceInstance : serviceInstances) {
			if (highestId == null || highestId.getId() < serviceInstance.getId()) {
				highestId = serviceInstance;
			}
		}

		return highestId;
	}

	@Test
	public void testADeployApplication() {
		try {
			String pathToCsar = runConfiguration.directoryPath + File.separator + runConfiguration.testCsarName;
			Application deployedApplication = client.deployApplication(pathToCsar);
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
				if (app.getId().equals(runConfiguration.testCsarName)) {
					foundApp = true;
					break;
				}
			}
			assertTrue(foundApp);
			application = deployedApplication;

		} catch (Exception e) {
			org.junit.Assert
					.fail("Error with " + runConfiguration + " in Mehtod testADeployApplication +\n" + e.getMessage());
		}
	}

	@Test
	public void testBGetApplications() {
		// Retrieve installed applications
		try {
			System.out.println(runConfiguration.testCsarName);
			List<Application> applications = client.getApplications();
			System.out.println("Installed Applications: " + applications.size());

			for (Application app : applications) {
				System.out.println("Application name: " + app.getId() + " Application instantiation input: "
						+ app.getInputParameters());
				System.out.println("Metadata: \n" + app.getMetadata());
			}
			assertEquals(1, applications.size());
		} catch (Exception e) {
			org.junit.Assert
					.fail("Error with " + runConfiguration + " in Mehtod testBGetApplications +\n" + e.getMessage());
		}
	}

	@Test
	public void testCGetServiceTemplates() {
		try {
			System.out.println(runConfiguration.testCsarName);
			List<Application> applications = client.getApplications();
			System.out.println("Installed Applications: " + applications.size());

			for (Application app : applications) {

				ServiceTemplate servTemplate = client.getServiceTemplate(app);
				assertNotNull(servTemplate);

				assertNotEquals(0, servTemplate.getNodeTemplates().size());
				assertNotEquals(0, servTemplate.getRelationshipTemplates().size());
			}

		} catch (Exception e) {
			org.junit.Assert.fail(
					"Error with " + runConfiguration + " in Mehtod testCGetServiceTemplates +\n" + e.getMessage());
		}

	}

	@Test
	public void testDGetInputParameters() {
		try {
			List<String> inputParams = application.getInputParameters();
			System.out.println("input parameters: " + inputParams);
			assertFalse(inputParams.isEmpty());
		} catch (Exception e) {
			org.junit.Assert
					.fail("Error with " + runConfiguration + " in Mehtod testDGetInputParameters +\n" + e.getMessage());
		}
	}

	@Test
	public void testECreateInstance() {
		try {
			instance = client.createServiceInstance(application, runConfiguration.testInputParams);
			assertNotNull(instance);

			float progress = instance.getCreationProgress();
			System.out.println("Current Progress: " + progress * 100);
			while (((int) progress) != 1) {
				System.out.println("Current Progress: " + progress * 100);
				instance = client.updateServiceInstance(instance);
				progress = instance.getCreationProgress();
			}

			assertEquals(1, (int) instance.getCreationProgress());
			System.out.println("output parameters: " + instance.getPlanOutputParameters());
		} catch (Exception e) {
			org.junit.Assert
					.fail("Error with " + runConfiguration + " in Mehtod testECreateInstance +\n" + e.getMessage());
		}
	}

	@Test
	public void testEGetServiceInstance() {
		try {
			long serviceInstanceId = instance.getId();
			ServiceInstance testInstance = client.getServiceInstance(serviceInstanceId);
			assertNotNull(testInstance);
		} catch (Exception e) {
			org.junit.Assert
					.fail("Error with " + runConfiguration + " in Mehtod testEGetServiceInstance +\n" + e.getMessage());
		}
	}

	@Test
	public void testFGetInstanceProperties() {
		try {
			Map<String, String> instanceProperties = instance.getProperties();
			System.out.println(instanceProperties);

			List<NodeInstance> nodeInstances = client.getNodeInstances(instance);
			List<RelationInstance> relationInstances = client.getRelationInstances(instance);

			// small and hacky test for nodeInstance operation invocation
			for (NodeInstance nodeInstance : nodeInstances) {
				if (nodeInstance.getNodeTemplateId().contains("Ubuntu")) {
					Map<String, String> params = new HashMap<String, String>();

					params.put("VMUserName", nodeInstance.getProperties().get("VMUserName"));
					params.put("VMPrivateKey", nodeInstance.getProperties().get("VMPrivateKey"));
					params.put("VMIP", nodeInstance.getProperties().get("VMIP"));

					this.client.invokeNodeInstanceOperation(nodeInstance, "OperatingSystemInterface",
							"waitForAvailability", params);
				}
			}

			assertNotNull(nodeInstances);
			assertNotNull(relationInstances);

			assertFalse(nodeInstances.isEmpty());
			assertFalse(relationInstances.isEmpty());
		} catch (Exception e) {
			org.junit.Assert.fail(
					"Error with " + runConfiguration + " in Mehtod testFGetInstanceProperties +\n" + e.getMessage());
		}
	}

	@Test
	public void testGTestInstanceRuns() {
		try {
			if (runConfiguration.instanceRuns != null) {
				for (TestInstanceConfiguration instanceRun : runConfiguration.instanceRuns) {

					Map<String, String> output = this.client.invokeServiceInstanceOperation(instance,
							instanceRun.interfaceName, instanceRun.operationName, instanceRun.inputParams);

					assertFalse(output.isEmpty());
				}
			}
		} catch (Exception e) {
			org.junit.Assert
					.fail("Error with " + runConfiguration + " in Mehtod testGTestInstanceRuns +\n" + e.getMessage());
		}
	}

	@Test
	public void testHDeleteInstance() {
		try {
			boolean result = client.terminateServiceInstance(instance);
			assertTrue(result);
		} catch (Exception e) {
			org.junit.Assert
					.fail("Error with " + runConfiguration + " in Mehtod testHDeleteInstance +\n" + e.getMessage());
		}
	}

	@Test
	public void testIGetApplication() {
		try {
			Application app = client.getApplication(runConfiguration.testCsarName);
			assertNotNull(app);
			System.out.println("Application name: " + app.getId() + " Application instantiation input: "
					+ app.getInputParameters());
			System.out.println("Metadata: \n" + app.getMetadata());
		} catch (Exception e) {
			org.junit.Assert
					.fail("Error with " + runConfiguration + " in Mehtod testIGetApplication +\n" + e.getMessage());
		}
	}

	@Test
	public void testJDeleteApplication() {
		try {

			// Delete application
			Application app = application;

			if (app == null) {
				app = client.getApplications().get(0);
			}
			client.undeployApplication(app);

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
		} catch (Exception e) {
			org.junit.Assert
					.fail("Error with " + runConfiguration + " in Mehtod testJDeleteApplication +\n" + e.getMessage());
		}
	}
}
