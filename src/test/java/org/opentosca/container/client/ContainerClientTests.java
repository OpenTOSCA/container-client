package org.opentosca.container.client;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

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
    ContainerClientTests.Config config;

    ContainerClient client;

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
        Assert.assertNull(client.getApplication("test"));
        for (Application app : client.getApplications()) {
            client.removeApplication(app);
        }
        Assert.assertEquals(0, client.getApplications().size());
    }

    @Test
    public void test_20_upload_and_remove() {
        for (Config.Test test : config.getTests()) {
            Assert.assertNull(client.getApplication(test.getName()));
            Path path = Paths.get(config.getPath(), test.getName());
            Application application = client.uploadApplication(path);
            Assert.assertEquals(test.getName(), application.getId());
        }
        List<Application> applications = client.getApplications();
        Assert.assertEquals(config.getTests().size(), applications.size());
        for (Application application : applications) {
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
    public static class Config {

        private String hostname;
        private String path;
        private List<Test> tests;

        @Setter
        @Getter
        public static class Test {

            private String name;
            private Map<String, String> input;
        }
    }
}
