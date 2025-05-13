package com.fintech;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.sql.*;
import java.util.UUID;

import static io.restassured.RestAssured.given;

public class TransactionTests {
    String BASE_URL = "http://localhost:3001";

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                "jdbc:postgresql://localhost:5432/ng_cash",
                "Kavin", "admin123"
        );
    }

    public int getUserId(String username) {
        Response response = given()
                .contentType("application/json")
                .body("{\"username\":\"" + username + "\", \"password\":\"1234\"}")
                .post(BASE_URL + "/auth/login"); // ✅ corrected login endpoint

        System.out.println("Status Code: " + response.getStatusCode());
        System.out.println("Response Body: " + response.getBody().asString());

        if (response.getStatusCode() != 200) {
            throw new RuntimeException("Login failed for user: " + username);
        }

        return response.jsonPath().getInt("id");
    }

    public String getToken(String username) {
        Response response = given()
                .contentType("application/json")
                .body("{\"username\":\"" + username + "\", \"password\":\"1234\"}")
                .post(BASE_URL + "/auth/login"); // ✅ corrected login endpoint

        if (response.getStatusCode() != 200) {
            throw new RuntimeException("Login failed for user: " + username);
        }

        return response.jsonPath().getString("token");
    }

    public void fundUser(int userId, int amount) throws SQLException {
        String sql = "UPDATE account SET balance = balance + ? WHERE id = (SELECT account_id FROM users WHERE id = ?)";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, amount);
            stmt.setInt(2, userId);
            stmt.executeUpdate();
        }
    }

    public int getBalance(int userId) throws SQLException {
        String sql = "SELECT balance FROM account WHERE id = (SELECT account_id FROM users WHERE id = ?)";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt("balance");
        }
        return -1;
    }

    @Test
    public void testSuccessfulTransfer() throws SQLException {
        String userA = "userA_" + UUID.randomUUID();
        String userB = "userB_" + UUID.randomUUID();

        // Create users
        given().contentType("application/json")
                .body("{\"username\":\"" + userA + "\", \"password\":\"1234\"}")
                .post(BASE_URL + "/users");

        given().contentType("application/json")
                .body("{\"username\":\"" + userB + "\", \"password\":\"1234\"}")
                .post(BASE_URL + "/users");

        int userAId = getUserId(userA);
        int userBId = getUserId(userB);

        fundUser(userAId, 1000);

        String tokenA = getToken(userA);

        // Transfer 200 from A to B
        given().header("Authorization", "Bearer " + tokenA)
                .contentType("application/json")
                .body("{\"username\": \"" + userB + "\", \"amount\": 200}")
                .post(BASE_URL + "/transactions")
                .then().statusCode(201);

        int balanceA = getBalance(userAId);
        int balanceB = getBalance(userBId);

        Assert.assertEquals(balanceA, 800);
        Assert.assertEquals(balanceB, 200);
    }

    @Test
    public void testRollbackOnInvalidUser() throws SQLException {
        String userA = "userA_" + UUID.randomUUID();

        // Create user A
        given().contentType("application/json")
                .body("{\"username\":\"" + userA + "\", \"password\":\"1234\"}")
                .post(BASE_URL + "/users");

        int userAId = getUserId(userA);
        fundUser(userAId, 500);
        int beforeBalance = getBalance(userAId);

        String tokenA = getToken(userA);

        // Attempt to send to invalid user
        given().header("Authorization", "Bearer " + tokenA)
                .contentType("application/json")
                .body("{\"username\": \"invalidUserX\", \"amount\": 100}")
                .post(BASE_URL + "/transactions")
                .then().statusCode(404);

        int afterBalance = getBalance(userAId);
        Assert.assertEquals(afterBalance, beforeBalance);
    }
}
