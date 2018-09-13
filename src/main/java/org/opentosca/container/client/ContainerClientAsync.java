package org.opentosca.container.client;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.opentosca.container.client.model.Application;
import org.opentosca.container.client.model.ApplicationInstance;

// TODO: Make use of Optionals?!
public interface ContainerClientAsync {

    CompletableFuture<List<Application>> getApplicationsAsync();

    CompletableFuture<Application> getApplicationAsync(String id);

    CompletableFuture<Application> uploadApplicationAsync(Path path);

    CompletableFuture<Boolean> removeApplicationAsync(Application application);

    CompletableFuture<ApplicationInstance> provisionApplicationAsync(Application application, Map<String, String> inputParameters);

    CompletableFuture<Boolean> terminateApplicationAsync(ApplicationInstance instance);

    CompletableFuture<List<ApplicationInstance>> getApplicationInstancesAsync(Application application);
}
