package org.kasyon.maximostash.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class logfileMetadata implements Serializable
{
	private static final long serialVersionUID = 2786349043634947181L;
	private String loggername;
	private String filename;
	private String path;
	private LocalDateTime created;
	private Long sizeScanned;
	private Long sizePerformed;
	private LocalDateTime lastEventLDT;

	public String getLoggername() {
		return loggername;
	}
	public void setLoggername(String loggername) {
		this.loggername = loggername;
	}
	public String getFilename() {
		return filename;
	}
	public void setFilename(String filename) {
		this.filename = filename;
	}
	public String getPath() {
		return path;
	}
	public void setPath(String path) {
		this.path = path;
	}
	public LocalDateTime getCreated() {
		return created;
	}
	public void setCreated(LocalDateTime created) {
		this.created = created;
	}
	public Long getSizeScanned() {
		return sizeScanned;
	}
	public void setSizeScanned(Long size) {
		this.sizeScanned = size;
	}
	public Long getSizePerformed() {
		return sizePerformed;
	}
	public void setSizePerformed(Long size) {
		this.sizePerformed = size;
	}
	public LocalDateTime getLastEventLDT() {
		return lastEventLDT;
	}
	public void setLastEventLDT(LocalDateTime lastEventLDT) {
		this.lastEventLDT = lastEventLDT;
	}
}
