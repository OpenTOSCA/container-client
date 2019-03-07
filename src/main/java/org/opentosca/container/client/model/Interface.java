package org.opentosca.container.client.model;

import java.util.List;
import java.util.stream.Collectors;

import io.swagger.client.model.InterfaceDTO;
import io.swagger.client.model.PlanDTO;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class Interface {

	private final InterfaceDTO iface;
	
	public String getName() {
		return this.iface.getName();
	}
	
	public List<Operation> getOperations() {
		return this.iface.getOperations().values().stream().map(x -> new Operation(x)).collect(Collectors.toList());
	}
	
}
