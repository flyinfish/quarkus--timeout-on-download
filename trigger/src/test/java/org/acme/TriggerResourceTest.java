package org.acme;

import io.quarkus.test.junit.QuarkusTest;

import static org.hamcrest.Matchers.equalTo;

import static org.awaitility.Awaitility.await;

import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

@QuarkusTest
class TriggerResourceTest {
    @Test
    void testTrigger() {
        // initially empty
        given().when().get("/triggers").then().statusCode(204);
        // trigger with defaults
        given()
                .when()
                .post("/triggers")
                .then()
                .statusCode(200)
                .body("requests", equalTo(10))
                .body("minDelayMs", equalTo(0))
                .body("maxDelayMs", equalTo(100));
        // await completion
        await().untilAsserted(() -> given()
                .when()
                .get("/triggers")
                .then()
                .statusCode(200)
                .body("completed", equalTo(true)));
        // expect success
        given().when().get("/triggers/failed").then().statusCode(200).body("size()", equalTo(0)).log().ifValidationFails();
        given().when().get("/triggers/success").then().statusCode(200).body("size()", equalTo(10));
        given().when().get("/triggers").then().statusCode(200).body("success", equalTo(true));
        // delete and empty
        given().when().delete("/triggers").then().statusCode(200);
        given().when().get("/triggers").then().statusCode(204);
    }

}