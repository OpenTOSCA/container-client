package org.opentosca.container.client;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.opentosca.container.client.model.Application;
import org.opentosca.container.client.model.ApplicationInstance;

public interface ContainerClient {

    List<Application> getApplications();

    Application getApplication(String id);

    Application uploadApplication(Path path);

    boolean removeApplication(Application application);

    ApplicationInstance provisionApplication(Application application, Map<String, String> inputParameters);

    boolean terminateApplication(ApplicationInstance instance);

    List<ApplicationInstance> getApplicationInstances(Application application);
}
