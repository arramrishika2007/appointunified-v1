package com.appointunified;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = {
    SecurityAutoConfiguration.class,
    UserDetailsServiceAutoConfiguration.class
})
@EnableAsync
//@EnableScheduling
public class AppointUnifiedApplication {

    public static void main(String[] args) {
        SpringApplication.run(AppointUnifiedApplication.class, args);
    }
}
