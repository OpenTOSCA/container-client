package org.opentosca.containerapi.client;

import java.util.Map;

public class RunParams {

	String testCsarName;
	Map<String, String> testInputParams;
	
	public RunParams() {
	}
	
	public RunParams(String testCsarName, Map<String, String> testInputParams) {
		this.testCsarName = testCsarName;
		this.testInputParams = testInputParams;
	}
}
