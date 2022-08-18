package org.opentosca.container.client;

import io.swagger.client.model.ServiceTemplateInstanceDTO;
import org.opentosca.container.client.model.Application;
import org.opentosca.container.client.model.ApplicationInstance;
import org.opentosca.container.client.model.NodeInstance;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface ContainerClientAsync {

    CompletableFuture<List<Application>> getApplicationsAsync();

    CompletableFuture<Optional<Application>> getApplicationAsync(String id);

    CompletableFuture<Application> uploadApplicationAsync(Path path);

    CompletableFuture<Boolean> removeApplicationAsync(Application application);

    CompletableFuture<ApplicationInstance> provisionApplicationAsync(Application application, Map<String, String> inputParameters);

    CompletableFuture<List<ApplicationInstance>> getApplicationInstancesAsync(Application application);

    CompletableFuture<List<ApplicationInstance>> getApplicationInstancesAsync(Application application, ServiceTemplateInstanceDTO.StateEnum... state);

    CompletableFuture<Optional<ApplicationInstance>> getApplicationInstanceAsync(Application application, String id);

    CompletableFuture<Boolean> terminateApplicationInstanceAsync(ApplicationInstance instance, Map<String, String> inputParameters);

    CompletableFuture<Map<String, String>> executeNodeOperationAsync(ApplicationInstance instance, NodeInstance node, String interfaceName, String operationName, Map<String, String> parameters);
}
