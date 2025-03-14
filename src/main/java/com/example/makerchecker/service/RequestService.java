package com.example.makerchecker.service;

import com.example.makerchecker.model.RequestDto;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class RequestService {

    private final RuntimeService runtimeService;
    private final HistoryService historyService;

    @Autowired
    public RequestService(RuntimeService runtimeService, HistoryService historyService) {
        this.runtimeService = runtimeService;
        this.historyService = historyService;
    }

    /**
     * Get all active requests
     */
    public List<RequestDto> getAllActiveRequests() {
        List<ProcessInstance> processes = runtimeService.createProcessInstanceQuery()
                .processDefinitionKey("maker-checker")
                .active()
                .list();

        return convertToRequestDtos(processes);
    }

    /**
     * Get all completed requests
     */
    public List<RequestDto> getAllCompletedRequests() {
        List<HistoricProcessInstance> processes = historyService.createHistoricProcessInstanceQuery()
                .processDefinitionKey("maker-checker")
                .finished()
                .list();

        return convertToHistoricRequestDtos(processes);
    }

    /**
     * Get request by business key (active or completed)
     */
    public RequestDto getRequestByBusinessKey(String businessKey) {
        // First check active processes
        ProcessInstance process = runtimeService.createProcessInstanceQuery()
                .processInstanceBusinessKey(businessKey)
                .singleResult();

        if (process != null) {
            Map<String, Object> variables = runtimeService.getVariables(process.getId());
            return convertToRequestDto(process, variables);
        }

        // If not found in active, check history
        HistoricProcessInstance historicProcess = historyService.createHistoricProcessInstanceQuery()
                .processInstanceBusinessKey(businessKey)
                .singleResult();

        if (historicProcess != null) {
            Map<String, Object> variables = historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(historicProcess.getId())
                    .list()
                    .stream()
                    .collect(java.util.stream.Collectors.toMap(
                            var -> var.getName(),
                            var -> var.getValue()
                    ));
            return convertToHistoricRequestDto(historicProcess, variables);
        }

        return null;
    }

    /**
     * Convert a list of process instances to RequestDtos
     */
    private List<RequestDto> convertToRequestDtos(List<ProcessInstance> processes) {
        List<RequestDto> requests = new ArrayList<>();

        for (ProcessInstance process : processes) {
            Map<String, Object> variables = runtimeService.getVariables(process.getId());
            requests.add(convertToRequestDto(process, variables));
        }

        return requests;
    }

    /**
     * Convert a list of historic process instances to RequestDtos
     */
    private List<RequestDto> convertToHistoricRequestDtos(List<HistoricProcessInstance> processes) {
        List<RequestDto> requests = new ArrayList<>();

        for (HistoricProcessInstance process : processes) {
            Map<String, Object> variables = historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(process.getId())
                    .list()
                    .stream()
                    .collect(java.util.stream.Collectors.toMap(
                            var -> var.getName(),
                            var -> var.getValue()
                    ));

            requests.add(convertToHistoricRequestDto(process, variables));
        }

        return requests;
    }

    /**
     * Convert a process instance to a RequestDto
     */
    private RequestDto convertToRequestDto(ProcessInstance process, Map<String, Object> variables) {
        RequestDto request = new RequestDto();
        request.setId(process.getId());
        request.setBusinessKey(process.getBusinessKey());
        request.setRequestType((String) variables.get("requestType"));
        request.setRequestData((String) variables.get("requestData"));
        request.setStatus((String) variables.get("status"));
        request.setMakerComments((String) variables.get("makerComments"));
        request.setCheckerComments((String) variables.get("checkerComments"));
        request.setCreatedBy((String) variables.get("createdBy"));

        // Handle LocalDateTime conversion if stored as String
        String createdAtStr = (String) variables.get("createdAt");
        if (createdAtStr != null) {
            request.setCreatedAt(LocalDateTime.parse(createdAtStr));
        }

        request.setLastModifiedBy((String) variables.get("lastModifiedBy"));

        String lastModifiedAtStr = (String) variables.get("lastModifiedAt");
        if (lastModifiedAtStr != null) {
            request.setLastModifiedAt(LocalDateTime.parse(lastModifiedAtStr));
        }

        return request;
    }

    /**
     * Convert a historic process instance to a RequestDto
     */
    private RequestDto convertToHistoricRequestDto(HistoricProcessInstance process, Map<String, Object> variables) {
        RequestDto request = new RequestDto();
        request.setId(process.getId());
        request.setBusinessKey(process.getBusinessKey());
        request.setRequestType((String) variables.get("requestType"));
        request.setRequestData((String) variables.get("requestData"));
        request.setStatus((String) variables.get("status"));
        request.setMakerComments((String) variables.get("makerComments"));
        request.setCheckerComments((String) variables.get("checkerComments"));
        request.setCreatedBy((String) variables.get("createdBy"));

        // Handle LocalDateTime conversion if stored as String
        String createdAtStr = (String) variables.get("createdAt");
        if (createdAtStr != null) {
            request.setCreatedAt(LocalDateTime.parse(createdAtStr));
        }

        request.setLastModifiedBy((String) variables.get("lastModifiedBy"));

        String lastModifiedAtStr = (String) variables.get("lastModifiedAt");
        if (lastModifiedAtStr != null) {
            request.setLastModifiedAt(LocalDateTime.parse(lastModifiedAtStr));
        }

        return request;
    }
}