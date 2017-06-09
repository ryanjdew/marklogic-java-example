package com.mycompany.app.pojos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.marklogic.client.pojo.annotation.Id;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes({
    @JsonSubTypes.Type(value = Person.class, name = "person") }
)
public abstract class EnvelopeContent {
	protected String _id;
	protected String contentType;

	@JsonProperty("_id")
	@Id
	public String get_id() {
		return _id;
	}
	public void set_id(String _id) {
		this._id = _id;
	}
		
	@JsonProperty("contentType")
	public String getContentType() {
		return contentType;
	}
	public void setContentType(String contentType) {
		this.contentType = contentType;
	}
}
