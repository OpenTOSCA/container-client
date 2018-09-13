package org.opentosca.container.client;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.swagger.client.api.DefaultApi;
import io.swagger.client.model.CsarDTO;
import io.swagger.client.model.InterfaceDTO;
import io.swagger.client.model.PlanDTO;
import io.swagger.client.model.ServiceTemplateDTO;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;
import org.opentosca.container.client.model.Application;
import org.opentosca.container.client.model.ApplicationInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SwaggerContainerClient implements ContainerClient, ContainerClientAsync {

    private static final Logger logger = LoggerFactory.getLogger(SwaggerContainerClient.class);

    private final DefaultApi client = new DefaultApi();
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public SwaggerContainerClient(final String basePath, int timeout) {
        this.client.getApiClient().setBasePath(basePath);
        this.client.getApiClient().setReadTimeout(timeout);
        this.client.getApiClient().setConnectTimeout(timeout);
    }

    // === Async

    @Override
    public CompletableFuture<List<Application>> getApplicationsAsync() {
        CompletableFuture<List<Application>> future = new CompletableFuture<>();
        executor.submit(() -> {
            List<Application> applications = new ArrayList<>();
            try {
                for (CsarDTO csar : this.client.getCsars().getCsars()) {
                    applications.add(getApplication(csar.getId()));
                }
                future.complete(applications);
            } catch (Exception e) {
                logger.error("Error executing request: {}", e.getMessage(), e);
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    @Override
    public CompletableFuture<Application> getApplicationAsync(String id) {
        CompletableFuture<Application> future = new CompletableFuture<>();
        executor.submit(() -> {
            try {
                CsarDTO csar = this.client.getCsar(id);
                ServiceTemplateDTO serviceTemplate = this.client.getServiceTemplates(id).getServiceTemplates().get(0);
                List<InterfaceDTO> interfaces = this.client.getBoundaryDefinitionInterfaces(csar.getId(), encodeValue(serviceTemplate.getId())).getInterfaces();
                PlanDTO buildPlan = this.client.getBuildPlans(csar.getId(), encodeValue(serviceTemplate.getId())).getPlans().get(0);
                future.complete(Application.builder()
                        .csar(csar)
                        .serviceTemplate(serviceTemplate)
                        .buildPlan(buildPlan)
                        .interfaces(interfaces)
                        .build());
            } catch (Exception e) {
                logger.error("Error executing request: {}", e.getMessage(), e);
                future.complete(null);
            }
        });
        return future;
    }

    @Override
    public CompletableFuture<Application> uploadApplicationAsync(Path path) {
        CompletableFuture<Application> future = new CompletableFuture<>();
        executor.submit(() -> {
            try {
                File file = path.toFile();

                if (!file.exists()) {
                    throw new FileNotFoundException("File does not exist");
                }

                Client httpClient = this.client.getApiClient().getHttpClient();
                FormDataMultiPart formData = new FormDataMultiPart();
                formData.bodyPart(new FileDataBodyPart("file", file, MediaType.APPLICATION_OCTET_STREAM_TYPE));
                Response response = httpClient
                        .target(this.client.getApiClient().getBasePath() + "/csars")
                        .request()
                        .post(Entity.entity(formData, formData.getMediaType()));

                int status = response.getStatus();
                if (status >= 200 && status < 400) {
                    future.complete(getApplication(file.getName()));
                } else {
                    logger.error("HTTP error {} while uploading file", status);
                    future.completeExceptionally(new RuntimeException("Failed to upload file: " + file.getAbsolutePath()));
                }
            } catch (Exception e) {
                logger.error("Error executing request: {}", e.getMessage(), e);
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    @Override
    public CompletableFuture<Boolean> removeApplicationAsync(Application application) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        executor.submit(() -> {
            try {
                this.client.deleteCsar(application.getId());
                future.complete(true);
            } catch (Exception e) {
                logger.error("Error executing request: {}", e.getMessage(), e);
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    @Override
    public CompletableFuture<ApplicationInstance> provisionApplicationAsync(Application application, Map<String, String> inputParameters) {
        CompletableFuture<ApplicationInstance> future = new CompletableFuture<>();
        executor.submit(() -> {
            try {
                // TODO: Wait for plan to be completed
                // TODO this.client.invokeBuildPlan(application.getBuildPlan().getId(), null, application.getId(), application.getServiceTemplate().getId());
            } catch (Exception e) {
                logger.error("Error executing request: {}", e.getMessage(), e);
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    @Override
    public CompletableFuture<List<ApplicationInstance>> getApplicationInstancesAsync(Application application) {
        CompletableFuture<List<ApplicationInstance>> future = new CompletableFuture<>();
        executor.submit(() -> {
            // TODO
            future.completeExceptionally(new UnsupportedOperationException());
        });
        return future;
    }

    @Override
    public CompletableFuture<Boolean> terminateApplicationAsync(ApplicationInstance instance) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        executor.submit(() -> {
            // TODO
            future.completeExceptionally(new UnsupportedOperationException());
        });
        return future;
    }

    // === Sync

    @Override
    public List<Application> getApplications() {
        try {
            return getApplicationsAsync().get();
        } catch (Exception e) {
            logger.error("Error while waiting for future to be completed");
            throw new RuntimeException(e);
        }
    }

    @Override
    public Application getApplication(String id) {
        try {
            return getApplicationAsync(id).get();
        } catch (Exception e) {
            logger.error("Error while waiting for future to be completed");
            throw new RuntimeException(e);
        }
    }

    @Override
    public Application uploadApplication(Path path) {
        try {
            return uploadApplicationAsync(path).get();
        } catch (Exception e) {
            logger.error("Error while waiting for future to be completed");
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean removeApplication(Application application) {
        try {
            return removeApplicationAsync(application).get();
        } catch (Exception e) {
            logger.error("Error while waiting for future to be completed");
            throw new RuntimeException(e);
        }
    }

    @Override
    public ApplicationInstance provisionApplication(Application application, Map<String, String> inputParameters) {
        try {
            return provisionApplicationAsync(application, inputParameters).get();
        } catch (Exception e) {
            logger.error("Error while waiting for future to be completed");
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<ApplicationInstance> getApplicationInstances(Application application) {
        try {
            return getApplicationInstancesAsync(application).get();
        } catch (Exception e) {
            logger.error("Error while waiting for future to be completed");
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean terminateApplication(ApplicationInstance instance) {
        try {
            return terminateApplicationAsync(instance).get();
        } catch (Exception e) {
            logger.error("Error while waiting for future to be completed");
            throw new RuntimeException(e);
        }
    }

    private String encodeValue(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
