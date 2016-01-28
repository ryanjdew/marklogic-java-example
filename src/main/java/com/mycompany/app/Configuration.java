package com.mycompany.app;

import com.marklogic.client.DatabaseClient;
import com.marklogic.client.DatabaseClientFactory;

public class Configuration {
	private static DatabaseClient client = DatabaseClientFactory.newClient("localhost", 9100, "test-db", "admin",
			"admin", DatabaseClientFactory.Authentication.DIGEST);

	public static DatabaseClient mlClient() {
		return client;
	}
}