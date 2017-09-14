package org.opentosca.containerapi.client;

import java.util.List;
import java.util.Map;

import org.opentosca.containerapi.client.TestRunConfiguration.TestInstanceConfiguration;

public class TestRunConfiguration {

	/*
	 * { "csarPath": "/Users/kalmankepes/Documents/temp/scalingplans",
	 * "containerHost": "localhost", "csarsTests": [ { "csarName":
	 * "MyTinyToDo_Bare_Docker.csar", "inputParams": [ { "DockerEngineURL":
	 * "tcp://localhost:2375", "DockerEngineCertificate": "", "ApplicationPort":
	 * "80" } ], "instanceRun": [ { "interfaceName": "scaleout_dockercontainer",
	 * "operationName": "scale-out", "inputParams": [ { "DockerEngineURL":
	 * "tcp://localhost:2375", "DockerEngineCertificate": "", "ApplicationPort":
	 * "81" } ] } ] } ] }
	 */

	String directoryPath;
	String containerHost;
	String testCsarName;
	Map<String, String> testInputParams;

	List<TestInstanceConfiguration> instanceRuns;

	public static class TestInstanceConfiguration {
		public String interfaceName;
		public String operationName;
		public Map<String, String> inputParams;

		public TestInstanceConfiguration(String interfaceName, String operationName, Map<String, String> inputParams) {
			this.inputParams = inputParams;
			this.interfaceName = interfaceName;
			this.operationName = operationName;
		}

		@Override
		public String toString() {
			return "Interface name: " + this.interfaceName + ", Operation name: " + this.operationName
					+ ", Input parmeters: " + this.inputParams;
		}
	}

	public TestRunConfiguration(String csarPath, String containerHost, String testCsarName, Map<String, String> testInputParams) {
		this.testCsarName = testCsarName;
		this.testInputParams = testInputParams;
	}

	public TestRunConfiguration(String csarPath, String containerHost, String testCsarName, Map<String, String> testInputParams,
			List<TestInstanceConfiguration> instanceRuns) {
		this.testCsarName = testCsarName;
		this.testInputParams = testInputParams;
		this.directoryPath = csarPath;
		this.containerHost = containerHost;
		this.instanceRuns = instanceRuns;
	}

	public String toString() {
		String toString = "CSAR directory path: " + this.directoryPath + ", Container host: " + this.containerHost
				+ ", CSAR: " + this.testCsarName + ", CSAR file path: " + this.directoryPath
				+ ", Input Parameters: " + this.testInputParams;

		if (this.instanceRuns != null && !this.instanceRuns.isEmpty()) {
			toString += ", Instance Run Configurations:[ ";
			for (TestInstanceConfiguration instanceRun : this.instanceRuns) {
				toString += instanceRun;
			}
			toString += " ]";
		}
		return toString;
	}
}
