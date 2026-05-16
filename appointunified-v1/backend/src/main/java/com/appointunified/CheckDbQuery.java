package com.appointunified;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class CheckDbQuery {
    public static void main(String[] args) throws Exception {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setUrl("jdbc:postgresql://ep-green-shadow-am8dmx75-pooler.c-5.us-east-1.aws.neon.tech/neondb?sslmode=require");
        dataSource.setUsername("neondb_owner");
        dataSource.setPassword("npg_PwG0luIg6fYA");
        
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, status, start_time FROM appointments ORDER BY start_time DESC LIMIT 10")) {
            System.out.println("--- LATEST APPOINTMENTS ---");
            while (rs.next()) {
                System.out.println(rs.getString("id") + " | " + rs.getString("status") + " | " + rs.getString("start_time"));
            }
        }
    }
}
