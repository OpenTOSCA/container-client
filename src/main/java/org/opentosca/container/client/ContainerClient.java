package org.opentosca.container.client;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.swagger.client.model.ServiceTemplateInstanceDTO;
import org.opentosca.container.client.model.Application;
import org.opentosca.container.client.model.ApplicationInstance;
import org.opentosca.container.client.model.NodeInstance;

public interface ContainerClient {

    List<Application> getApplications();

    Optional<Application> getApplication(String id);

    Application uploadApplication(Path path);

    boolean removeApplication(Application application);

    ApplicationInstance provisionApplication(Application application, Map<String, String> inputParameters);

    List<ApplicationInstance> getApplicationInstances(Application application);

    List<ApplicationInstance> getApplicationInstances(Application application, ServiceTemplateInstanceDTO.StateEnum... state);

    Optional<ApplicationInstance> getApplicationInstance(Application application, String id);

    boolean terminateApplicationInstance(ApplicationInstance instance);

    Map<String, String> executeNodeOperation(ApplicationInstance instance, NodeInstance node, String interfaceName, String operationName, Map<String, String> parameters);
}
