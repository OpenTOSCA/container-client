package org.opentosca.container.client;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import io.swagger.client.model.ServiceTemplateInstanceDTO;
import lombok.Getter;
import lombok.Setter;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.opentosca.container.client.model.Application;
import org.opentosca.container.client.model.ApplicationInstance;
import org.opentosca.container.client.model.Plan;
import org.opentosca.container.client.model.PlanType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@SpringBootApplication
@EnableConfigurationProperties(ContainerClientTests.Config.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ContainerClientTests {

    @Autowired
    private ContainerClientTests.Config config;

    private ContainerClient client;

    @Before
    public void before() {
        this.client = ContainerClientBuilder.builder()
                .withHostname(config.getHostname())
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
        for (Config.Test test : config.getTests()) {
            Assert.assertFalse(client.getApplication(test.getName()).isPresent());
            Path path = Paths.get(config.getPath(), test.getName());
            Application application = client.uploadApplication(path);
            Assert.assertEquals(test.getName(), application.getId());
        }
        List<Application> applications = client.getApplications();
        Assert.assertEquals(config.getTests().size(), applications.size());
    }

    @Test
    public void test_30_provision_application() {
        for (Config.Test test : config.getTests()) {
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
        for (Config.Test test : config.getTests()) {
            Application application = client.getApplication(test.getName()).orElseThrow(IllegalStateException::new);
            List<ApplicationInstance> applicationInstances = client.getApplicationInstances(application, ServiceTemplateInstanceDTO.StateEnum.CREATED);
            Assert.assertEquals(1, applicationInstances.size());
            for (ApplicationInstance instance : applicationInstances) {
                Assert.assertEquals(PlanType.TERMINATION, instance.getTerminationPlan().getType());
                List<Plan> managementPlans = instance.getManagementPlans();
                for (Plan plan : managementPlans) {
                    Assert.assertEquals(PlanType.MANAGEMENT, plan.getType());
                }
            }
        }
    }

    @Test
    public void test_50_terminate_instance() {
        for (Config.Test test : config.getTests()) {
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

    public static void main(String[] args) {
        SpringApplication.run(ContainerClientTests.class, args);
    }

    @Setter
    @Getter
    @ConfigurationProperties(prefix = "csar")
    static class Config {

        private String hostname;
        private String path;
        private List<Test> tests;

        @Setter
        @Getter
        static class Test {

            private String name;
            private Map<String, String> input;
        }
    }
}
