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

| Username | Password | Role     |
|----------|----------|----------|
| maker1   | password | Maker    |
| maker2   | password | Maker    |
| checker1 | password | Checker  |
| checker2 | password | Checker  |
| admin    | admin    | Both     |

## Camunda Admin Console

You can access the Camunda Admin Console at:

```
http://localhost:8080/camunda/app/admin/default/
```

Use the admin credentials:
- Username: admin
- Password: admin

## API Endpoints

### Authentication

No authentication mechanism is included in this demo. In a production environment, you would implement proper authentication (OAuth2, JWT, etc.).

### Process Management

#### Start a Process

```
POST /api/maker-checker/start
```

Request body:
```json
{
  "businessKey": "REQUEST-123",
  "requestType": "ACCOUNT_CREATION",
  "requestData": "{\"accountName\":\"Test Account\",\"initialDeposit\":1000}",
  "makerComments": "New account request"
}
```

#### Get All Requests

```
GET /api/maker-checker/requests
```

#### Get Request by Business Key

```
GET /api/maker-checker/request/{businessKey}
```

### Task Management

#### Get Tasks for Business Key

```
GET /api/maker-checker/tasks?businessKey={businessKey}
```

#### Get My Tasks

```
GET /api/maker-checker/my-tasks
```

#### Claim a Task

```
POST /api/maker-checker/claim/{taskId}
```

#### Unclaim a Task

```
POST /api/maker-checker/unclaim/{taskId}
```

#### Assign a Task to Another User

```
POST /api/maker-checker/assign?taskId={taskId}&assigneeId={userId}
```

#### Complete a Maker Task

```
POST /api/maker-checker/complete/maker
```

Request body:
```json
{
  "taskId": "123",
  "variables": {
    "makerComments": "Request details verified",
    "additionalData": "Any additional information"
  }
}
```

#### Complete a Checker Task

```
POST /api/maker-checker/complete/checker
```

Request body:
```json
{
  "taskId": "123",
  "decision": "approve",
  "comments": "Approved after verification",
  "variables": {
    "additionalData": "Any additional information"
  }
}
```

Note: `decision` must be one of: `approve`, `reject`, or `rework`.

### User Management

#### Create User

```
POST /api/users/create
```

Request body:
```json
{
  "userId": "newuser",
  "firstName": "New",
  "lastName": "User",
  "email": "newuser@example.com",
  "password": "password"
}
```

#### Create Group

```
POST /api/users/group/create
```

Request body:
```json
{
  "groupId": "newgroup",
  "groupName": "New Group"
}
```

#### Assign User to Group

```
POST /api/users/assign
```

Request body:
```json
{
  "userId": "newuser",
  "groupId": "makers"
}
```

#### Assign Permissions

```
POST /api/users/permissions/assign
```

Request body:
```json
{
  "userId": "newuser",
  "groupId": "makers"
}
```

## Workflow Diagram

The BPMN diagram of the maker-checker process consists of:

1. Start Event
2. Validate Request (Service Task)
3. Maker Task (User Task)
4. Checker Task (User Task)
5. Decision Gateway (Exclusive Gateway)
   - Approve: leads to Process Approved Request and completion
   - Reject: leads to Handle Rejected Request and completion
   - Rework: returns to Maker Task for modifications

## Production Considerations

For a production environment, consider:

1. Implementing proper authentication and authorization
2. Adding data validation and error handling
3. Implementing logging and monitoring
4. Adding unit and integration tests
5. Setting up a CI/CD pipeline
6. Implementing database migration tools
7. Adding documentation using Swagger/OpenAPI

## License

This project is licensed under the Apache License 2.0.
