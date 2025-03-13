package com.example.makerchecker.config;

import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.repository.Deployment;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CamundaConfig {

    private final RepositoryService repositoryService;

    public CamundaConfig(RepositoryService repositoryService) {
        this.repositoryService = repositoryService;
    }

    @Bean
    public Deployment deployProcesses() {
        return repositoryService.createDeployment()
                .addClasspathResource("processes/maker-checker.bpmn") // Path to your BPMN file
                .name("maker-checker-deployment")
                .enableDuplicateFiltering(true) // Avoid re-deploying unchanged files
                .deploy();
    }
}
