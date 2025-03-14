package com.example.makerchecker.config;

import org.camunda.bpm.engine.AuthorizationService;
import org.camunda.bpm.engine.IdentityService;
import org.camunda.bpm.engine.authorization.Authorization;
import org.camunda.bpm.engine.authorization.Permission;
import org.camunda.bpm.engine.authorization.Permissions;
import org.camunda.bpm.engine.authorization.Resources;
import org.camunda.bpm.engine.identity.Group;
import org.camunda.bpm.engine.identity.User;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.boot.CommandLineRunner;

import java.util.logging.Logger;

@Configuration
public class ApplicationInitializer {
    private static final Logger LOGGER = Logger.getLogger(ApplicationInitializer.class.getName());

    @Bean
    @Order(100) // Run after engine is fully initialized
    public CommandLineRunner initializeUsers(IdentityService identityService,
                                             AuthorizationService authorizationService) {
        return args -> {
            try {
                createUsersAndGroups(identityService, authorizationService);
            } catch (Exception e) {
                LOGGER.severe("Error initializing users and groups: " + e.getMessage());
                // Continue without failing the application startup
            }
        };
    }

    private void createUsersAndGroups(IdentityService identityService, AuthorizationService authorizationService) {
        // Create Maker group if it doesn't exist
        if (identityService.createGroupQuery().groupId("makers").count() == 0) {
            try {
                Group makersGroup = identityService.newGroup("makers");
                makersGroup.setName("Makers");
                makersGroup.setType("WORKFLOW");
                identityService.saveGroup(makersGroup);

                // Set permissions for makers
                try {
                    grantPermission(authorizationService, "makers", Permissions.CREATE_INSTANCE, Resources.PROCESS_DEFINITION, "maker-checker");
                    grantPermission(authorizationService, "makers", Permissions.READ, Resources.PROCESS_DEFINITION, "maker-checker");
                    grantPermission(authorizationService, "makers", Permissions.READ, Resources.PROCESS_INSTANCE, "*");
                    grantPermission(authorizationService, "makers", Permissions.TASK_WORK, Resources.TASK, "*");
                } catch (Exception e) {
                    LOGGER.warning("Could not grant all permissions to makers group: " + e.getMessage());
                }
            } catch (Exception e) {
                LOGGER.warning("Could not create makers group: " + e.getMessage());
            }
        }

        // Create Checker group if it doesn't exist
        if (identityService.createGroupQuery().groupId("checkers").count() == 0) {
            try {
                Group checkersGroup = identityService.newGroup("checkers");
                checkersGroup.setName("Checkers");
                checkersGroup.setType("WORKFLOW");
                identityService.saveGroup(checkersGroup);

                // Set permissions for checkers
                try {
                    grantPermission(authorizationService, "checkers", Permissions.READ, Resources.PROCESS_DEFINITION, "maker-checker");
                    grantPermission(authorizationService, "checkers", Permissions.READ, Resources.PROCESS_INSTANCE, "*");
                    grantPermission(authorizationService, "checkers", Permissions.TASK_WORK, Resources.TASK, "*");
                } catch (Exception e) {
                    LOGGER.warning("Could not grant all permissions to checkers group: " + e.getMessage());
                }
            } catch (Exception e) {
                LOGGER.warning("Could not create checkers group: " + e.getMessage());
            }
        }

        // Create test users
        createUserIfNotExists(identityService, "maker1", "Maker", "One", "maker1@example.com", "password", "makers");
        createUserIfNotExists(identityService, "maker2", "Maker", "Two", "maker2@example.com", "password", "makers");
        createUserIfNotExists(identityService, "checker1", "Checker", "One", "checker1@example.com", "password", "checkers");
        createUserIfNotExists(identityService, "checker2", "Checker", "Two", "checker2@example.com", "password", "checkers");

        // Create admin user with both roles
        if (identityService.createUserQuery().userId("admin").count() == 0) {
            try {
                User adminUser = identityService.newUser("admin");
                adminUser.setFirstName("Admin");
                adminUser.setLastName("User");
                adminUser.setEmail("admin@example.com");
                adminUser.setPassword("admin");
                identityService.saveUser(adminUser);

                identityService.createMembership("admin", "makers");
                identityService.createMembership("admin", "checkers");
            } catch (Exception e) {
                LOGGER.warning("Could not create admin user: " + e.getMessage());
            }
        }
    }

    private void createUserIfNotExists(IdentityService identityService, String userId, String firstName, String lastName, String email, String password, String groupId) {
        if (identityService.createUserQuery().userId(userId).count() == 0) {
            try {
                User user = identityService.newUser(userId);
                user.setFirstName(firstName);
                user.setLastName(lastName);
                user.setEmail(email);
                user.setPassword(password);
                identityService.saveUser(user);

                // Assign to group
                try {
                    identityService.createMembership(userId, groupId);
                } catch (Exception e) {
                    LOGGER.warning("Could not assign user " + userId + " to group " + groupId + ": " + e.getMessage());
                }
            } catch (Exception e) {
                LOGGER.warning("Could not create user " + userId + ": " + e.getMessage());
            }
        }
    }

    private void grantPermission(AuthorizationService authorizationService, String groupId, Permission permission, Resources resource, String resourceId) {
        try {
            // Check if authorization already exists to avoid duplicates
            long existingCount = authorizationService.createAuthorizationQuery()
                    .groupIdIn(groupId)
                    .resourceType(resource)
                    .resourceId(resourceId)
                    .count();

            if (existingCount == 0) {
                Authorization authorization = authorizationService.createNewAuthorization(Authorization.AUTH_TYPE_GRANT);
                authorization.setGroupId(groupId);
                authorization.setPermissions(new Permission[]{permission});
                authorization.setResource(resource);
                authorization.setResourceId(resourceId);
                authorizationService.saveAuthorization(authorization);
            }
        } catch (Exception e) {
            LOGGER.warning("Could not grant permission " + permission + " to group " + groupId + " on resource " + resource + " " + resourceId + ": " + e.getMessage());
        }
    }
}