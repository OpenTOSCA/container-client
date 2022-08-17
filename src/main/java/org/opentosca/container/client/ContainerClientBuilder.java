package org.opentosca.container.client;

import org.opentosca.container.client.impl.SwaggerContainerClient;

import java.util.concurrent.TimeUnit;

public final class ContainerClientBuilder {

    private String hostname = "localhost";
    private Integer port = 1337;
    private Integer timeout = (int) TimeUnit.MINUTES.toMillis(5);

    public static ContainerClientBuilder builder() {
        return new ContainerClientBuilder();
    }

    public static ContainerClient defaultClient() {
        return builder().build();
    }

    public static ContainerClientAsync defaultClientAsync() {
        return builder().buildAsync();
    }

    public ContainerClientBuilder withHostname(String hostname) {
        this.hostname = hostname;
        return this;
    }

    public ContainerClientBuilder withPort(Integer port) {
        this.port = port;
        return this;
    }

    public ContainerClientBuilder withTimeout(Integer timeout, TimeUnit timeUnit) {
        this.timeout = (int) timeUnit.toMillis(timeout);
        return this;
    }

    public ContainerClient build() {
        return new SwaggerContainerClient("http://" + this.hostname + ":" + this.port, this.timeout);
    }

    public ContainerClientAsync buildAsync() {
        return new SwaggerContainerClient("http://" + this.hostname + ":" + this.port, this.timeout);
    }
}
