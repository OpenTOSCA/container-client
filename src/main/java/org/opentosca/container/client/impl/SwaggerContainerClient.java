package org.opentosca.container.client.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.client.ApiClient;
import io.swagger.client.ApiException;
import io.swagger.client.api.DefaultApi;
import io.swagger.client.model.*;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;
import org.opentosca.container.client.ContainerClient;
import org.opentosca.container.client.ContainerClientAsync;
import org.opentosca.container.client.data.JSONInvocationData;
import org.opentosca.container.client.data.JSONParameterData;
import org.opentosca.container.client.data.JSONRequestData;
import org.opentosca.container.client.model.Application;
import org.opentosca.container.client.model.ApplicationInstance;
import org.opentosca.container.client.model.NodeInstance;
import org.opentosca.container.client.model.ServiceTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.opentosca.container.client.impl.Exceptions.rethrow;

public class SwaggerContainerClient implements ContainerClient, ContainerClientAsync {

    private static final Logger logger = LoggerFactory.getLogger(SwaggerContainerClient.class);
    private static final String API_ENDPOINT_PROTOCOL = "http";
    private static final String API_ENDPOINT_PORT = "8086";
    private static final String API_ENDPOINT_URL = API_ENDPOINT_PROTOCOL + "://%s:" + API_ENDPOINT_PORT + "/ManagementBus/v1/invoker";
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
                List<NodeInstance> nodeInstances = nodeTemplateInstances.stream().map(rethrow(n -> {
                    String nodeTemplateId = encodeValue(n.getNodeTemplateId());
                    Map<String, Object> properties = this.client.getNodeTemplateInstancePropertiesAsJson(nodeTemplateId, csarId, serviceTemplateId, n.getId());
                    return new NodeInstance(n, properties);
                })).collect(Collectors.toList());
                ApplicationInstance applicationInstance = ApplicationInstance.builder()
                        .application(application)
                        .serviceTemplateInstance(serviceTemplateInstance)
                        .nodeInstances(nodeInstances)
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

        CompletableFuture<Map<String, String>> result = new CompletableFuture<>();
        executor.submit(() -> {
            JSONRequestData requestData = new JSONRequestData();
            JSONInvocationData invocationData = new JSONInvocationData();
            JSONParameterData parameterData = new JSONParameterData();

            // TODO: find correct serviceTemplate
            ServiceTemplate serviceTemplate = null;

            // TODO: set proper data
            invocationData.setCsarID(node.getId());
            invocationData.setServiceTemplateID(serviceTemplate.getId());
            // invocationData.setServiceInstanceID("");
            invocationData.setNodeTemplateID(node.getTemplate());
            invocationData.setInterfaceName(interfaceName);
            invocationData.setOperationName(operationName);

            parameterData.setParams(parameters);

            requestData.setInvocationInformation(invocationData);

            // Convert data to JSON
            ObjectMapper mapper = new ObjectMapper();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            try {
                mapper.writeValue(outputStream, requestData);
            } catch (IOException e) {
                e.printStackTrace();
            }
            String json = new String(outputStream.toByteArray());

            ApiClient apiClient = this.client.getApiClient();
            Client httpClient = apiClient.getHttpClient();
            String basePath = apiClient.getBasePath();
            List<String> hostInfo = extractHostInfo(basePath);
            String hostname = hostInfo.get(1);
            // String port = hostInfo.get(2);
            String apiEndpointUrl = String.format(API_ENDPOINT_URL, hostname);
            WebTarget target = httpClient.target(apiEndpointUrl);
        });

        return result;
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

    private List<String> extractHostInfo(String basePath) {
        Pattern pattern = Pattern.compile("^(https?)://(.+):(.+)$");
        Matcher matcher = pattern.matcher(basePath);
        if (!matcher.matches()) {
            throw new RuntimeException(); // TODO: find proper exception to throw
        }
        MatchResult matchResult = matcher.toMatchResult();
        List<String> result = new ArrayList<>();
        for (int i = 0; i <= matchResult.groupCount(); i++) {
            result.add(matchResult.group(i));
        }
        return result;
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