package com.mycompany.app.pojos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.marklogic.client.pojo.annotation.Id;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonRootName("envelope")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Envelope<T extends EnvelopeContent> {
	private EnvelopeMeta meta;
	private T content;

	@Id
	public String get_id() {
		return content.get_id();
	}
	
	public void set_id(String _id) {
		content.set_id(_id);
	}
	
	@JsonProperty("meta")
	public EnvelopeMeta getMeta() {
		return meta;
	}

	public void setMeta(EnvelopeMeta meta) {
		this.meta = meta;
	}

	@JsonProperty("content")
	public T getContent() {
		return content;
	}

	public void setContent(T content) {
		this.content = content;
	}
	
}
