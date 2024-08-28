package com.mycompany.app;

import com.marklogic.client.DatabaseClient;
import com.marklogic.client.DatabaseClientFactory;
import com.marklogic.client.DatabaseClientFactory.DigestAuthContext;

public class Configuration {
	private static DatabaseClient client = DatabaseClientFactory.newClient("localhost", 8000, "Documents", new DigestAuthContext("admin", "admin"));

	public static DatabaseClient mlClient() {
		return client;
	}
}
