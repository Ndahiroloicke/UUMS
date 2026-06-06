package com.example.uums;

/**
 * Main entry point for the UUMS (Utility Usage Management System) application.
 * This is a Spring Boot REST API for WASAC/REG utility billing — water and electricity
 * customers, meters, readings, tariffs, bills, payments, and notifications.
 * Enables scheduled tasks (e.g. overdue penalty jobs) via {@code @EnableScheduling}.
 */
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class UumsApplication {

	public static void main(String[] args) {
		SpringApplication.run(UumsApplication.class, args);
	}

}
