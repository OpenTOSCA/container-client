package org.opentosca.containerapi.client;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.json.JSONObject;
import org.junit.runner.*;
import org.junit.runner.notification.Failure;


public class TestRunner {
	
	public static void main(String[] args) {
		if (args.length > 1) { //arg[0] = csars_dir arg[1] = opentosca container host
			String directory = args[0];
			String testContainerHost = args[1];
			//iterate in directory and search for .csar
			File[] csarFiles = new File(directory).listFiles(new FilenameFilter() {
			  public boolean accept(File dir, String name) {
			     return name.endsWith(".csar");		  
			}});
			
			for (File csarFile : csarFiles) {
				File testParams = new File("testParams.txt");
				JSONObject params = new JSONObject();
				params.put("csarPath", csarFile.getPath());
				params.put("csarName", csarFile.getName());
				params.put("containerHost", testContainerHost);
				//System.out.println(params.toString());
				
				
				try {
					FileUtils.writeStringToFile(testParams, params.toString(), "UTF-8");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				// runs tests
				Result result = JUnitCore.runClasses(ContainerAPIClientTestJUnit.class);
				
				for (Failure failure : result.getFailures()) {
					System.out.println(failure.toString());
				}
			
				System.out.println(result.wasSuccessful());
			}
			
		} else {
			System.out.println("ERROR: arguments missing.  Please provide then as arguments:\n arg[0] = directory path to csars; \n arg[1] = opentosca container host IP.");
		}
		
		

	}
}
