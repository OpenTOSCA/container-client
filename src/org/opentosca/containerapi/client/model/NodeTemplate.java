package org.opentosca.containerapi.client.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.json.JSONArray;
import org.json.JSONObject;

public class NodeTemplate {

	private String id;
	private QName serviceTemplateId;
	private QName type;
	private List<Interface> interfaces;

	public NodeTemplate(final String id, final QName serviceTemplateId, final QName type,
			final List<Interface> interfaces) {
		this.id = id;
		this.serviceTemplateId = serviceTemplateId;
		this.type = type;
		this.interfaces = interfaces;
	}

	public String getId() {
		return id;
	}

	public QName getServiceTemplateId() {
		return serviceTemplateId;
	}

	public QName getType() {
		return type;
	}

	public List<Interface> getInterfaces() {
		return this.interfaces;
	}

	public static class Transformer {
		public static NodeTemplate transform(QName serviceTemplateId, JSONObject nodeTemplateJson) {
			String id = nodeTemplateJson.getString("id");
			nodeTemplateJson.getString("name");
			QName nodeTypeId = QName.valueOf(nodeTemplateJson.getString("node_type"));

			JSONArray interfacesArray = nodeTemplateJson.getJSONObject("interfaces").getJSONArray("interfaces");

			List<Interface> interfaces = new ArrayList<Interface>();

			for (int index = 0; index < interfacesArray.length(); index++) {
				JSONObject interfaceJson = interfacesArray.getJSONObject(index);
				String interfaceName = interfaceJson.getString("name");
				JSONObject operationsJson = interfaceJson.getJSONObject("operations");
				Map<String,List<String>> inputMap = new HashMap<String,List<String>>();
				Map<String,List<String>> outputMap = new HashMap<String,List<String>>();
				
				for (String operationName : operationsJson.keySet()) {
					JSONObject operationJson = operationsJson.getJSONObject(operationName).getJSONObject("_embedded")
							.getJSONObject("node_operation");
					JSONArray inputParametersJson = operationJson.getJSONArray("input_parameters");
					List<String> inputParams = new ArrayList<String>();
					JSONArray outputParametersJson = operationJson.getJSONArray("output_parameters");
					List<String> outputParams = new ArrayList<String>();
					for (int index2 = 0; index2 < inputParametersJson.length(); index2++) {
						JSONObject parameterJson = inputParametersJson.getJSONObject(index2);
						inputParams.add(parameterJson.getString("name"));
					}
					for (int index2 = 0; index2 < outputParametersJson.length(); index2++) {
						JSONObject parameterJson = outputParametersJson.getJSONObject(index2);
						outputParams.add(parameterJson.getString("name"));
					}
					
					inputMap.put(operationName, inputParams);
					outputMap.put(operationName, outputParams);
					
				}
				
				Interface iface = new Interface(interfaceName, inputMap, outputMap);
				interfaces.add(iface);
			}

			NodeTemplate nodeTemplate = new NodeTemplate(id, serviceTemplateId, nodeTypeId, interfaces);
			return nodeTemplate;
		}
	}

}
