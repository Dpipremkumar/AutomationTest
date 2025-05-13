package com.fintech;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.hamcrest.Matchers.*;
import static io.restassured.RestAssured.*;
public class ApiTests {

        private String authToken;

    @BeforeClass
    public void setup() {
        RestAssured.baseURI = "http://localhost:3001";

        Response response = given()
                .contentType(ContentType.JSON)
                .body("{\"username\":\"kavin\", \"password\":\"admin123\"}")
                .when()
                .post("/users/login")
                .then()
                .statusCode(200)
                .extract().response(); // extract once here

        authToken = response.jsonPath().getString("token");
        System.out.println("TOKEN: " + authToken);
    }

        @Test (priority = 1)
        public void sqlInjectionGuard() {
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"username\":\"' OR '1'='1\", \"password\":\"anything\"}")
                    .log().all()
                    .when()
                    .post("/users/login")
                    .then()
                    .statusCode(anyOf(equalTo(400), equalTo(401)))
                    .log().all()
                    .body("message", not(isEmptyString()));
        }

        @Test (priority = 2)
        public void unauthorizedOnMissingToken() {
            given()
                    .when()
                    .get("/transactions")
                    .then()
                    .statusCode(401)
                    .log().all()
                    .body("message", not(isEmptyString()));
        }

        @Test(priority = 3)
        public void filterByTypeSent() {
            System.out.println("Using Token: " + authToken);

            given()
                    .header("Authorization", "Bearer " + authToken)
                    .log().all()
                    .when()
                    .get("/transactions?type=sent")
                    .then()
                    .log().all()
                    .statusCode(200)
                    .body("sender", everyItem(equalTo("kavin")));
        }

        @Test (priority = 4)
        public void dbDownError() {
            System.out.println("🔴 STOP the DB container manually before running this test: docker-compose stop ng_db");

            given()
                    .header("Authorization", "Bearer " + authToken)
                    .log().all()
                    .when()
                    .get("/users/balance")
                    .then()
                    .log().all()
                    .statusCode(500)

                    .body("message", containsString("error")); // Adjust based on actual error message
        }

}
