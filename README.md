[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![](https://jitpack.io/v/OpenTOSCA/container-client.svg)](https://jitpack.io/#OpenTOSCA/container-client)

# OpenTOSCA Container Client

Part of the [OpenTOSCA Ecosystem](http://www.opentosca.org)

## Development

* Run `mvn generate-sources` for generating Swagger API client and `mvn clean package` to build the project.
* Install Lombok in your IDE (https://projectlombok.org)

## User Guide

### Create Client and List Applications

```java
String dockerHost="localhost";
        Integer port=1337;
        ContainerClient containerClient=new ContainerClientBuilder()
        .withHostname(dockerHost)
        .withPort(port)
        .build();
        List<Application> applications=containerClient.getApplications();
```

### Upload an Application

To upload an application, specify the path to your *.csar* file and call the *uploadApplication* method.

```java
Path applicationPath=Paths.get("MyTinyToDo_Bare_Docker.csar");
        Application application=containerClient.uploadApplication(applicationPath);
```

### Provision an Application

```java
// The application you want to provision.
Application application;
        Map<String, String> inputParameters=new HashMap<>();
        inputParameters.put("DockerEngineURL","tcp://dind:2375");
        inputParameters.put("ApplicationPort","9990");
        ApplicationInstance applicationInstance=containerClient.provisionApplication(application,inputParameters);
```

### Retrieve Application Instances

```java
// The application you want to retrieve application instances from.
Application application;
        List<ApplicationInstance> applicationInstances=containerClient.getApplicationInstances(application);
// Optionally you may retrieve applications with a certain state:
        List<ApplicationInstance> applicationInstances=containerClient.getApplicationInstances(application,ServiceTemplateInstanceDTO.StateEnum.CREATED);
```

### Execute Node Operation

```java
// The application instance you want to execute a node operation on.
ApplicationInstance applicationInstance;
        List<NodeInstance> nodeInstances=applicationInstance.getNodeInstances();
        List<NodeInstance> myTinyToDoDockerNodeInstances=nodeInstances.stream().filter(x->(x.getTemplate().equals("MyTinyToDoDockerContainer"))).collect(Collectors.toList());
        NodeInstance nodeInstance=myTinyToDoDockerNodeInstances.get(0);
        Map<String, String> inputParameters=new HashMap<>();
        inputParameters.put("Script","ls");
// After execution of the node operation, a Map with response headers will be returned.
        Map<String, String> response=containerClient.executeNodeOperation(applicationInstance,nodeInstance,"ContainerManagementInterface","runScript",inputParameters);
```

### Access Application Information

To access further information about an application there are following options available:

```java
// The application you want to retrieve further information from.
Application application;
        Plan buildPlan=application.getBuildPlan();
        List<PlanInstance> buildPlanInstances=application.getBuildPlanInstances();
        String id=application.getId();
        String name=application.getName();
        List<NodeTemplate> nodeTemplates=application.getNodeTemplates();
        ServiceTemplate serviceTemplate=application.getServiceTemplate();
```

### Access Application Instance Information

To access further information about an ApplicationInstance, there are following options available:

```java
// The applicationInstance you want to access properties from.
ApplicationInstance applicationInstance;
        Plan terminationPlan=applicationInstance.getTerminationPlan();
        Application application=applicationInstance.getApplication();
        DateTime createdAt=applicationInstance.getCreatedAt();
        String id=applicationInstance.getId();
        List<PlanInstance> managementPlanInstances=applicationInstance.getManagementPlanInstances();
        List<Plan> managementPlans=applicationInstance.getManagementPlans();
        List<NodeInstance> nodeInstances=applicationInstance.getNodeInstances();
        ServiceTemplateInstanceDTO.StateEnum state=applicationInstance.getState();
```

### Access Node Instance Information

To access further information about any NodeInstance, there are following options available:

```java
// The NodeInstance you want to access properties from.
NodeInstance nodeInstance;
        Map<String, String> properties=nodeInstance.getProperties();
        String template=nodeInstance.getTemplate();
        String id=nodeInstance.getId();
        NodeTemplateInstanceDTO.StateEnum state=nodeInstance.getState();
        String templateType=nodeInstance.getTemplateType();
```

### Access NodeTemplate Information

To access further information about any NodeTemplate, there are following options available:

```java
// The NodeTemplate you want to access properties from.
NodeTemplate nodeTemplate;
        String id=nodeTemplate.getId();
        List<InterfaceDTO> interfaces=nodeTemplate.getInterfaces();
        String name=nodeTemplate.getName();
        String nodeType=nodeTemplate.getNodeType();
```

### Access Plan Information

To access further information about a Plan, there are following options available:

```java
// The plan you want to access further information from.
Plan plan;
        String id=plan.getId();
        List<String> inputParameters=plan.getInputParameters();
        String name=plan.getName();
        PlanType type=plan.getType();
        boolean buildPlan=plan.isBuildPlan();
        boolean managementPlan=plan.isManagementPlan();
        boolean terminationPlan=plan.isTerminationPlan();
```

### Access PlanInstance Information

To access further information about any PlanInstance, there are following options available:

```java
// The PlanInstance you want to access further information from.
PlanInstance planInstance;
        String correlationId=planInstance.getCorrelationId();
        List<PlanInstanceInputDTO> inputs=planInstance.getInputs();
        List<PlanInstanceEventDTO> logs=planInstance.getLogs();
        Map<String, String> outputMappings=planInstance.getOutputMappings();
        Long serviceTemplateInstanceId=planInstance.getServiceTemplateInstanceId();
        PlanInstanceDTO.StateEnum state=planInstance.getState();
        PlanInstanceDTO.TypeEnum type=planInstance.getType();
```

### Access ServiceTemplate Information

To access further information about any ServiceTemplate, there are following options available:

```java
// The ServiceTemplate you want to access further information from.
ServiceTemplate serviceTemplate;
        String id=serviceTemplate.getId();
        String name=serviceTemplate.getName();
```

### Terminate Application Instance

```java
// The applicationInstance you want to terminate.
ApplicationInstance applicationInstance;
        boolean success=containerClient.terminateApplicationInstance(applicationInstance);
```

### Remove Application

```java
// The application you want to remove.
Application application;
        boolean success=containerClient.removeApplication(application);
```

## Haftungsausschluss

Dies ist ein Forschungsprototyp. Die Haftung für entgangenen Gewinn, Produktionsausfall, Betriebsunterbrechung,
entgangene Nutzungen, Verlust von Daten und Informationen, Finanzierungsaufwendungen sowie sonstige Vermögens- und
Folgeschäden ist, außer in Fällen von grober Fahrlässigkeit, Vorsatz und Personenschäden ausgeschlossen.

## Disclaimer of Warranty

Unless required by applicable law or agreed to in writing, Licensor provides the Work (and each Contributor provides its
Contributions) on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied, including,
without limitation, any warranties or conditions of TITLE, NON-INFRINGEMENT, MERCHANTABILITY, or FITNESS FOR A
PARTICULAR PURPOSE. You are solely responsible for determining the appropriateness of using or redistributing the Work
and assume any risks associated with Your exercise of permissions under this License.
