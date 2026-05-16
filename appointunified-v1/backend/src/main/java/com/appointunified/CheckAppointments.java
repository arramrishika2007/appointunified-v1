package com.appointunified;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class CheckAppointments {
    public static void main(String[] args) {
        String url = "jdbc:postgresql://ep-green-shadow-am8dmx75-pooler.c-5.us-east-1.aws.neon.tech/neondb?sslmode=require&channel_binding=require";
        String user = "neondb_owner";
        String password = "npg_PwG0luIg6fYA";
        try (Connection conn = DriverManager.getConnection(url, user, password);
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT a.id, a.status, u.email FROM appointments a " +
                     "JOIN users u ON a.client_id = u.id " +
                     "ORDER BY a.created_at DESC LIMIT 5")) {
            try (ResultSet rs = stmt.executeQuery()) {
                System.out.println("RECENT APPOINTMENTS:");
                while (rs.next()) {
                    System.out.println("ID: " + rs.getString("id") + " | Status: " + rs.getString("status") + " | Client: " + rs.getString("email"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
