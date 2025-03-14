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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/maker-checker")
public class MakerCheckerController {

    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final IdentityService identityService;

    @Autowired
    public MakerCheckerController(RuntimeService runtimeService, TaskService taskService, IdentityService identityService) {
        this.runtimeService = runtimeService;
        this.taskService = taskService;
        this.identityService = identityService;
    }

    /**
     * Get the current authenticated username
     */
    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        return authentication.getName();
    }

    /**
     * Start a new maker-checker process
     */
    @PostMapping("/start")
    public ResponseEntity<?> startProcess(@RequestBody Map<String, Object> requestData) {
        // Extract businessKey
        String businessKey = (String) requestData.get("businessKey");
        if (businessKey == null || businessKey.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Business Key is required!");
        }

        // Get current username
        String username = getCurrentUsername();
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication required");
        }

        // Check if user is in makers group
        if (!isUserInGroup(username, "makers")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access Denied: Only Makers can start processes.");
        }

        try {
            // Add audit information
            requestData.put("createdBy", username);
            requestData.put("createdAt", LocalDateTime.now().toString());
            requestData.put("status", "SUBMITTED");

            // Start process with Business Key
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
                    "maker-checker",
                    businessKey,
                    requestData
            );

            return ResponseEntity.ok(Map.of(
                    "message", "Process started successfully",
                    "processInstanceId", processInstance.getId(),
                    "businessKey", processInstance.getBusinessKey()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error starting process: " + e.getMessage());
        }
    }

    /**
     * Get all active tasks
     */
    @GetMapping("/tasks")
    public ResponseEntity<List<TaskDto>> getTasks(@RequestParam(name = "businessKey", required = false) String businessKey) {
        List<Task> tasks;

        if (businessKey != null && !businessKey.isEmpty()) {
            tasks = taskService.createTaskQuery()
                    .processInstanceBusinessKey(businessKey)
                    .active()
                    .list();
        } else {
            tasks = taskService.createTaskQuery()
                    .active()
                    .list();
        }

        List<TaskDto> taskDtos = tasks.stream()
                .map(task -> new TaskDto(task.getId(), task.getName(), task.getAssignee()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(taskDtos);
    }

    /**
     * Get tasks for the current user
     */
    @GetMapping("/my-tasks")
    public ResponseEntity<?> getMyTasks() {
        String userId = getCurrentUsername();
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication required");
        }

        List<Task> tasks = taskService.createTaskQuery()
                .taskAssignee(userId)
                .active()
                .list();

        // Also get tasks where the user is in the candidate group
        List<String> groups = identityService.createGroupQuery()
                .groupMember(userId)
                .list()
                .stream()
                .map(g -> g.getId())
                .collect(Collectors.toList());

        if (!groups.isEmpty()) {
            List<Task> candidateTasks = taskService.createTaskQuery()
                    .taskCandidateGroupIn(groups)
                    .active()
                    .list();

            // Combine the lists avoiding duplicates
            for (Task task : candidateTasks) {
                if (tasks.stream().noneMatch(t -> t.getId().equals(task.getId()))) {
                    tasks.add(task);
                }
            }
        }

        List<TaskDto> taskDtos = tasks.stream()
                .map(task -> {
                    TaskDto dto = new TaskDto(task.getId(), task.getName(), task.getAssignee());
                    // You could add more information here if needed
                    return dto;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(taskDtos);
    }

    /**
     * Claim a task (assign to self)
     */
    @PostMapping("/claim/{taskId}")
    public ResponseEntity<?> claimTask(@PathVariable String taskId) {
        String userId = getCurrentUsername();
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication required");
        }

        try {
            Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
            if (task == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Task not found");
            }

            // Check if user belongs to the candidate group for the task
            String taskName = task.getName();
            if ("Maker Task".equals(taskName) && !isUserInGroup(userId, "makers")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only makers can claim maker tasks");
            } else if ("Checker Task".equals(taskName) && !isUserInGroup(userId, "checkers")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only checkers can claim checker tasks");
            }

            taskService.claim(taskId, userId);
            return ResponseEntity.ok("Task claimed successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error claiming task: " + e.getMessage());
        }
    }

    /**
     * Complete a maker task and send to checker
     */
    @PostMapping("/complete/maker")
    public ResponseEntity<?> completeMakerTask(@RequestBody Map<String, Object> requestData) {
        String userId = getCurrentUsername();
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication required");
        }

        String taskId = (String) requestData.get("taskId");
        Map<String, Object> variables = (Map<String, Object>) requestData.get("variables");

        if (variables == null) {
            variables = new HashMap<>();
        }

        try {
            Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
            if (task == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Task not found");
            }

            // Verify this is a maker task
            if (!"Maker Task".equals(task.getName())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("This is not a maker task");
            }

            // Verify user is in makers group
            if (!isUserInGroup(userId, "makers")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Access Denied: Only Makers can complete maker tasks.");
            }

            // Add audit information
            variables.put("lastModifiedBy", userId);
            variables.put("lastModifiedAt", LocalDateTime.now().toString());
            variables.put("status", "IN_REVIEW");

            // Complete the task
            taskService.complete(taskId, variables);

            return ResponseEntity.ok("Maker task completed and sent to checker");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error completing task: " + e.getMessage());
        }
    }

    /**
     * Complete a checker task with decision
     */
    @PostMapping("/complete/checker")
    public ResponseEntity<?> completeCheckerTask(@RequestBody Map<String, Object> requestData) {
        String userId = getCurrentUsername();
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication required");
        }

        String taskId = (String) requestData.get("taskId");
        String decision = (String) requestData.get("decision");
        Map<String, Object> variables = (Map<String, Object>) requestData.get("variables");

        if (variables == null) {
            variables = new HashMap<>();
        }

        if (decision == null || decision.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Decision is required (approve, reject, or rework)");
        }

        if (!decision.equals("approve") && !decision.equals("reject") && !decision.equals("rework")) {
            return ResponseEntity.badRequest().body("Invalid decision value. Must be 'approve', 'reject', or 'rework'");
        }

        try {
            Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
            if (task == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Task not found");
            }

            // Verify this is a checker task
            if (!"Checker Task".equals(task.getName())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("This is not a checker task");
            }

            // Verify user is in checkers group
            if (!isUserInGroup(userId, "checkers")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Access Denied: Only Checkers can complete checker tasks.");
            }

            // Add audit information
            variables.put("lastModifiedBy", userId);
            variables.put("lastModifiedAt", LocalDateTime.now().toString());
            variables.put("checkerComments", requestData.get("comments"));
            variables.put("decision", decision);

            // Complete the task
            taskService.complete(taskId, variables);

            return ResponseEntity.ok("Checker task completed with decision: " + decision);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error completing task: " + e.getMessage());
        }
    }

    /**
     * Helper method to check if user belongs to a group
     */
    private boolean isUserInGroup(String userId, String groupId) {
        try {
            return identityService.createGroupQuery()
                    .groupMember(userId)
                    .groupId(groupId)
                    .singleResult() != null;
        } catch (Exception e) {
            // When using Spring Security instead of Camunda Identity Service
            // We can check roles as an alternative
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null) {
                return false;
            }

            if ("makers".equals(groupId)) {
                return authentication.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_MAKER") || a.getAuthority().equals("ROLE_ADMIN"));
            } else if ("checkers".equals(groupId)) {
                return authentication.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_CHECKER") || a.getAuthority().equals("ROLE_ADMIN"));
            }

            return false;
        }
    }
}