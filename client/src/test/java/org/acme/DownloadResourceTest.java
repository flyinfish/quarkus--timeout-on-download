package org.acme;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Properties;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.annotation.PostConstruct;

@QuarkusTest
class DownloadResourceTest {
    @ConfigProperty(name = "files.dir", defaultValue = "../files")
    String filesDir;

    Properties filesRegistry;

    @PostConstruct
    void loadFilesRegistry() throws IOException {
        filesRegistry = new Properties();
        try (InputStream is = Files.newInputStream(java.nio.file.Path.of(filesDir, "files.properties"))) {
            filesRegistry.load(is);
        }
    }

    @ParameterizedTest
    @CsvSource({
            "quarkus-all-config.html,text/html",
            // too fat "quarkus-all-config.pdf,application/pdf",
            "quarkus-all-config.zip,application/zip",
            "quarkus-guides.html,text/html",
            "quarkus-guides.pdf,application/pdf",
            "quarkus-guides.zip,application/zip"
    })
    void shouldDownload(String filename, String contentType) {
        var typeAndLength = filesRegistry.getProperty(filename).split(";");
        var expectedLength = Integer.parseInt(typeAndLength[1]);
        given().when().accept("application/json")
                .get("/files/" + filename)
                .then()
                .statusCode(200)
                .body("name", equalTo(filename))
                .body("type", equalTo(typeAndLength[0]))
                .body("receivedBytes", equalTo(expectedLength));
    }

    void writeFile(String filename, byte[] data) {
        var dir = new java.io.File("target/downloaded");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        try (var fos = new java.io.FileOutputStream("target/downloaded/" + filename)) {
            fos.write(data);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}