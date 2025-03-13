package com.example.makerchecker.controller;

import com.example.makerchecker.model.TaskDto;
import org.camunda.bpm.engine.IdentityService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;

import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/maker-checker")
public class MakerCheckerController {

    private final RuntimeService runtimeService;
    private final TaskService taskService;

    @Autowired
    private IdentityService identityService;

    public MakerCheckerController(RuntimeService runtimeService, TaskService taskService) {
        this.runtimeService = runtimeService;
        this.taskService = taskService;
    }

    @PostMapping("/start")
    public ResponseEntity<?> startProcess(@RequestBody Map<String, Object> requestData, Principal principal) {
        // Extract businessKey
        String businessKey = (String) requestData.get("businessKey");
        requestData.remove("businessKey"); // Remove so only variables are passed

        if (!isUserInGroup(principal.getName(), "makers")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access Denied: Only Makers can start processes.");
        }

        if (businessKey == null || businessKey.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Business Key is required!");
        }

        try {
            // Start process with Business Key
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("maker-checker", businessKey, requestData);

            return ResponseEntity.ok(Map.of(
                    "message", "Process started successfully",
                    "processInstanceId", processInstance.getId(),
                    "businessKey", processInstance.getBusinessKey()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error starting process: " + e.getMessage());
        }
    }

    // 🔹 Assign Task to Checker
    @PostMapping("/assign")
    public ResponseEntity<String> assignTaskToChecker(@RequestParam(name = "taskId") String taskId, @RequestParam("checkerUserId") String checkerUserId) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();

        if (task == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Task not found");
        }

        // Assigning task to Checker
        taskService.setAssignee(taskId, checkerUserId);
        return ResponseEntity.ok("Task assigned to Checker: " + checkerUserId);
    }

    // 🔹 Get Tasks for Business Key
    @GetMapping("/tasks")
    public ResponseEntity<List<TaskDto>> getTasks(@RequestParam(name = "businessKey") String businessKey) {
        List<Task> tasks = taskService.createTaskQuery()
                .processInstanceBusinessKey(businessKey)
                .list();

        List<TaskDto> taskDtos = tasks.stream()
                .map(task -> new TaskDto(task.getId(), task.getName(), task.getAssignee()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(taskDtos);
    }

    // 🔹 Complete a Task
    @PostMapping("/complete")
    public ResponseEntity<String> completeTask(@RequestBody Map<String, Object> requestData, Principal principal) {
        String taskId = (String) requestData.get("taskId");
        Map<String, Object> variables = (Map<String, Object>) requestData.get("variables");

        if (!isUserInGroup(principal.getName(), "checkers")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access Denied: Only Checkers can complete tasks.");
        }

        taskService.complete(taskId, variables);

        return ResponseEntity.ok("Task " + taskId + " completed.");
    }

    // Check if user belongs to a group
    private boolean isUserInGroup(String userId, String groupId) {
        return identityService.createGroupQuery().groupMember(userId).groupId(groupId).singleResult() != null;
    }

}
