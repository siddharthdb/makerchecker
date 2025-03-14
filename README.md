# Maker-Checker Workflow API

A simple maker-checker (four-eye principle) REST API implementation using Spring Boot and Camunda BPM.

## Overview

This application implements a maker-checker workflow where requests initiated by a maker must be approved by a checker before being processed. The workflow includes:

1. A maker initiates a request
2. The request is validated
3. A checker reviews the request
4. The checker can approve, reject, or request rework
5. Approved requests are processed
6. Rejected requests are handled accordingly

## Prerequisites

- Java 17+
- Maven 3.8+
- PostgreSQL database

## Database Setup

Create a PostgreSQL database and user:

```sql
CREATE DATABASE makercheckerdb;
CREATE USER maker_checker_user WITH PASSWORD 'mc@123';
GRANT ALL PRIVILEGES ON DATABASE makercheckerdb TO maker_checker_user;
```

## Running the Application

1. Clone the repository
2. Configure the database connection in `src/main/resources/application.properties` if needed
3. Run the application using:

```bash
./mvnw spring-boot:run
```

The application will:
- Start on port 8080
- Deploy the BPMN process
- Create default users and groups
- Initialize the Camunda BPM engine

## Default Users

The application creates the following default users:

| Username | Password |