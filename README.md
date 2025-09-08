# quarkus--timeout-on-download

should reproduce https://quarkusio.zulipchat.com/#narrow/channel/187030-users/topic/timeout.20on.20filedownload.20with.20quarkus.203.2E25.2E4/near/537660512

build
```
mvn clean install -DskipTests=true
```
control quarkus version for all apps in [pom.xml](pom.xml#L17)

## components

### [files](files)

shared set of files with a descriptor [files.properties](files/files.properties) containing mime-type and size per file

### [service](service)

serves files trough [FilesResource localhost:8092/files](service/src/main/java/org/acme/FilesResource.java)

### [client](client)

reads files and returns name type and size as json with [DownloadResource  localhost:8091/files](client/src/main/java/org/acme/DownloadResource.java)

### [trigger](trigger)

triggers downloads in client and checks them for received size.
check [swagger-ui](http://localhost:8080/q/dev-ui/io.quarkus.quarkus-smallrye-openapi/swagger-ui)

## reproduce 

```
cd service 
quarkus dev
```
```
cd client 
quarkus dev
```
```
cd trigger 
quarkus dev
```


use [swagger-ui](http://localhost:8080/q/dev-ui/io.quarkus.quarkus-smallrye-openapi/swagger-ui) to start downloads.
 
it does reproduce with 1000 requests usingConcurrencyOf 30. once in failed state no request is getting trough any more. i think after making "enough" errors `triggerFailure - jakarta.ws.rs.ProcessingException: The timeout period of 35000ms has been exceeded while executing GET /files/quarkus-all-config.html for server null` because of performance unter heavy fire. we start to see `clientFailure - jakarta.ws.rs.ProcessingException: The timeout of 30000 ms has been exceeded when getting a connection to localhost:8092`.
once they occur the service is "out of order". and when afterwards processing a single request it fails, ass subsequent requests are failing.

it does reproduce with 20 request and [quarkus.rest-client.connection-pool-size=3](client/src/main/resources/application.properties#L5) active.
since this setting did not change between quarkus 3.24.4 and 3.25.4 it is not "for real" reproduced.
what we are looking for is `clientFailure - jakarta.ws.rs.ProcessingException: The timeout of 30000 ms has been exceeded when getting a connection to localhost:8092`
not `triggerFailure - jakarta.ws.rs.ProcessingException: The timeout period of 35000ms has been exceeded while executing GET /files/quarkus-all-config.html for server null`
