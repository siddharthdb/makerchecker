package com.example.makerchecker;

import org.camunda.bpm.spring.boot.starter.annotation.EnableProcessApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableProcessApplication
@ComponentScan(basePackages = {"com.example.makerchecker"}) // Explicitly add package scan
public class MakercheckerApplication {

	public static void main(String[] args) {
		SpringApplication.run(MakercheckerApplication.class, args);
	}
}