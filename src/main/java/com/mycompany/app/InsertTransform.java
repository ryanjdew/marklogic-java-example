package com.mycompany.app;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.marklogic.client.DatabaseClient;
import com.marklogic.client.admin.ServerConfigurationManager;
import com.marklogic.client.admin.TransformExtensionsManager;
import com.marklogic.client.io.InputStreamHandle;

public class InsertTransform {
	private static final Logger logger = Logger.getLogger(InsertTransform.class.getName());
	final static DatabaseClient client = Configuration.mlClient();
	
	public static void main(String[] args) {
			ServerConfigurationManager configManager = client.newServerConfigManager();
			TransformExtensionsManager transformExtManager = configManager.newTransformExtensionsManager();
			FileInputStream transStream = null;
			ClassLoader classLoader = InsertTransform.class.getClassLoader();
			try {
			    transStream = new FileInputStream(classLoader.getResource("customEnvelope.sjs").getFile());
			} catch (FileNotFoundException e) {
			    logger.log(Level.SEVERE, "Error reading extension", e);
			}
			InputStreamHandle ipStreamHandle = new InputStreamHandle(transStream);
			transformExtManager.writeJavascriptTransform("customEnvelope", ipStreamHandle);
			logger.log(Level.INFO, "Transform Inserted");
	}
}
