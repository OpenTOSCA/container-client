package org.opentosca.container.client.data;


import com.fasterxml.jackson.annotation.*;

import java.util.Map;

// @JsonPropertyOrder({}) // If required, uncomment and add property order for serialization.
@JsonRootName("requestData")
public class JSONRequestData {


    @JsonProperty("invocation-information")
    private JSONInvocationData invocationInformation;
    private Map<String, String> params;

    @JsonGetter
    public JSONInvocationData getInvocationInformation() {
        return invocationInformation;
    }

    @JsonSetter
    public void setInvocationInformation(JSONInvocationData invocationInformation) {
        this.invocationInformation = invocationInformation;
    }

    @JsonAnyGetter
    public Map<String, String> getParams() {
        return params;
    }

    @JsonAnySetter
    public void setParams(Map<String, String> params) {
        this.params = params;
    }
}
