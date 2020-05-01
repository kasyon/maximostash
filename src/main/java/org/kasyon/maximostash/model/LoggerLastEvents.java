package org.kasyon.maximostash.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class LoggerLastEvents implements Serializable 
{
	private static final long serialVersionUID = -4033000343348916670L;
	private String loggername;
	private LocalDateTime lastEventLDT;
	
	public String getLoggername() {
		return loggername;
	}
	public void setLoggername(String loggername) {
		this.loggername = loggername;
	}
	public LocalDateTime getLastEventLDT() {
		return lastEventLDT;
	}
	public void setLastEventLDT(LocalDateTime lastEventLDT) {
		this.lastEventLDT = lastEventLDT;
	}
}
