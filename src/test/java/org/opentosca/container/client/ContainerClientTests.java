package org.opentosca.container.client;

import io.swagger.client.model.ServiceTemplateInstanceDTO;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.winery.accountability.exceptions.AccountabilityException;
import org.eclipse.winery.repository.exceptions.RepositoryCorruptException;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.opentosca.container.client.model.*;
import org.springframework.boot.test.context.SpringBootTest;

import javax.xml.namespace.QName;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@SpringBootTest
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ContainerClientTests {

    private final String TESTAPPLICATIONSREPOSITORY = "https://github.com/OpenTOSCA/tosca-definitions-test-applications";
    private final Path csarPath;
    private final String applicationName;
    private final QName csarId = new QName("http://opentosca.org/test/applications/servicetemplates", "MyTinyToDo-DockerEngine-Test_w1-wip1");
    private final ContainerClient client;
    private final String containerHost = "localhost";
    private final String containerPort = "1337";
    private final TestUtils testUtils = new TestUtils();

    public ContainerClientTests() throws GitAPIException, AccountabilityException, RepositoryCorruptException, IOException, ExecutionException, InterruptedException {
        this.client = ContainerClientBuilder.builder()
                .withHostname(this.containerHost)
                .build();
        this.csarPath = testUtils.fetchCsar(TESTAPPLICATIONSREPOSITORY, csarId);
        this.applicationName = this.csarPath.getFileName().toString();
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
        Application application = client.uploadApplication(this.csarPath);
        Assert.assertEquals(this.csarPath.getFileName().toString(), application.getId());

        List<Application> applications = client.getApplications();
        Assert.assertEquals(1, applications.size());
    }


    @Test
    public void test_21_get_boundary_definition_properties() {
        Application application = client.getApplication(this.applicationName).orElseThrow(IllegalStateException::new);
        BoundaryDefinitionProperties properties = application.getBoundaryDefinitionProperties();
        Assert.assertNotNull(properties);
    }


    @Test
    public void test_30_provision_application() {
        Application application = client.getApplication(this.applicationName).orElseThrow(IllegalStateException::new);
        Assert.assertEquals(0, client.getApplicationInstances(application, ServiceTemplateInstanceDTO.StateEnum.CREATED).size());
        int startSize = client.getApplicationInstances(application).size();
        ApplicationInstance instance = client.provisionApplication(application, this.testUtils.getProvisioningInputParameters());
        Assert.assertNotNull(instance);
        Assert.assertEquals(ServiceTemplateInstanceDTO.StateEnum.CREATED, instance.getState());
        Assert.assertEquals(startSize + 1, client.getApplicationInstances(application).size());
        Assert.assertEquals(1, client.getApplicationInstances(application, ServiceTemplateInstanceDTO.StateEnum.CREATED).size());
    }

    @Test
    public void test_40_get_application_instances() {
        Application application = client.getApplication(this.applicationName).orElseThrow(IllegalStateException::new);
        List<ApplicationInstance> applicationInstances = client.getApplicationInstances(application, ServiceTemplateInstanceDTO.StateEnum.CREATED);
        Assert.assertEquals(1, applicationInstances.size());
        for (ApplicationInstance instance : applicationInstances) {
            Assert.assertEquals(PlanType.TERMINATION, instance.getTerminationPlan().getType());
            List<Plan> managementPlans = instance.getManagementPlans();
            for (Plan plan : managementPlans) {
                Assert.assertEquals(PlanType.MANAGEMENT, plan.getType());
            }
            instance.getNodeInstances().forEach(i -> {
                if (i.getTemplate().contains("DockerEngine")) {
                    Assert.assertEquals("tcp://" + this.testUtils.getDockerHost() + ":2375", i.getProperties().get("DockerEngineURL"));
                }
                if (i.getTemplate().contains("MyTinyToDoDockerContainer")) {
                    Assert.assertEquals(i.getProperties().get("ContainerPort"), "80");
                }
            });
        }
    }

    @Test
    public void test_45_execute_node_operation() {
        Application application = client.getApplication(this.applicationName).orElseThrow(IllegalStateException::new);
        List<ApplicationInstance> applicationInstances = client.getApplicationInstances(application, ServiceTemplateInstanceDTO.StateEnum.CREATED);
        Assert.assertEquals(1, applicationInstances.size());
        for (ApplicationInstance instance : applicationInstances) {
            instance.getNodeInstances().forEach(i -> {
                if (i.getTemplate().contains("MyTinyToDoDockerContainer")) {
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

    @Test
    public void test_50_get_buildplan_instances() {
        Application application = client.getApplication(this.applicationName).orElseThrow(IllegalStateException::new);
        List<PlanInstance> buildPlanInstances = application.getBuildPlanInstances();
        Assert.assertNotNull(buildPlanInstances);
        buildPlanInstances.forEach(Assert::assertNotNull);
    }

    @Test
    public void test_60_terminate_instance() {
        Application application = client.getApplication(this.applicationName).orElseThrow(IllegalStateException::new);
        List<ApplicationInstance> applicationInstances = client.getApplicationInstances(application, ServiceTemplateInstanceDTO.StateEnum.CREATED);

        Assert.assertEquals(1, applicationInstances.size());
        for (ApplicationInstance instance : applicationInstances) {
            String serviceInstanceUrl = this.testUtils.getServiceInstanceURL(this.containerHost, this.containerPort, this.applicationName, application.getServiceTemplate().getId(), instance.getId());
            Map<String, String> params = this.testUtils.getTerminationPlanInputParameters(serviceInstanceUrl);
            Assert.assertTrue(client.terminateApplicationInstance(instance, params));
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
