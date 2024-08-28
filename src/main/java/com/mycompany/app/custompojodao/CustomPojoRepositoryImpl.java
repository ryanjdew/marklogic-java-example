package com.mycompany.app.custompojodao;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.TimeZone;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.marklogic.client.DatabaseClient;
import com.marklogic.client.MarkLogicInternalException;
import com.marklogic.client.ResourceNotFoundException;
import com.marklogic.client.Transaction;
import com.marklogic.client.document.DocumentPage;
import com.marklogic.client.document.JSONDocumentManager;
import com.marklogic.client.impl.HandleAccessor;
import com.marklogic.client.impl.PojoRepositoryImpl;
import com.marklogic.client.io.SearchHandle;
import com.marklogic.client.io.marker.SearchReadHandle;
import com.marklogic.client.pojo.PojoPage;
import com.marklogic.client.pojo.PojoQueryBuilder;
import com.marklogic.client.pojo.PojoQueryDefinition;
import com.marklogic.client.pojo.PojoRepository;
import com.marklogic.client.query.QueryManager.QueryView;

public class CustomPojoRepositoryImpl<T, ID extends Serializable>
    implements PojoRepository<T, ID>
{
    private final String EXTENSION = ".json";

    private Class<T> entityClass;
    private PojoRepositoryImpl<T,ID> pojoRepository;
    @SuppressWarnings("unused")
    private Class<ID> idClass;
    private JSONDocumentManager docMgr;
    private PojoQueryBuilder<T> qb;
    @SuppressWarnings("unused")
    private String idPropertyName;
    private static final String ISO_8601_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";
    private static SimpleDateFormat simpleDateFormat8601;
    static {
        try {
            simpleDateFormat8601 = new SimpleDateFormat(ISO_8601_FORMAT);
        // Java 1.6 doesn't yet know about X (ISO 8601 format)
        } catch (IllegalArgumentException e) {
            if ( "Illegal pattern character 'X'".equals(e.getMessage()) ) {
                simpleDateFormat8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            }
        }
    }
    static { simpleDateFormat8601.setTimeZone(TimeZone.getTimeZone("UTC")); }
    private ObjectMapper objectMapper = new ObjectMapper()
    	.enable(SerializationFeature.WRAP_ROOT_VALUE)
    	.enable(DeserializationFeature.UNWRAP_ROOT_VALUE)
        // if we don't do the next two lines Jackson will automatically close our streams which is undesirable
        .configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false)
        .configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, false)
        // we do the next two so dates are written in xs:dateTime format
        // which makes them ready for range indexes in MarkLogic Server
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        .setDateFormat(simpleDateFormat8601);

    CustomPojoRepositoryImpl(DatabaseClient client, Class<T> entityClass, Class<ID> idClass) {
    	pojoRepository = (PojoRepositoryImpl<T,ID>) client.newPojoRepository(entityClass, idClass);
    	pojoRepository.setObjectMapper(objectMapper);
    	docMgr = client.newJSONDocumentManager();
    	this.entityClass = entityClass;
    	this.idClass = idClass;
    }

    @Override
    public void write(T entity) {
    	pojoRepository.write(entity, null, (String[]) null);
    }
    @Override
    public void write(T entity, String... collections) {
    	pojoRepository.write(entity, null, collections);
    }
    @Override
    public void write(T entity, Transaction transaction) {
    	pojoRepository.write(entity, transaction, (String[]) null);
    }
    @Override
    public void write(T entity, Transaction transaction, String... collections) {
    	pojoRepository.write(entity, transaction, collections);
    }

    @Override
    public boolean exists(ID id) {
    	return pojoRepository.exists(id);
    }

    @Override
    public boolean exists(ID id, Transaction transaction) {
    	return pojoRepository.exists(id, transaction);
    }

    @Override
    public long count() {
        return pojoRepository.count();
    }

    @Override
    public long count(String... collections) {
        return pojoRepository.count(collections);
    }

    @Override
    public long count(PojoQueryDefinition query) {
        return pojoRepository.count(query, null);
    }
  
    @Override
    public long count(Transaction transaction) {
        return pojoRepository.count(transaction);
    }

    @Override
    public long count(String[] collections, Transaction transaction) {
        return pojoRepository.count(collections, transaction);
    }

    @Override
    public long count(PojoQueryDefinition query, Transaction transaction) {
        return pojoRepository.count(query, transaction);
    }

    @SuppressWarnings("unchecked")
	@Override
    public void delete(ID... ids) {
    	pojoRepository.delete(ids, null);
    }

    @Override
    public void delete(ID[] ids, Transaction transaction) {
    	pojoRepository.delete(ids, transaction);
    }

    @Override
    public void deleteAll() {
    	pojoRepository.deleteAll(null);
    }

    @Override
    public void deleteAll(Transaction transaction) {
    	pojoRepository.deleteAll(transaction);
    }
  
    @Override
    public T read(ID id) {
        return read(id, null);
    }
    @Override
    public T read(ID id, Transaction transaction) {
        ArrayList<ID> ids = new ArrayList<ID>();
        ids.add(id);
        @SuppressWarnings("unchecked")
        PojoPage<T> page = read(ids.toArray((ID[])new Serializable[0]), transaction);
        if ( page == null || page.hasNext() == false ) {
            throw new ResourceNotFoundException("Could not find document of type " +
                entityClass.getName() + " with id " + id);
        }
        return page.next();
    }
    @Override
    public PojoPage<T> read(ID[] ids) {
        return read(ids, null);
    }
    @Override
    public PojoPage<T> read(ID[] ids, Transaction transaction) {
        ArrayList<String> uris = new ArrayList<String>();
        for ( ID id : ids ) {
            uris.add(createUri(id));
        }
        DocumentPage docPage = (DocumentPage) docMgr.read(transaction, uris.toArray(new String[0]));
        PojoPage<T> pojoPage = new CustomPojoPageImpl<T>(docPage, entityClass);
        return pojoPage;
    }
    @Override
    public PojoPage<T> readAll(long start) {
        return search(null, start, null, null);
    }
    @Override
    public PojoPage<T> readAll(long start, Transaction transaction) {
        return search(null, start, null, transaction);
    }

    @Override
    public PojoPage<T> search(long start, String... collections) {
        return search(qb.collection(collections), start, null, null);
    }
    @Override
    public PojoPage<T> search(long start, Transaction transaction, String... collections) {
        return search(qb.collection(collections), start, null, transaction);
    }

    @Override
    public PojoPage<T> search(PojoQueryDefinition query, long start) {
        return search(query, start, null, null);
    }
    @Override
    public PojoPage<T> search(PojoQueryDefinition query, long start, Transaction transaction) {
        return search(query, start, null, transaction);
    }
    @Override
    public PojoPage<T> search(PojoQueryDefinition query, long start, SearchReadHandle searchHandle) {
        return search(query, start, searchHandle, null);
    }
    @Override
    public PojoPage<T> search(PojoQueryDefinition query, long start, SearchReadHandle searchHandle, Transaction transaction) {
    	if ( searchHandle != null ) {
            HandleAccessor.checkHandle(searchHandle, "search");
            if (searchHandle instanceof SearchHandle) {
                SearchHandle responseHandle = (SearchHandle) searchHandle;
                responseHandle.setQueryCriteria(query);
            }
        }

        DocumentPage docPage = docMgr.search(wrapQuery(query), start, searchHandle, transaction);
        PojoPage<T> pojoPage = new CustomPojoPageImpl<T>(docPage, entityClass);
        return pojoPage;
    }
 
    @Override
    public PojoQueryBuilder<T> getQueryBuilder() {
        return pojoRepository.getQueryBuilder();
    }

    @Override
    public long getPageLength() {
        return pojoRepository.getPageLength();
    }
    @Override
    public void setPageLength(long length) {
    	pojoRepository.setPageLength(length);
        docMgr.setPageLength(length);
    }
    
    public QueryView getSearchView() {
        return pojoRepository.getSearchView();
    }

    public void setSearchView(QueryView view) {
    	pojoRepository.setSearchView(view);
        docMgr.setSearchView(view);
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    public void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        pojoRepository.setObjectMapper(objectMapper);
    }
    
    private PojoQueryDefinition wrapQuery(PojoQueryDefinition query) {
        if ( query == null ) {
            return qb.collection(entityClass.getName());
        } else {
            List<String> collections = Arrays.asList(query.getCollections());
            HashSet<String> collectionSet = new HashSet<String>(collections);
            collectionSet.add(entityClass.getName());
            query.setCollections(collectionSet.toArray(new String[0]));
            return query;
        }
    }
    
    @Override
    public String getDocumentUri(T entity) {
        return this.createUri(pojoRepository.getId(entity));
    }

    protected String createUri(ID id) {
        if ( id == null ) {
            throw new IllegalStateException("id cannot be null");
        }
        try {
            return entityClass.getName() + "/" + URLEncoder.encode(id.toString(), "UTF-8") + EXTENSION;
        } catch (UnsupportedEncodingException e) {
            throw new MarkLogicInternalException(e);
        }
    }

	@Override
	public ID getId(T entity) {
		return pojoRepository.getId(entity);
	}
}