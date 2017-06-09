# Marklogic Java Example
Example of writing a POJO with the MarkLogic Java Client API's Data Movement SDK. 

## Setup
### com.mycompany.app.Configuration
This class creates the MarkLogic Client with connection details. This needs to be adjusted with connection details for your MarkLogic cluster.

## Running main classes
### com.mycompany.app.InsertTransform
This installs the customEnvelope MarkLogic transform (resources/customEnvelope.sjs) into the MarkLogic REST server.

### com.mycompany.app.RandomGenerateApp
Generates Person POJOs with randomly selected data and then inserts them asynchronously in MarkLogic via the Data Movement SDK.

### com.mycompany.app.ReadOutPojos
Reads out the generated person documents into the Envelope\<Person\> POJO, representing the envelope pattern. This is done asynchronously via the Data Movement SDK.


