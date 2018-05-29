package org.opentosca.containerapi.client.model;

public class Log {

	private long timestamp;
	private String status;
	private String type;
	private String message;

	public Log(Long timestamp, String status, String type, String message) {
		this.timestamp = timestamp;
		this.status = status;
		this.type = type;
		this.message = message;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public String getStatus() {
		return status;
	}

	public String getType() {
		return type;
	}

	public String getMessage() {
		return message;
	}
}
