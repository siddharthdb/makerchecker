package com.example.makerchecker.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.logging.Logger;

@Component
@Order(10) // Run very early in the startup process
public class CamundaSchemaCheck implements CommandLineRunner {
    private static final Logger LOGGER = Logger.getLogger(CamundaSchemaCheck.class.getName());

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public CamundaSchemaCheck(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) throws Exception {
        LOGGER.info("Checking Camunda database schema...");

        try {
            // Check if the authorization table exists
            boolean authTableExists = tableExists("ACT_RU_AUTHORIZATION");
            LOGGER.info("Authorization table check: " + (authTableExists ? "exists" : "not found"));

            // Check if the identity tables exist
            boolean userTableExists = tableExists("ACT_ID_USER");
            boolean groupTableExists = tableExists("ACT_ID_GROUP");
            boolean membershipTableExists = tableExists("ACT_ID_MEMBERSHIP");

            LOGGER.info("User table: " + (userTableExists ? "exists" : "not found"));
            LOGGER.info("Group table: " + (groupTableExists ? "exists" : "not found"));
            LOGGER.info("Membership table: " + (membershipTableExists ? "exists" : "not found"));

            // If tables don't exist, the Camunda auto schema update should create them
            // But just in case, log a warning
            if (!authTableExists || !userTableExists || !groupTableExists || !membershipTableExists) {
                LOGGER.warning("Some Camunda tables are missing. Make sure camunda.bpm.database.schema-update=true in application.properties");
            }
        } catch (Exception e) {
            LOGGER.severe("Error checking database schema: " + e.getMessage());
            // We don't want to fail application startup, so just log the error
        }
    }

    private boolean tableExists(String tableName) {
        try {
            // This query works for PostgreSQL and H2
            String query = "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = ? AND table_schema = current_schema()";
            Integer count = jdbcTemplate.queryForObject(query, Integer.class, tableName.toLowerCase());
            return count != null && count > 0;
        } catch (Exception e) {
            LOGGER.warning("Error checking if table exists: " + tableName + " - " + e.getMessage());
            return false;
        }
    }
}