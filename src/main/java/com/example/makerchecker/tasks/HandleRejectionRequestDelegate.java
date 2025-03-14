package com.example.makerchecker.tasks;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.util.logging.Logger;

@Component
public class HandleRejectionRequestDelegate implements JavaDelegate {
    private final Logger LOGGER = Logger.getLogger(HandleRejectionRequestDelegate.class.getName());

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String businessKey = execution.getProcessBusinessKey();
        LOGGER.info("Handling rejected request for business key: " + businessKey);

        // Update status
        execution.setVariable("status", "REJECTED");

        // Get rejection reason if available
        String rejectionReason = (String) execution.getVariable("rejectionReason");
        if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
            rejectionReason = "No reason provided";
        }

        LOGGER.info("Request rejected. Reason: " + rejectionReason);

        // In a real application, here you would:
        // 1. Notify relevant stakeholders
        // 2. Update records in a database
        // 3. Log the rejection for audit purposes

        // Additional handling based on request type can be implemented here
        String requestType = (String) execution.getVariable("requestType");
        LOGGER.info("Rejected request of type: " + requestType);
    }
}