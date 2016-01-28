package com.mycompany.app.custompojodao;


import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.marklogic.client.document.DocumentPage;
import com.marklogic.client.impl.PojoPageImpl;
import com.marklogic.client.io.JacksonDatabindHandle;

public class CustomPojoPageImpl<T> extends PojoPageImpl<T> {
	private DocumentPage docPage;
	private Class<T> entityClass;
	
	public CustomPojoPageImpl(DocumentPage docPage, Class<T> entityClass) {
        super(docPage, entityClass);
        this.docPage = docPage;
        this.entityClass = entityClass;
    }
	

    @Override
    public T next() {
        JacksonDatabindHandle<T> handle = new JacksonDatabindHandle<T>(entityClass);
        handle.getMapper()
        		.enable(SerializationFeature.WRAP_ROOT_VALUE)
				.enable(DeserializationFeature.UNWRAP_ROOT_VALUE);
        return docPage.nextContent(handle).get();
    }
}
