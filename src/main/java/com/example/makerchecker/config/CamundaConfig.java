package com.example.makerchecker.config;

import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.repository.Deployment;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
public class CamundaConfig {

    private final RepositoryService repositoryService;

    public CamundaConfig(RepositoryService repositoryService) {
        this.repositoryService = repositoryService;
    }

    @Bean
    @Order(90) // Ensure this runs before the ApplicationInitializer
    public Deployment deployProcesses() {
        return repositoryService.createDeployment()
                .addClasspathResource("processes/maker-checker.bpmn")
                .name("maker-checker-deployment")
                .enableDuplicateFiltering(true)
                .deploy();
    }
}