package org.opentosca.container.client.data;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonRootName;

import java.util.Map;

// @JsonPropertyOrder({}) // If required, uncomment and add property order for serialization.
@JsonRootName("parameterData")
public class JSONParameterData {

    private Map<String, String> params;

    @JsonAnyGetter
    public Map<String, String> getParams() {
        return params;
    }

    @JsonAnySetter
    public void setParams(Map<String, String> params) {
        this.params = params;
    }
}
