package org.opentosca.container.client.impl;

import lombok.Data;

import java.util.Map;

@Data
public class InvocationResponse {

    private String status;

    private Map<String, String> response;
}
