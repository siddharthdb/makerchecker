package com.example.makerchecker.tasks;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.util.logging.Logger;

@Component
public class ProcessApprovedRequestDelegate implements JavaDelegate {
    private final Logger LOGGER = Logger.getLogger(ProcessApprovedRequestDelegate.class.getName());

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String businessKey = execution.getProcessBusinessKey();
        LOGGER.info("Processing approved request for business key: " + businessKey);

        // Update status
        execution.setVariable("status", "APPROVED");

        // In a real application, here you would:
        // 1. Retrieve the request details
        // 2. Process the actual business logic (e.g., update a database, call an external service)
        // 3. Record the completion in an audit log

        String requestType = (String) execution.getVariable("requestType");
        LOGGER.info("Request of type " + requestType + " has been approved and processed");

        // Additional processing can be implemented based on the request type
        switch (requestType) {
            case "ACCOUNT_CREATION":
                // Logic for account creation
                LOGGER.info("Processing account creation");
                break;
            case "FUND_TRANSFER":
                // Logic for fund transfer
                LOGGER.info("Processing fund transfer");
                break;
            case "LIMIT_CHANGE":
                // Logic for limit change
                LOGGER.info("Processing limit change");
                break;
            default:
                LOGGER.info("Processing generic request");
                break;
        }
    }
}