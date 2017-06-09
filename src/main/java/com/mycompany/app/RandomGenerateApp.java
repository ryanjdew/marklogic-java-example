package com.mycompany.app;

import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.marklogic.client.DatabaseClient;
import com.marklogic.client.datamovement.DataMovementManager;
import com.marklogic.client.datamovement.JobTicket;
import com.marklogic.client.datamovement.WriteBatcher;
import com.marklogic.client.document.ServerTransform;
import com.marklogic.client.io.DocumentMetadataHandle;
import com.marklogic.client.io.JacksonDatabindHandle;
import com.mycompany.app.custompojodao.CustomPojoRepositoryImpl;
import com.mycompany.app.custompojodao.DAOFactory;
import com.mycompany.app.data.RandomDocGenerator;
import com.mycompany.app.pojos.Person;

public class RandomGenerateApp {
	private static DAOFactory daoFactory = new DAOFactory();
	private static final Logger logger = Logger.getLogger(RandomGenerateApp.class.getName());

	final static DatabaseClient client = Configuration.mlClient();
	// DataMovementManager is the core class for doing asynchronous jobs against
	// a MarkLogic cluster.
	final static DataMovementManager manager = client.newDataMovementManager();
	private static ServerTransform transform = new ServerTransform("customEnvelope");

	// In this case, we’re writing data in batches
	final static WriteBatcher writer = manager.newWriteBatcher().withTransform(transform).withJobName("Test Job")
			// Configure parallelization and memory tradeoffs
			.withBatchSize(50)
			// Configure listeners for asynchronous life-cycle events
			// Success:
			.onBatchSuccess(batch -> {
				RandomGenerateApp.logger.log(Level.INFO,  batch.getTimestamp().getTime() +
	                       " documents written: " +
	                       batch.getJobWritesSoFar());
			})
			// Failure:
			.onBatchFailure((batch, throwable) -> {
				RandomGenerateApp.logger.log(Level.SEVERE, "Error Writing Batch", throwable);
			});

	final static int numOfDocsToCreate = 1000;
	
	public static void main(String[] args) {
		final JobTicket ticket = manager.startJob(writer);
	    DocumentMetadataHandle metadataHandle = new DocumentMetadataHandle();
	    metadataHandle = metadataHandle.withCollections(Person.class.getName());
	    CustomPojoRepositoryImpl<Person, Serializable> pojoRepo = (CustomPojoRepositoryImpl<Person, Serializable>)daoFactory.getPojoRepository(Person.class);
		logger.log(Level.INFO, "Writing " + numOfDocsToCreate + " Documents");
		for (int i = 0; i < numOfDocsToCreate; i++) {
			try {
				Person personInstance = RandomDocGenerator.newRandomClass(Person.class);
				JacksonDatabindHandle<Person> contentHandle = new JacksonDatabindHandle<>(personInstance);
			    contentHandle.setMapper(pojoRepo.getObjectMapper());
			    writer.addAs(pojoRepo.getDocumentUri(personInstance), metadataHandle, contentHandle);
			} catch (Exception e) {
				RandomGenerateApp.logger.log(Level.SEVERE, "Error adding POJO to Batch Writer", e);
			}

		}
		// Override the default asynchronous behavior and make the current
		// thread wait for all documents to be written to MarkLogic.
		writer.flushAndWait();
		// Finalize the job by its unique handle generated in startJob() above.
		manager.stopJob(ticket);
	}
}
