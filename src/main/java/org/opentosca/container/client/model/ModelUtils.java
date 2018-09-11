package org.opentosca.container.client.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opentosca.api.rest.client.model.CsarDTO;
import org.opentosca.api.rest.client.model.InterfaceDTO;
import org.opentosca.api.rest.client.model.OperationDTO;
import org.opentosca.api.rest.client.model.ServiceTemplateInstanceDTO;
import org.opentosca.api.rest.client.model.TParameter;

public class ModelUtils {

	public static Application transform(final CsarDTO csar, final List<TParameter> inputParameters,
			final List<ServiceTemplateInstanceDTO> serviceInstanceIds, final List<InterfaceDTO> interfaces,
			final String metadata) {
		return new Application(csar.getId(), transform(inputParameters), transform2IdList(serviceInstanceIds),
				csar.getDisplayName(), csar.getVersion(), csar.getDescription(), concat(csar.getAuthors()),
				transform2Interfaces(interfaces), metadata);
	}

	public static List<Interface> transform2Interfaces(final List<InterfaceDTO> ifaces) {
		List<Interface> ifaces2 = new ArrayList<Interface>();
		for (InterfaceDTO iface : ifaces) {
			ifaces2.add(transform(iface));
		}
		return ifaces2;
	}

	public static Interface transform(final InterfaceDTO iface) {
		Map<String, List<String>> inputOperationMappings = new HashMap<String, List<String>>();
		Map<String, List<String>> outputOperationMappings = new HashMap<String, List<String>>();
		String name = iface.getName();
		for (String operationName : iface.getOperations().keySet()) {
			OperationDTO opDto = iface.getOperations().get(operationName);
			inputOperationMappings.putAll(transform(opDto, true));
			outputOperationMappings.putAll(transform(opDto, false));
		}
		return new Interface(name, inputOperationMappings, outputOperationMappings);
	}

	public static List<String> transform2IdList(final List<ServiceTemplateInstanceDTO> servInstances) {
		List<String> ids = new ArrayList<String>();
		for (ServiceTemplateInstanceDTO servInst : servInstances) {
			ids.add(transform(servInst));
		}
		return ids;
	}

	public static String transform(final ServiceTemplateInstanceDTO servInstance) {
		return String.valueOf(servInstance.getId());
	}

	public static String transform(final TParameter param) {
		return param.getName();
	}

	public static List<String> transform(final List<TParameter> params) {
		List<String> paramStrings = new ArrayList<String>();
		for (TParameter param : params) {
			paramStrings.add(transform(param));
		}
		return paramStrings;
	}

	public static Map<String, List<String>> transform(final OperationDTO op, final boolean forInput) {
		Map<String, List<String>> operationMapping = new HashMap<String, List<String>>();

		if (op.getPlan() != null) {
			String name = op.getName();
			if (forInput) {
				List<String> inputParams = transform(op.getPlan().getInputParameters());
				operationMapping.put(name, inputParams);
			} else {
				List<String> outputParams = transform(op.getPlan().getOutputParameters());
				operationMapping.put(name, outputParams);
			}
		}
		// TODO implement checking Node- and Relationship Operations
		return operationMapping;
	}

	private static String concat(Collection<String> strings) {
		StringBuilder strB = new StringBuilder();
		for (String string : strings) {
			strB.append(string);
		}
		return strB.toString();
	}
}
