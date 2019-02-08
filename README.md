
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![](https://jitpack.io/v/OpenTOSCA/container-client.svg)](https://jitpack.io/#OpenTOSCA/container-client)

# OpenTOSCA Container Client

Part of the [OpenTOSCA Ecosystem](http://www.opentosca.org)

## User Guide

### Create Client and List Applications
```java
String dockerHost = "localhost";
Integer port = 1337;
ContainerClient containerClient = new ContainerClientBuilder()
        .withHostname(dockerHost)
        .withPort(port)
        .build();
List<Application> applications = containerClient.getApplications();
```

### Upload an Application
To upload an application, specify the path to your *.csar* file
and call the *uploadApplication* method.
```java
Path applicationPath = Paths.get("MyTinyToDo_Bare_Docker.csar");
containerClient.uploadApplication(applicationPath);
```

### Provision an Application

```java
// The application you want to provision.
Application application;
Map<String, String> inputParameters = new HashMap<>();
inputParameters.put("DockerEngineURL", "tcp://dind:2375");
inputParameters.put("ApplicationPort", "9990");
ApplicationInstance applicationInstance = containerClient.provisionApplication(application, inputParameters);
```

### Retrieve Application Instances

```java
// The application you want to retrieve application instances from.
Application application = containerClient.getApplications().get(0);
List<ApplicationInstance> applicationInstances = containerClient.getApplicationInstances(application);
// Optionally you may retrieve applications with a certain state:
// List<ApplicationInstance> applicationInstances = containerClient.getApplicationInstances(application, ServiceTemplateInstanceDTO.StateEnum.CREATED);
```

### Execute Node Operation

```java
// The application instance you want to execute a node operation on.
ApplicationInstance applicationInstance;
List<NodeInstance> nodeInstances = applicationInstance.getNodeInstances();
List<NodeInstance> myTinyToDoDockerNodeInstances = nodeInstances.stream().filter(x -> (x.getTemplate().equals("MyTinyToDoDockerContainer"))).collect(Collectors.toList());
NodeInstance nodeInstance = myTinyToDoDockerNodeInstances.get(0);
Map<String, String> inputParameters = new HashMap<>();
inputParameters.put("Script", "ls");
// After execution of the node operation, a Map with response headers will be returned.
Map<String, String> response = containerClient.executeNodeOperation(applicationInstance, nodeInstance, "ContainerManagementInterface", "runScript", inputParameters);
```

### Access Instance Properties
```java
// The applicationInstance you want to access properties from.
ApplicationInstance applicationInstance;
Plan terminationPlan = applicationInstance.getTerminationPlan();
Application application = applicationInstance.getApplication();
DateTime createdAt = applicationInstance.getCreatedAt();
String id = applicationInstance.getId();
List<PlanInstance> managementPlanInstances = applicationInstance.getManagementPlanInstances();
List<Plan> managementPlans = applicationInstance.getManagementPlans();
List<NodeInstance> nodeInstances = applicationInstance.getNodeInstances();
ServiceTemplateInstanceDTO.StateEnum state = applicationInstance.getState();
```

### Terminate Application Instance
```java
// The applicationInstance you want to terminate.
ApplicationInstance applicationInstance;
boolean success = containerClient.terminateApplicationInstance(applicationInstance);
```

### Remove Application

```java
// The application you want to remove.
Application application;
boolean success = containerClient.removeApplication(application);
```

## Haftungsausschluss

Dies ist ein Forschungsprototyp.
Die Haftung für entgangenen Gewinn, Produktionsausfall, Betriebsunterbrechung, entgangene Nutzungen, Verlust von Daten und Informationen, Finanzierungsaufwendungen sowie sonstige Vermögens- und Folgeschäden ist, außer in Fällen von grober Fahrlässigkeit, Vorsatz und Personenschäden ausgeschlossen.

## Disclaimer of Warranty

Unless required by applicable law or agreed to in writing, Licensor provides the Work (and each Contributor provides its Contributions) on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied, including, without limitation, any warranties or conditions of TITLE, NON-INFRINGEMENT, MERCHANTABILITY, or FITNESS FOR A PARTICULAR PURPOSE.
You are solely responsible for determining the appropriateness of using or redistributing the Work and assume any risks associated with Your exercise of permissions under this License.