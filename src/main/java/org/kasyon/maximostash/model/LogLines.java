package org.kasyon.maximostash.model;

import java.time.LocalDateTime;

public class LogLines 
{
	private String line;
	private String eventType;
	private LocalDateTime eventLDT;
	private String BMXcode;
	private String serverName;
	private String serverIP;
	private String serverTotalMem;
	private String serverAvailMem;

	public String getLine() {
		return line;
	}
	public void setLine(String line) {
		this.line = line;
	}
	public String getEventType() {
		return eventType;
	}
	public void setEventType(String eventType) {
		this.eventType = eventType;
	}
	public LocalDateTime getEventLDT() {
		return eventLDT;
	}
	public void setEventLDT(LocalDateTime eventLDT) {
		this.eventLDT = eventLDT;
	}
	public String getBMXcode() {
		return BMXcode;
	}
	public void setBMXcode(String BMXcode) {
		this.BMXcode = BMXcode;
	}
	public String getServerName() {
		return serverName;
	}
	public void setServerName(String serverName) {
		this.serverName = serverName;
	}
	public String getServerIP() {
		return serverIP;
	}
	public void setServerIP(String serverIP) {
		this.serverIP = serverIP;
	}
	public String getServerTotalMem() {
		return serverTotalMem;
	}
	public void setServerTotalMem(String serverTotalMem) {
		this.serverTotalMem = serverTotalMem;
	}
	public String getServerAvailMem() {
		return serverAvailMem;
	}
	public void setServerAvailMem(String serverAvailMem) {
		this.serverAvailMem = serverAvailMem;
	}
}
