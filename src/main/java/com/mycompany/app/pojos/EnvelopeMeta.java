package com.mycompany.app.pojos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonRootName("envelope")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EnvelopeMeta {
	private String ingestUser;
	private String ingestDateTime;
	
	@JsonProperty("ingestUser")
	public String getIngestUser() {
		return ingestUser;
	}
	public void setIngestUser(String ingestUser) {
		this.ingestUser = ingestUser;
	}
	@JsonProperty("ingestDateTime")
	public String getIngestDateTime() {
		return ingestDateTime;
	}
	public void setIngestDateTime(String ingestDateTime) {
		this.ingestDateTime = ingestDateTime;
	}
}
