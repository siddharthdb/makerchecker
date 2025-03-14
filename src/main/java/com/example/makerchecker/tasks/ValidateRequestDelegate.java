package com.example.makerchecker.tasks;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.util.logging.Logger;

@Component
public class ValidateRequestDelegate implements JavaDelegate {
    private final Logger LOGGER = Logger.getLogger(ValidateRequestDelegate.class.getName());

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        LOGGER.info("Validating request for process instance: " + execution.getProcessInstanceId());

        // Get variables
        String businessKey = execution.getProcessBusinessKey();
        String requestType = (String) execution.getVariable("requestType");
        String requestData = (String) execution.getVariable("requestData");

        // Perform validation logic
        boolean isValid = true;
        String validationMessage = "";

        if (requestType == null || requestType.trim().isEmpty()) {
            isValid = false;
            validationMessage += "Request type is required. ";
        }

        if (requestData == null || requestData.trim().isEmpty()) {
            isValid = false;
            validationMessage += "Request data is required. ";
        }

        // Set the validation result as process variables
        execution.setVariable("isValid", isValid);
        execution.setVariable("validationMessage", validationMessage);

        // Set initial status
        execution.setVariable("status", "SUBMITTED");

        LOGGER.info("Validation completed for business key: " + businessKey +
                ", isValid: " + isValid +
                (isValid ? "" : ", message: " + validationMessage));
    }
}