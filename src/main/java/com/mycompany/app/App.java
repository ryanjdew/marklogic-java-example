package com.mycompany.app;

import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marklogic.client.DatabaseClient;
import com.marklogic.client.datamovement.DataMovementManager;
import com.marklogic.client.datamovement.ExportListener;
import com.marklogic.client.datamovement.JobTicket;
import com.marklogic.client.datamovement.QueryBatcher;
import com.marklogic.client.datamovement.WriteBatcher;
import com.marklogic.client.impl.PojoRepositoryImpl;
import com.marklogic.client.io.DocumentMetadataHandle;
import com.marklogic.client.io.JacksonDatabindHandle;
import com.marklogic.client.query.StructuredQueryBuilder;
import com.marklogic.client.query.StructuredQueryDefinition;
import com.mycompany.app.custompojodao.DAOFactory;
import com.mycompany.app.data.RandomDocGenerator;
import com.mycompany.app.pojos.Person;

public class App {
	private static DAOFactory daoFactory = new DAOFactory();
	private static final Logger logger = Logger.getLogger(App.class.getName());
	// DataMovementManager is the core class for doing asynchronous jobs against
	// a MarkLogic cluster.
	final static DatabaseClient client = Configuration.mlClient();
	final static DataMovementManager manager = client.newDataMovementManager();

	// In this case, we’re writing data in batches
	final static WriteBatcher writer = manager.newWriteBatcher().withJobName("Test Job")
			// Configure parallelization and memory tradeoffs
			.withBatchSize(50)
			// Configure listeners for asynchronous life-cycle events
			// Success:
			.onBatchSuccess(batch -> {
				App.logger.log(Level.INFO,  batch.getTimestamp().getTime() +
	                       " documents written: " +
	                       batch.getJobWritesSoFar());
			})
			// Failure:
			.onBatchFailure((batch, throwable) -> {
				App.logger.log(Level.SEVERE, "Error Writing Batch", throwable);
			});

	final static int numOfDocsToCreate = 10000;
	
	public static void main(String[] args) {
		final JobTicket ticket = manager.startJob(writer);
	    DocumentMetadataHandle metadataHandle = new DocumentMetadataHandle();
	    metadataHandle = metadataHandle.withCollections(Person.class.getName());
		PojoRepositoryImpl<Person, Serializable> pojoRepo = (PojoRepositoryImpl<Person, Serializable>)daoFactory.getPojoRepository(Person.class);
		logger.log(Level.INFO, "Writing " + numOfDocsToCreate + " Documents");
		for (int i = 0; i < numOfDocsToCreate; i++) {
			try {
				Person personInstance = RandomDocGenerator.newRandomClass(Person.class);
				JacksonDatabindHandle<Person> contentHandle = new JacksonDatabindHandle<>(personInstance);
			    contentHandle.setMapper(pojoRepo.getObjectMapper());
			    writer.addAs(pojoRepo.getDocumentUri(personInstance), metadataHandle, contentHandle);
			} catch (Exception e) {
				App.logger.log(Level.SEVERE, "Error adding POJO to Batch Writer", e);
			}

		}
		// Override the default asynchronous behavior and make the current
		// thread wait for all documents to be written to MarkLogic.
		writer.flushAndWait();
		// Finalize the job by its unique handle generated in startJob() above.
		manager.stopJob(ticket);
		
		final StructuredQueryBuilder sqb = new StructuredQueryBuilder();
		final StructuredQueryDefinition query = sqb.and(
		  sqb.collection(Person.class.getName())
		);
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
			        	 JacksonDatabindHandle<Person> handle = new JacksonDatabindHandle<>(Person.class);
			        	 handle.getMapper().enableDefaultTyping(
			        	      ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.WRAPPER_OBJECT);
			        	doc.getContent(handle);
			        	Person personInstance = handle.get();
			        	logger.log(Level.INFO, "Found Person: " + personInstance.getSurname() + ", " + personInstance.getGivenName());
			        }))
		  .onQueryFailure(throwable -> throwable.printStackTrace());
		final JobTicket readTicket = manager.startJob(batcher);
		batcher.awaitCompletion();
		manager.stopJob(readTicket);
	}
}
