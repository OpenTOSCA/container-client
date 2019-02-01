package org.opentosca.container.client;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.swagger.client.ApiException;
import io.swagger.client.api.DefaultApi;
import io.swagger.client.model.CsarDTO;
import io.swagger.client.model.InterfaceDTO;
import io.swagger.client.model.NodeTemplateDTO;
import io.swagger.client.model.NodeTemplateInstanceDTO;
import io.swagger.client.model.PlanDTO;
import io.swagger.client.model.PlanInstanceDTO;
import io.swagger.client.model.ServiceTemplateDTO;
import io.swagger.client.model.ServiceTemplateInstanceDTO;
import io.swagger.client.model.TParameter;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;
import org.opentosca.container.client.model.Application;
import org.opentosca.container.client.model.ApplicationInstance;
import org.opentosca.container.client.model.NodeInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.opentosca.container.client.Exceptions.rethrow;

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
                    applications.add(getApplication(csar.getId()).orElseThrow(IllegalStateException::new));
                }
                future.complete(applications);
            } catch (Exception e) {
                logger.error("Error executing request", e);
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    @Override
    public CompletableFuture<Optional<Application>> getApplicationAsync(String id) {
        CompletableFuture<Optional<Application>> future = new CompletableFuture<>();
        executor.submit(() -> {
            try {
                CsarDTO csar = this.client.getCsar(id);
                ServiceTemplateDTO serviceTemplate = this.client.getServiceTemplates(id).getServiceTemplates().get(0);
                String serviceTemplateId = encodeValue(serviceTemplate.getId());
                List<InterfaceDTO> interfaces = this.client.getBoundaryDefinitionInterfaces(csar.getId(), encodeValue(serviceTemplate.getId())).getInterfaces();
                PlanDTO buildPlan = this.client.getBuildPlans(csar.getId(), encodeValue(serviceTemplate.getId())).getPlans().get(0);
                List<NodeTemplateDTO> nodeTemplates = this.client.getNodeTemplates(csar.getId(), serviceTemplateId).getNodeTemplates();
                Application application = Application.builder()
                        .csar(csar)
                        .serviceTemplate(serviceTemplate)
                        .nodeTemplates(nodeTemplates)
                        .buildPlan(buildPlan)
                        .interfaces(interfaces)
                        .build();
                future.complete(Optional.of(application));
            } catch (ApiException e) {
                logger.error("HTTP response code {} while executing request", e.getCode());
                future.complete(Optional.empty());
            } catch (Exception e) {
                logger.error("Error executing request", e);
                future.completeExceptionally(e);
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
                    future.complete(getApplication(file.getName()).orElseThrow(IllegalStateException::new));
                } else {
                    logger.error("HTTP response code {} while uploading file", status);
                    future.completeExceptionally(new RuntimeException("Failed to upload file: " + file.getAbsolutePath()));
                }
            } catch (Exception e) {
                logger.error("Error executing request", e);
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
            } catch (ApiException e) {
                logger.error("HTTP response code {} while executing request", e.getCode());
                future.complete(false);
            } catch (Exception e) {
                logger.error("Error executing request", e);
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
                String csarId = application.getId();
                String planId = encodeValue(application.getBuildPlan().getId());
                String serviceTemplateId = encodeValue(application.getServiceTemplate().getId());
                List<TParameter> parameters = inputParameters.entrySet().stream().map(e -> {
                    TParameter p = new TParameter();
                    p.setName(e.getKey());
                    p.setValue(e.getValue());
                    p.setRequired(TParameter.RequiredEnum.YES);
                    p.setType("String");
                    return p;
                }).collect(Collectors.toList());

                String correlationId = this.client.invokeBuildPlan(planId, parameters, csarId, serviceTemplateId);
                PlanInstanceDTO pi = waitForFinishedPlan(planId, correlationId, csarId, serviceTemplateId, null);
                if (pi == null) {
                    throw new RuntimeException("Could not determine plan instance");
                }
                ApplicationInstance i = getApplicationInstance(application, pi.getServiceTemplateInstanceId().toString())
                        .orElseThrow(IllegalStateException::new);
                future.complete(i);
            } catch (Exception e) {
                logger.error("Error executing request", e);
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    @Override
    public CompletableFuture<List<ApplicationInstance>> getApplicationInstancesAsync(Application application) {
        return getApplicationInstancesAsync(application, ServiceTemplateInstanceDTO.StateEnum.values());
    }

    @Override
    public CompletableFuture<List<ApplicationInstance>> getApplicationInstancesAsync(Application application, ServiceTemplateInstanceDTO.StateEnum... state) {
        List<ServiceTemplateInstanceDTO.StateEnum> validStates = Arrays.asList(state);
        CompletableFuture<List<ApplicationInstance>> future = new CompletableFuture<>();
        executor.submit(() -> {
            List<ApplicationInstance> applicationInstances = new ArrayList<>();
            try {
                String csarId = application.getId();
                String serviceTemplateId = encodeValue(application.getServiceTemplate().getId());
                for (ServiceTemplateInstanceDTO dto : this.client.getServiceTemplateInstances(csarId, serviceTemplateId).getServiceTemplateInstances()) {
                    applicationInstances.add(getApplicationInstance(application, dto.getId().toString()).orElseThrow(IllegalStateException::new));
                }
                future.complete(applicationInstances.stream().filter(a -> validStates.contains(a.getState())).collect(Collectors.toList()));
            } catch (Exception e) {
                logger.error("Error executing request", e);
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    @Override
    public CompletableFuture<Optional<ApplicationInstance>> getApplicationInstanceAsync(Application application, String id) {
        CompletableFuture<Optional<ApplicationInstance>> future = new CompletableFuture<>();
        executor.submit(() -> {
            String csarId = application.getId();
            String serviceTemplateId = encodeValue(application.getServiceTemplate().getId());
            try {
                ServiceTemplateInstanceDTO serviceTemplateInstance = this.client.getServiceTemplateInstance(csarId, serviceTemplateId, Long.valueOf(id));
                List<PlanDTO> plans = this.client.getManagementPlans(csarId, serviceTemplateId, Long.valueOf(id)).getPlans();
                List<NodeTemplateInstanceDTO> nodeTemplateInstances = new ArrayList<>();
                application.getNodeTemplates().forEach(rethrow(nodeTemplate -> {
                    String nodeTemplateId = encodeValue(nodeTemplate.getId());
                    nodeTemplateInstances.addAll(this.client.getNodeTemplateInstances(nodeTemplateId, csarId, serviceTemplateId, null, null)
                            .getNodeTemplateInstances()
                            .stream()
                            .filter(n -> n.getServiceTemplateInstanceId().equals(serviceTemplateInstance.getId()))
                            .collect(Collectors.toList()));
                }));

                // TODO: Resolve node properties

                ApplicationInstance applicationInstance = ApplicationInstance.builder()
                        .application(application)
                        .serviceTemplateInstance(serviceTemplateInstance)
                        .nodeTemplateInstances(nodeTemplateInstances)
                        .managementPlans(plans)
                        .build();
                future.complete(Optional.of(applicationInstance));
            } catch (ApiException e) {
                logger.error("HTTP response code {} while executing request", e.getCode());
                future.complete(Optional.empty());
            } catch (Exception e) {
                logger.error("Error executing request", e);
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    @Override
    public CompletableFuture<Boolean> terminateApplicationInstanceAsync(ApplicationInstance instance) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        executor.submit(() -> {
            try {
                Application application = instance.getApplication();
                String csarId = application.getId();
                String planId = encodeValue(instance.getTerminationPlan().getId());
                String serviceTemplateId = encodeValue(application.getServiceTemplate().getId());
                Long id = Long.valueOf(instance.getId());

                String correlationId = this.client.invokeManagementPlan(planId, new ArrayList<>(), csarId, serviceTemplateId, id);
                PlanInstanceDTO pi = waitForFinishedPlan(planId, correlationId, csarId, serviceTemplateId, id);
                if (pi == null) {
                    throw new RuntimeException("Could not determine plan instance");
                }
                ApplicationInstance i = getApplicationInstance(application, pi.getServiceTemplateInstanceId().toString())
                        .orElseThrow(IllegalStateException::new);
                if (i.getState().equals(ServiceTemplateInstanceDTO.StateEnum.DELETED)) {
                    future.complete(true);
                } else {
                    future.complete(false);
                }
            } catch (Exception e) {
                logger.error("Error executing request", e);
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    @Override
    public CompletableFuture<Map<String, String>> executeNodeOperationAsync(NodeInstance node, String interfaceName, String operationName, Map<String, String> parameters) {
        // TODO: See 'master' and implement this accordingly
        // -> https://github.com/OpenTOSCA/container-client/blob/master/src/org/opentosca/containerapi/client/impl/OpenTOSCAContainerLegacyAPIClient.java#L617

        // Use custom http client -> Client httpClient = this.client.getApiClient().getHttpClient();
        String basePath = this.client.getApiClient().getBasePath(); // "http://localhost:1337"
        // Extract hostname -> regex: ^https?://(.*):(.*)$
        // Actual API endpoint -> "http://" + hostname + ":8086/ManagementBus/v1/invoker";
        // See master...

        throw new UnsupportedOperationException();
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
    public Optional<Application> getApplication(String id) {
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
    public List<ApplicationInstance> getApplicationInstances(Application application, ServiceTemplateInstanceDTO.StateEnum... state) {
        try {
            return getApplicationInstancesAsync(application, state).get();
        } catch (Exception e) {
            logger.error("Error while waiting for future to be completed");
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<ApplicationInstance> getApplicationInstance(Application application, String id) {
        try {
            return getApplicationInstanceAsync(application, id).get();
        } catch (Exception e) {
            logger.error("Error while waiting for future to be completed");
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean terminateApplicationInstance(ApplicationInstance instance) {
        try {
            return terminateApplicationInstanceAsync(instance).get();
        } catch (Exception e) {
            logger.error("Error while waiting for future to be completed");
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map<String, String> executeNodeOperation(NodeInstance node, String interfaceName, String operationName, Map<String, String> parameters) {
        try {
            return executeNodeOperationAsync(node, interfaceName, operationName, parameters).get();
        } catch (Exception e) {
            logger.error("Error while waiting for future to be completed");
            throw new RuntimeException(e);
        }
    }

    // === Helper

    private String encodeValue(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private PlanInstanceDTO waitForFinishedPlan(String plan, String instance, String csar, String serviceTemplate, Long id) {
        PlanInstanceDTO pi = null;
        final long sleep = 5000;
        final long timeout = TimeUnit.MINUTES.toMillis(10);
        long waited = 0;
        while (true) {
            boolean finished;
            try {
                if (id == null) {
                    pi = this.client.getBuildPlanInstance(plan, instance, csar, serviceTemplate);
                } else {
                    pi = this.client.getManagementPlanInstance(plan, instance, csar, serviceTemplate, id);
                }
                finished = pi.getState().equals(PlanInstanceDTO.StateEnum.FINISHED);
            } catch (final Exception e) {
                finished = false;
            }
            if (finished) {
                break;
            }
            if (waited >= timeout) {
                logger.warn("Timeout reached, plan has net been finished yet");
                break;
            }
            try {
                Thread.sleep(sleep);
            } catch (final InterruptedException e) {
                // Ignore
            }
            waited += sleep;
        }
        return pi;
    }
}
