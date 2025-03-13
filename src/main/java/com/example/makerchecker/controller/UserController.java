package com.example.makerchecker.controller;

import org.camunda.bpm.engine.AuthorizationService;
import org.camunda.bpm.engine.IdentityService;
import org.camunda.bpm.engine.authorization.*;
import org.camunda.bpm.engine.identity.Group;
import org.camunda.bpm.engine.identity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private IdentityService identityService;

    @Autowired
    private AuthorizationService authorizationService;

    // Create a user
    @PostMapping("/create")
    public ResponseEntity<String> createUser(@RequestBody Map<String, String> userData) {
        String userId = userData.get("userId");
        String firstName = userData.get("firstName");
        String lastName = userData.get("lastName");
        String email = userData.get("email");
        String password = userData.get("password");

        if (identityService.createUserQuery().userId(userId).singleResult() != null) {
            return ResponseEntity.badRequest().body("User already exists.");
        }

        User newUser = identityService.newUser(userId);
        newUser.setFirstName(firstName);
        newUser.setLastName(lastName);
        newUser.setEmail(email);
        newUser.setPassword(password);
        identityService.saveUser(newUser);

        return ResponseEntity.ok("User created successfully.");
    }

    // Create a group
    @PostMapping("/group/create")
    public ResponseEntity<String> createGroup(@RequestBody Map<String, String> groupData) {
        String groupId = groupData.get("groupId");
        String groupName = groupData.get("groupName");

        if (identityService.createGroupQuery().groupId(groupId).singleResult() != null) {
            return ResponseEntity.badRequest().body("Group already exists.");
        }

        Group group = identityService.newGroup(groupId);
        group.setName(groupName);
        group.setType("WORKFLOW");
        identityService.saveGroup(group);

        return ResponseEntity.ok("Group created successfully.");
    }

    // Assign user to group
    @PostMapping("/assign")
    public ResponseEntity<String> assignUserToGroup(@RequestBody Map<String, String> assignData) {
        String userId = assignData.get("userId");
        String groupId = assignData.get("groupId");

        if (identityService.createUserQuery().userId(userId).singleResult() == null) {
            return ResponseEntity.badRequest().body("User does not exist.");
        }

        if (identityService.createGroupQuery().groupId(groupId).singleResult() == null) {
            return ResponseEntity.badRequest().body("Group does not exist.");
        }

        identityService.createMembership(userId, groupId);

        return ResponseEntity.ok("User assigned to group successfully.");
    }

    // Assign permissions for Maker and Checker roles
    @PostMapping("/permissions/assign")
    public ResponseEntity<String> assignPermissions(@RequestBody Map<String, String> requestData) {
        String userId = requestData.get("userId");
        String groupId = requestData.get("groupId");

        // Check if user exists
        if (identityService.createUserQuery().userId(userId).singleResult() == null) {
            return ResponseEntity.badRequest().body("User does not exist.");
        }

        // Check if group exists
        if (identityService.createGroupQuery().groupId(groupId).singleResult() == null) {
            return ResponseEntity.badRequest().body("Group does not exist.");
        }

        // Create membership
        identityService.createMembership(userId, groupId);

        // Assign permissions
        if (groupId.equals("makers")) {
            grantPermission(groupId, Permissions.CREATE_INSTANCE, Resources.PROCESS_DEFINITION, "maker-checker");
            grantPermission(groupId, Permissions.TASK_WORK, Resources.TASK, "*");
        } else if (groupId.equals("checkers")) {
            grantPermission(groupId, Permissions.TASK_WORK, Resources.TASK, "*");
            grantPermission(groupId, Permissions.UPDATE, Resources.TASK, "*");
        }

        return ResponseEntity.ok("Permissions assigned successfully.");
    }

    // Helper method to grant permissions
    private void grantPermission(String groupId, Permission permission, Resources resource, String resourceId) {
        Authorization authorization = authorizationService.createNewAuthorization(Authorization.AUTH_TYPE_GRANT);
        authorization.setGroupId(groupId);
        authorization.setPermissions(new Permission[]{permission});
        authorization.setResource(resource);
        authorization.setResourceId(resourceId);
        authorizationService.saveAuthorization(authorization);
    }

}

