
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![](https://jitpack.io/v/OpenTOSCA/container-client.svg)](https://jitpack.io/#OpenTOSCA/container-client)

# OpenTOSCA Container Client

Part of the [OpenTOSCA Ecosystem](http://www.opentosca.org)

## User Guide

### Test Environment Connection
```java
public class ContainerClientExamples {  
      public void testClientConnection() {
          String dockerHost = "localhost";
          Integer port = 1337;
          ContainerClient containerClient = new ContainerClientBuilder()
                  .withHostname(dockerHost)
                  .withPort(port)
                  .build();
          containerClient.getApplications();
      }
  }
```

### Upload an Application
```java
public class ContainerClientExamples {
        public void uploadApplication() {
            String dockerHost = "localhost";
            Integer port = 1337;
            ContainerClient containerClient = new ContainerClientBuilder()
                    .withHostname(dockerHost)
                    .withPort(port)
                    .build();
            Path applicationPath = Paths.get("MyTinyToDo_Bare_Docker.csar");
            containerClient.uploadApplication(applicationPath);
        }
    }
```

### Provision an Application

```java
public class ContainerClientExamples {
        public void provisionApplication() {
            String dockerHost = "localhost";
            Integer port = 1337;
            ContainerClient containerClient = new ContainerClientBuilder()
                    .withHostname(dockerHost)
                    .withPort(port)
                    .build();
            Application application = containerClient.getApplications().get(0);
            Map<String, String> inputParameters = new HashMap<>();
            inputParameters.put("DockerEngineURL", "tcp://dind:2375");
            inputParameters.put("ApplicationPort", "9990");
            containerClient.provisionApplication(application, inputParameters);
        }
    }
```

### Retrieve Application Instances

```java
public class ContainerClientExamples {
        public void retrieveApplicationInstances() {
            String dockerHost = "localhost";
            Integer port = 1337;
            ContainerClient containerClient = new ContainerClientBuilder()
                    .withHostname(dockerHost)
                    .withPort(port)
                    .build();
            Application application = containerClient.getApplications().get(0);
            List<ApplicationInstance> applicationInstances = containerClient.getApplicationInstances(application);
            // Optionally you may retrieve applications with a certain state:
            containerClient.getApplicationInstances(application, ServiceTemplateInstanceDTO.StateEnum.CREATED);
        }
    }
```

### Execute Node Operation

```java
public class ContainerClientExamples {
        public void executeNodeOperation() {
            String dockerHost = "localhost";
            Integer port = 1337;
            ContainerClient containerClient = new ContainerClientBuilder()
                    .withHostname(dockerHost)
                    .withPort(port)
                    .build();
            Application application = containerClient.getApplications().get(0);
            List<ApplicationInstance> applicationInstances = containerClient.getApplicationInstances(application, ServiceTemplateInstanceDTO.StateEnum.CREATED);
            ApplicationInstance applicationInstance = applicationInstances.get(0);
            List<NodeInstance> nodeInstances = applicationInstance.getNodeInstances();
            List<NodeInstance> myTinyToDoDockerNodeInstances = nodeInstances.stream().filter(x -> (x.getTemplate().equals("MyTinyToDoDockerContainer"))).collect(Collectors.toList());
            NodeInstance nodeInstance = myTinyToDoDockerNodeInstances.get(0);
            Map<String, String> inputParameters = new HashMap<>();
            inputParameters.put("Script", "ls");
            containerClient.executeNodeOperation(applicationInstance, nodeInstance, "ContainerManagementInterface", "runScript", inputParameters);
        }
    }
```

### Terminate Application Instance
```java
public class ContainerClientExamples {
        public void terminateApplicationInstance() {
            String dockerHost = "localhost";
            Integer port = 1337;
            ContainerClient containerClient = new ContainerClientBuilder()
                    .withHostname(dockerHost)
                    .withPort(port)
                    .build();
            List<Application> applications = containerClient.getApplications();
            Application application = applications.get(0);
            List<ApplicationInstance> applicationInstances = containerClient.getApplicationInstances(application, ServiceTemplateInstanceDTO.StateEnum.CREATED);
            ApplicationInstance applicationInstance = applicationInstances.get(0);
            containerClient.terminateApplicationInstance(applicationInstance);
        }
    }
```

### Remove Application

```java
public class ContainerClientExamples {
        public void removeApplication() {
            String dockerHost = "localhost";
            Integer port = 1337;
            ContainerClient containerClient = new ContainerClientBuilder()
                    .withHostname(dockerHost)
                    .withPort(port)
                    .build();
            List<Application> applications = containerClient.getApplications();
            Application application = applications.get(0);
            containerClient.removeApplication(application);
        }
    }
```

## Haftungsausschluss

Dies ist ein Forschungsprototyp.
Die Haftung für entgangenen Gewinn, Produktionsausfall, Betriebsunterbrechung, entgangene Nutzungen, Verlust von Daten und Informationen, Finanzierungsaufwendungen sowie sonstige Vermögens- und Folgeschäden ist, außer in Fällen von grober Fahrlässigkeit, Vorsatz und Personenschäden ausgeschlossen.

## Disclaimer of Warranty

Unless required by applicable law or agreed to in writing, Licensor provides the Work (and each Contributor provides its Contributions) on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied, including, without limitation, any warranties or conditions of TITLE, NON-INFRINGEMENT, MERCHANTABILITY, or FITNESS FOR A PARTICULAR PURPOSE.
You are solely responsible for determining the appropriateness of using or redistributing the Work and assume any risks associated with Your exercise of permissions under this License.
