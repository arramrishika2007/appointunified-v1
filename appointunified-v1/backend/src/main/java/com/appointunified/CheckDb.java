package com.appointunified;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class CheckDb {
    public static void main(String[] args) {
        String url = "jdbc:postgresql://ep-green-shadow-am8dmx75-pooler.c-5.us-east-1.aws.neon.tech/neondb?sslmode=require&channel_binding=require";
        String user = "neondb_owner";
        String password = "npg_PwG0luIg6fYA";
        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, start_time, end_time, status, professional_id FROM appointments")) {
            System.out.println("=== APPOINTMENTS ===");
            while (rs.next()) {
                System.out.println(rs.getString("id") + " | " + 
                                   rs.getString("start_time") + " | " + 
                                   rs.getString("end_time") + " | " + 
                                   rs.getString("status") + " | " +
                                   rs.getString("professional_id"));
            }
            System.out.println("====================");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
