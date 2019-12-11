package org.opentosca.container.client;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.swagger.client.model.ServiceTemplateInstanceDTO;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.junit.runners.Parameterized;
import org.opentosca.container.client.impl.SmartServiceSwaggerContainerClient.NoSmartServiceException;
import org.opentosca.container.client.model.Application;
import org.opentosca.container.client.model.ApplicationInstance;
import org.opentosca.container.client.model.Plan;
import org.opentosca.container.client.model.PlanInstance;
import org.opentosca.container.client.model.PlanType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

@RunWith(Parameterized.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class SmartServiceContainerClientTests {

    @Parameterized.Parameter(0)
    public Testfiles config;

    private SmartServiceContainerClient client;
    //retrieves the ipaddress which is defined in the dockerfile of the tests project
    String ipAdress=System.getProperty("ipaddress");
    String csarsPath="/var/opentosca/csars/";
    //import all yaml configurations located in the resources folder
    @Parameterized.Parameters
    public static Iterable<Testfiles> data(){
        File dir = new File("src/test/resources");
        File[] directoryListing= dir.listFiles();
        List<Testfiles> list = new ArrayList<>();
        for(File child: directoryListing){
            Yaml yaml = new Yaml(new Constructor(Testfiles.class));
            try(InputStream in = Files.newInputStream(Paths.get(child.getAbsolutePath()))){
        Testfiles tempconfig= yaml.load(in);
        list.add(tempconfig);

            }catch (Exception e){
                e.printStackTrace();
            }
        }
        return list;
    }

    @Before
    public void before() {
        this.client = ContainerClientBuilder.builder()
                .withHostname(ipAdress)
                .buildSmart();
        // Only run tests if OpenTOSCA ecosystem is up and running ;-)
        try {
            client.getApplications();
        } catch (Exception e) {
            Assume.assumeNoException(e);
        }
    }

    @Test
    public void test_10_empty_responses() {
        Assert.assertFalse(client.getApplication("test").isPresent());
        for (Application application : client.getApplications()) {
            client.removeApplication(application);
        }
        Assert.assertEquals(0, client.getApplications().size());
    }

    @Test
    public void test_20_upload() {
        for (CSARTest test : config.getTests()) {
            Assert.assertFalse(client.getApplication(test.getName()).isPresent());
            Path path = Paths.get(csarsPath, test.getName());
            Application application = client.uploadApplication(path);
            Assert.assertEquals(test.getName(), application.getId());
            try {
                JSONObject smartServiceDescription = client.getSmartServiceDescription(application);
                Assert.assertNotNull(smartServiceDescription);
            } catch (NoSmartServiceException e) {
                Assert.fail("Application under test is not a smart service");
            }
        }
        List<Application> applications = client.getApplications();
        Assert.assertEquals(config.getTests().size(), applications.size());
    }

    @Test
    public void test_30_provision_application() {
        for (CSARTest test : config.getTests()) {
            Application application = client.getApplication(test.getName()).orElseThrow(IllegalStateException::new);
            Assert.assertEquals(0, client.getApplicationInstances(application, ServiceTemplateInstanceDTO.StateEnum.CREATED).size());
            int startSize = client.getApplicationInstances(application).size();
            ApplicationInstance instance = client.provisionApplication(application, test.getInput());
            Assert.assertNotNull(instance);
            Assert.assertEquals(ServiceTemplateInstanceDTO.StateEnum.CREATED, instance.getState());
            Assert.assertEquals(startSize + 1, client.getApplicationInstances(application).size());
            Assert.assertEquals(1, client.getApplicationInstances(application, ServiceTemplateInstanceDTO.StateEnum.CREATED).size());
        }
    }

    @Test
    public void test_40_get_application_instances() {
        for (CSARTest test : config.getTests()) {
            Application application = client.getApplication(test.getName()).orElseThrow(IllegalStateException::new);
            List<ApplicationInstance> applicationInstances = client.getApplicationInstances(application, ServiceTemplateInstanceDTO.StateEnum.CREATED);
            Assert.assertEquals(1, applicationInstances.size());
            for (ApplicationInstance instance : applicationInstances) {
                Assert.assertEquals(PlanType.TERMINATION, instance.getTerminationPlan().getType());
                List<Plan> managementPlans = instance.getManagementPlans();
                for (Plan plan : managementPlans) {
                    Assert.assertEquals(PlanType.MANAGEMENT, plan.getType());
                }
                instance.getNodeInstances().forEach(i -> {
                    if (i.getTemplate().equals("DockerEngine")) {
                        Assert.assertEquals(i.getProperties().get("DockerEngineURL"), "tcp://dind:2375");
                    }
                    if (i.getTemplate().equals("MyTinyToDoDockerContainer")) {
                        Assert.assertEquals(i.getProperties().get("ContainerPort"), "80");
                    }
                });
            }
        }
    }

    @Test
    public void test_45_execute_node_operation() {
        for (CSARTest test : config.getTests()) {
            Application application = client.getApplication(test.getName()).orElseThrow(IllegalStateException::new);
            List<ApplicationInstance> applicationInstances = client.getApplicationInstances(application, ServiceTemplateInstanceDTO.StateEnum.CREATED);
            Assert.assertEquals(1, applicationInstances.size());
            for (ApplicationInstance instance : applicationInstances) {
                instance.getNodeInstances().forEach(i -> {
                    if (i.getTemplate().equals("MyTinyToDoDockerContainer")) {
                        Map<String, String> input = new HashMap<>();
                        input.put("Script", "ls");
                        Map<String, String> result = client.executeNodeOperation(
                                instance, i,
                                "ContainerManagementInterface", "runScript",
                                input);
                        Assert.assertTrue(result.size() > 0);
                    }
                });
            }
        }
    }

    @Test
    public void test_50_get_buildplan_instances() {
        for (CSARTest test : config.getTests()) {
            Application application = client.getApplication(test.getName()).orElseThrow(IllegalStateException::new);
            List<PlanInstance> buildPlanInstances = application.getBuildPlanInstances();
            Assert.assertNotNull(buildPlanInstances);
            buildPlanInstances.forEach(Assert::assertNotNull);
        }
    }

    @Test
    public void test_60_terminate_instance() {
        for (CSARTest test : config.getTests()) {
            Application application = client.getApplication(test.getName()).orElseThrow(IllegalStateException::new);
            List<ApplicationInstance> applicationInstances = client.getApplicationInstances(application, ServiceTemplateInstanceDTO.StateEnum.CREATED);
            Assert.assertEquals(1, applicationInstances.size());
            for (ApplicationInstance instance : applicationInstances) {
                Assert.assertTrue(client.terminateApplicationInstance(instance));
            }
        }
    }

    @Test
    public void test_90_remove_application() {
        for (Application application : client.getApplications()) {
            Assert.assertTrue(client.removeApplication(application));
        }
        Assert.assertEquals(0, client.getApplications().size());
    }
}
