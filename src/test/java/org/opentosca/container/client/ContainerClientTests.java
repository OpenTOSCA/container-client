package org.opentosca.container.client;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.net.Inet4Address;
import java.net.InetAddress;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONTokener;
import io.swagger.client.model.ServiceTemplateInstanceDTO;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.junit.runners.Parameterized;
import org.opentosca.container.client.model.Application;
import org.opentosca.container.client.model.ApplicationInstance;
import org.opentosca.container.client.model.BoundaryDefinitionProperties;
import org.opentosca.container.client.model.Plan;
import org.opentosca.container.client.model.PlanInstance;
import org.opentosca.container.client.model.PlanType;
import org.opentosca.container.client.model.PropertyMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

@RunWith(Parameterized.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ContainerClientTests {

    @Parameterized.Parameter(0)
    public Testfiles config;

    private ContainerClient client;
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
                .build();
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
            System.out.println("Uploading CSAR-file "+test.getName());

            Assert.assertFalse(client.getApplication(test.getName()).isPresent());
            Path path = Paths.get(csarsPath, test.getName());
            Application application = client.uploadApplication(path);
            Assert.assertEquals(test.getName(), application.getId());
            System.out.println("Upload of CSAR-file "+test.getName()+" finished");

        }
        List<Application> applications = client.getApplications();

        Assert.assertEquals(config.getTests().size(), applications.size());
    }


    @Test
    public void test_30_provision_application() {

        for (CSARTest test : config.getTests()) {
            System.out.println("Provisioning of "+test.getName());

            Application application = client.getApplication(test.getName()).orElseThrow(IllegalStateException::new);
            Assert.assertEquals(0, client.getApplicationInstances(application, ServiceTemplateInstanceDTO.StateEnum.CREATED).size());
            int startSize = client.getApplicationInstances(application).size();
            ApplicationInstance instance = client.provisionApplication(application, test.getInput());
            System.out.println("Provisioning of "+test.getName()+ " finished");

            Assert.assertNotNull(instance);
            Assert.assertEquals(ServiceTemplateInstanceDTO.StateEnum.CREATED, instance.getState());
            Assert.assertEquals(startSize + 1, client.getApplicationInstances(application).size());
            Assert.assertEquals(1, client.getApplicationInstances(application, ServiceTemplateInstanceDTO.StateEnum.CREATED).size());
        }

    }

    @Test
    public void test_60_terminate_instance() {

        for (CSARTest test : config.getTests()) {
            System.out.println("Terminating instance of "+test.getName());

            Application application = client.getApplication(test.getName()).orElseThrow(IllegalStateException::new);
            List<ApplicationInstance> applicationInstances = client.getApplicationInstances(application, ServiceTemplateInstanceDTO.StateEnum.CREATED);
            Assert.assertEquals(1, applicationInstances.size());
            System.out.println("Termination of "+test.getName()+ " finished");

            for (ApplicationInstance instance : applicationInstances) {
                Assert.assertTrue(client.terminateApplicationInstance(instance));
            }
        }

    }

    @Test
    public void test_90_remove_application() {

        for (Application application : client.getApplications()) {
            System.out.println("Removing "+application.getName());
            Assert.assertTrue(client.removeApplication(application));
            System.out.println(application.getName()+" removed");
        }
        Assert.assertEquals(0, client.getApplications().size());
    }
}
