package org.acme;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.annotation.PostConstruct;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Properties;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@QuarkusTest
class FilesResourceTest {
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

  @Test
  void shouldDownloadXTxt() {
    var data = given()
        .when().get("/files/x.txt")
        .then()
        .statusCode(200)
        .header("content-type", "text/plain")
        .header("content-disposition", "attachment; filename*=UTF-8''x.txt")
        .body(is("--x--"))
        .extract()
        .body()
        .asByteArray();
    assertThat(data.length).isEqualTo(5);
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
    var data = given()
        .when().get("/files/" + filename)
        .then()
        .statusCode(200)
        .header("content-type", contentType)
        .header("content-disposition", "attachment; filename*=UTF-8''" + filename)
        .extract()
        .body()
        .asByteArray();
    var expectedLength = Long.parseLong(filesRegistry.getProperty(filename).split(";")[1]);
    assertThat(data.length).isEqualTo(expectedLength);
  }

  @Test
  void shouldReceiveNoContent() {
    given()
        .when().get("/files/y.txt")
        .then()
        .statusCode(204);
  }
}