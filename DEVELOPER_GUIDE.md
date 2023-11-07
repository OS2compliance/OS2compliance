# OS2compliance
## Developer guide

This document is intended for developers, who are going to work on OS2compliance. 
For details on what OS2compliance is, take a look in the [readme](README.md).  

### Prerequisites
* Java JDK 17+
* Docker & docker-compose
* A SAML IDP (eg. AD-FS, OS2faktor, keycloak etc.)

As a developer, you will most likely be using an IDE like Eclipse or IntelliJ.  
The OS2compliance uses **MapStruct** and **Project Lombok** for generating a range of boilerplate code, 
both Lombok and MapStruct has made plugins available for most common Java IDEs, so the generated code is made available for auto-completion in the IDEs.

### Optional: Setup hosts alias
It is recommended to configure an alias in /hosts/env (linux & mac) or C:\Windows\System32\drivers\etc\hosts (windows)   
Eg.  
```127.0.0.1 os2compliance```
this will ensure you can access you local instance at https://os2compliance:8343 and cookies will be on that name.

### Recommended: Configuring an SAML IDP
If you need to access the UI a SAML IDP is needed to login.  
In the doc/ folder there is two guides to configuring either AD-FS or OS2faktor, take a look at those.  
Note that since you are running locally the IDP will not be able to resolve the metadata at the address in those guides, instead save the metadata.xml to a local file and upload it when creating the relaying party / service provider.  
To download the metadata, start everything in docker-compose like described below and download the metadata at ```https://os2compliance:8343/saml/metadata```  

### Start everything using docker-compose
In the project root a docker-compose.yml file is included, running this will start both MySQL and OS2compliance.  
First create a .env file with the following settings
```
SAML_METADATA_LOCATION=url:URL-TO-METADATA-FROM-SAML-IDP 
SAML_ENTITY_ID=EntitityId
```

To start run the following commands:  
```
docker-compose build    
docker-compose up
```

### Seeding users
Now you have both SAML IDP and OS2compliance running, but unless you have configured OS2sync, you have no users in OS2compliance.    
Open the MySQL database with your favorite tool, and add a row in the users table:
```
INSERT INTO os2compliance.users (uuid, active, name, email, user_id) 
VALUES ('matching-uuid', true, 'An user', 'some@email.com', 'user');
```
The UUID id is the one sent from the IDP, and needs to match the OS2compliance user's uuid.  

### Debugging
Start the database from the docker-compose like so:    
```    
docker-compose up -d db
```
Now you can run the OS2compliance project from either from your editor or using maven.    
Using maven is simple just run the following command
```
./mvnw package
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```
To enable remote debug use the following command instead:    
```
./mvnw spring-boot:run  -Dspring-boot.run.profiles=dev "-Dspring-boot.run.jvmArguments=-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005"
```
This will start the OS2compliance in suspended mode and when an remote debugger is connected on port 5005 it will continue.

### Integrations
#### CVR - Datafordeler middleware
The CVR integration will lookup common information on suppliers, when they are created and periodically if entities are marked for update.  

#### Kitos
Kitos is used to fetch it-systems and supplier and create create them automatically in OS2compliance.  
At time of writing further documentation can be found on the Kitos wiki here https://os2web.atlassian.net/wiki/spaces/KITOS/pages/657391621/Teknisk+dokumentation  

#### OS2sync
Is used to fetch users and organisations, in time hopefully KLE as well.
Documentation for OS2sync kan be found here https://www.os2.eu/os2sync

#### Mail
Simple SMTP client for e-mail integration  

### List of configuration properties
Below is a list of all the properties that can be modified through environments variables.

