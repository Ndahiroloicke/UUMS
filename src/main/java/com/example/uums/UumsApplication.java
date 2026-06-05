package com.example.uums;

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
