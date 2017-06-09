package com.mycompany.app;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.marklogic.client.DatabaseClient;
import com.marklogic.client.datamovement.DataMovementManager;
import com.marklogic.client.datamovement.ExportListener;
import com.marklogic.client.datamovement.QueryBatcher;
import com.marklogic.client.io.JacksonDatabindHandle;
import com.marklogic.client.query.StructuredQueryBuilder;
import com.marklogic.client.query.StructuredQueryDefinition;
import com.mycompany.app.pojos.Envelope;
import com.mycompany.app.pojos.Person;

public class ReadOutPojos {
	private static final Logger logger = Logger.getLogger(ReadOutPojos.class.getName());
	final static DatabaseClient client = Configuration.mlClient();
	// DataMovementManager is the core class for doing asynchronous jobs against
	// a MarkLogic cluster.
	final static DataMovementManager manager = client.newDataMovementManager();

	public static void main(String[] args) {
		final StructuredQueryBuilder sqb = new StructuredQueryBuilder();
		final StructuredQueryDefinition query = sqb.and(
		  sqb.collection(Person.class.getName())
		);
		@SuppressWarnings("unchecked")
		final QueryBatcher batcher = manager
		  .newQueryBatcher(query)
		  .withBatchSize(100)
		  // Run the query at a consistent point in time.
		  // This means that the matched documents will be the same 
		  // across batches, even if the underlying data is changing.
		  .withConsistentSnapshot()
		  // Included QueryBatchListener implementation that deletes
		  // a batch of URIs. 
		  .onUrisReady(new ExportListener()
			        .onDocumentReady(doc-> {
			        	Envelope<Person> personInstance = new Envelope<Person>();
			        	JacksonDatabindHandle<Envelope<Person>> handle = new JacksonDatabindHandle<Envelope<Person>>((Class<Envelope<Person>>) personInstance.getClass());
			            handle.getMapper()
			            		.enable(SerializationFeature.WRAP_ROOT_VALUE)
			    				.enable(DeserializationFeature.UNWRAP_ROOT_VALUE);
			            doc.getContent(handle);
						try {
							personInstance = handle.get();
						} catch (Exception e) {
							logger.log(Level.SEVERE, "Error parsing returned JSON", e);
						}
			        	logger.log(Level.INFO, "Found Person: " + personInstance.getContent().getSurname() + ", " + personInstance.getContent().getGivenName() + 
			        			" ingested by " + personInstance.getMeta().getIngestUser() + " at " + personInstance.getMeta().getIngestDateTime());
			        }))
		  .onQueryFailure(throwable -> logger.log(Level.SEVERE, "Error on query", throwable));
		manager.startJob(batcher);
		batcher.awaitCompletion();
	}

}
