package org.opentosca.container.client.impl;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InvocationRequest {

    @JsonProperty("invocation-information")
    private InvocationData data;

    @JsonProperty("params")
    private Map<String, String> parameters;

    @Data
    @Builder
    public static class InvocationData {

        @JsonProperty("csarID")
        private String csarId;

        @JsonProperty("serviceTemplateID")
        private String serviceTemplateId;

        @JsonProperty("serviceInstanceID")
        private String serviceInstanceId;

        @JsonProperty("nodeTemplateID")
        private String nodeTemplateId;

        @JsonProperty("interface")
        private String interfaceName;

        @JsonProperty("operation")
        private String operationName;
    }
}
