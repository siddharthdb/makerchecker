package com.example.makerchecker;

import org.camunda.bpm.spring.boot.starter.annotation.EnableProcessApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableProcessApplication
public class MakercheckerApplication {

	public static void main(String[] args) {
		SpringApplication.run(MakercheckerApplication.class, args);
	}

}
