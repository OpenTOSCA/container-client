package org.opentosca.container.client.data;


import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.annotation.JsonSetter;

// @JsonPropertyOrder({}) // If required, uncomment and add property order for serialization.
@JsonRootName("invocationData")
public class JSONInvocationData {

    private String csarID;
    private String serviceTemplateID;
    private String serviceInstanceID;
    private String nodeTemplateID;
    private String interfaceName;
    private String operationName;

    @JsonGetter
    public String getCsarID() {
        return csarID;
    }

    @JsonSetter
    public void setCsarID(String csarID) {
        this.csarID = csarID;
    }

    @JsonGetter
    public String getServiceTemplateID() {
        return serviceTemplateID;
    }

    @JsonSetter
    public void setServiceTemplateID(String serviceTemplateID) {
        this.serviceTemplateID = serviceTemplateID;
    }

    @JsonGetter
    public String getServiceInstanceID() {
        return serviceInstanceID;
    }

    @JsonSetter
    public void setServiceInstanceID(String serviceInstanceID) {
        this.serviceInstanceID = serviceInstanceID;
    }

    @JsonGetter
    public String getNodeTemplateID() {
        return nodeTemplateID;
    }

    @JsonSetter
    public void setNodeTemplateID(String nodeTemplateID) {
        this.nodeTemplateID = nodeTemplateID;
    }

    @JsonGetter
    public String getInterfaceName() {
        return interfaceName;
    }

    @JsonSetter
    public void setInterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }

    @JsonGetter
    public String getOperationName() {
        return operationName;
    }

    @JsonSetter
    public void setOperationName(String operationName) {
        this.operationName = operationName;
    }
}
