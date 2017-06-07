package com.mycompany.app;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.marklogic.client.DatabaseClient;
import com.marklogic.client.DatabaseClientFactory;
import com.marklogic.client.DatabaseClientFactory.DigestAuthContext;
import com.marklogic.client.io.JacksonHandle;
import com.mycompany.app.pojos.Person;

public class Configuration {
	private static final Logger logger = Logger.getLogger(Configuration.class.getName());
	static {
		try {
			DatabaseClientFactory.getHandleRegistry().register(JacksonHandle.newFactory(), Person.class);
		} catch (Exception e1) {
			logger.log(Level.SEVERE, "Failed to register Person.class handle", e1);
		}
	}
	private static DatabaseClient client = DatabaseClientFactory.newClient("ml-9", 8002, "test-db", new DigestAuthContext("admin", "admin"));

	public static DatabaseClient mlClient() {
		return client;
	}
}