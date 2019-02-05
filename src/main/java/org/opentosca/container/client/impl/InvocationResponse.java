package org.opentosca.container.client.impl;

import java.util.Map;

import lombok.Data;

@Data
public class InvocationResponse {

    private String status;

    private Map<String, String> response;
}