| Variable          | Default value                                                       | Description                                                                                                                |
| --- |---------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------|
|SSL_ENABLED| true                                                                | SSL enabled                                                                                                                |
|SSL_KEYSTORE_LOCATION| security/ssl-demo.pfx                                               | path to ssl certificate                                                                                                    |
|SSL_KEYSTORE_PASSWORD| Test1234                                                        | SSL keystore password                                                                                                      |
|SSL_KEY_PASSWORD| Password1234                                                        | SSL key password                                                                                                           |
|MUNICIPAL_CVR| 123456                                                              | CVR of the municipal                                                                                                       |
|MUNICIPAL_NAME| Ukendt Kommune                                                      | Name of the municipal                                                                                                      |
|DB_URL| jdbc:mysql://localhost/os2compliance?useSSL=false&serverTimezone=UTC | JDBC database connection string                                                                                            |
|DB_USERNAME| root                                                                | Database user                                                                                                              |
|DB_PASSWORD| Test1234                                                            | Database password                                                                                                          |
|SAML_ENTITY_ID| https://os2compliance                                          | Entity ID used in SAML metadata                                                                                            |
|SAML_ENTITY_BASE_URL| https://os2compliance:8343                                          | Entity base url used in SAML metadata                                                                                      |
|SAML_METADATA_LOCATION|                                                                     | Url to the IDPs metadata should start with url:                                                                            |
|SAML_KEYSTORE_LOCATION| security/saml-keystore-dev.pfx                                      | SAML keystore location                                                                                                     |
|SAML_KEYSTORE_PASSWORD| Password1234                                                        | SAML keystore password                                                                                                     |
|SAML_ACCEPT_SELF_SIGNED| true                                                               | Accept self signed certificate                                                                                             |
|SAML_ROLE_CLAIM_NAME| roles                                                               | The name of the clain that contains the users roles                                                                        |
|SCHEDULING_ENABLED| true                                                                | If scheduled task should run on this instance, if running multiple instance, make sure only one is running scheduled tasks |
|INTEGRATION_OS2SYNC_MUNICIPAL_CVR| 123456                                                              | Municipal CVR for use in OS2sync integration                                                                               |
|INTEGRATION_OS2SYNC_ENABLED| false                                                               | Is OS2sync integration enabled                                                                                             |
|INTEGRATION_OS2SYNC_CRON| 0 0 10 * * ?                                                        | Cron expression that determinates of often OS2sync is syncronized                                                          |
|INTEGRATION_CVR_ENABLED| false                                                               | IS CVR integration is enabled                                                                                              |
|INTEGRATION_CVR_API_KEY|                                                                     | API Key for the datafordeler middleware integration                                                                        |
|INTEGRATION_CVR_ENDPOINT|                                                                     | Datafordeler middleware endpoint url                                                                                       |
|INTEGRATION_CVR_CRON| 0 11 * * * ?                                                        | Cron expression that determinates how often CVR is syncronized                                                             |
|INTEGRATION_KITOS_ENABLED| false                                                               | Is Kitos integration enabled                                                                                               |
|INTEGRATION_KITOS_CRON| 0 */30 * * * ?                                                      | Cron expression that determinates how often Kitos data is syncronize                                                       |
|INTEGRATION_KITOS_BASE_PATH| https://kitos.dk                                                    | Url to kitos                                                                                                               |
|INTEGRATION_KITOS_USER_EMAIL|                                                                     | Email of the kitos API user                                                                                                |
|INTEGRATION_KITOS_PASSWORD|                                                                     | Password for the kitos API user                                                                                            |
|INTEGRATION_MAIL_ENABLED| false                                                               | Is email integration active                                                                                                |
|INTEGRATION_MAIL_CRON| 0 */5 * * * ?                                                       | Cron expression that determinates how often the mail job is run                                                            |
|INTEGRATION_MAIL_FROM| no-reply@os2compliance.dk                                           | Sender mail on e-mail sent from OS2compliance                                                                              |
|INTEGRATION_MAIL_FROM_NAME| OS2compliance                                                       | Sender name on e-mails sent from OS2compliance                                                                             |
|INTEGRATION_MAIL_USERNAME|                                                                     | SMTP username                                                                                                              |
|INTEGRATION_MAIL_PASSWORD|                                                                     | SMTP password                                                                                                              |
|INTEGRATION_MAIL_HOST|                                                                     | SMTP host                                                                                                                  |


## Happy Coding
