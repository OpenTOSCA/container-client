package org.opentosca.container.client;

import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(ClientTests.Config.class)
public class ClientTests {

    public static void main(String[] args) {
        SpringApplication.run(ClientTests.class, args);
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
        static class Test {

            private String name;
            private Map<String, String> input;
        }
    }
}
