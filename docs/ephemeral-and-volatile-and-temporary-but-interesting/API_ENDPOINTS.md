# Feature Toggle API Endpoints

This document lists all implemented API endpoints for the feature toggle service.

## Public Endpoints (No Authentication Required)

### Query Toggles
```
GET /public/toggles
```
**Query Parameters:**
- `stage` (required): Stage name (exact match or regex pattern)
- `nameFilter` (optional): Regex pattern to filter toggle names
- `includeDisabled` (optional): Include disabled toggles (default: false)

**Response:** `ToggleQueryResponse` with toggles and metadata

---

## Protected Endpoints (Authentication Required)

### Toggle Management

#### Query Toggles (Protected)
```
GET /api/toggles
```
**Query Parameters:**
- `stage` (required): Stage name (exact match or regex pattern)
- `nameFilter` (optional): Regex pattern to filter toggle names
- `includeDisabled` (optional): Include disabled toggles (default: true)

**Response:** `ToggleQueryResponse` with toggles and metadata

#### List All Toggles
```
GET /api/toggles/all
```
**Response:** List of `ToggleDto` objects (basic toggle info without rules)

#### Create Toggle
```
POST /api/toggles
```
**Request Body:** `CreateToggleRequest`
```json
{
  "name": "new-feature-x",
  "description": "Description of the new feature",
  "createdBy": "admin-user"
}
```

**Response:** `ToggleDto`

#### Update Toggle
```
PUT /api/toggles/{name}
```
**Request Body:** `UpdateToggleRequest`
```json
{
  "description": "Updated description",
  "enabled": true
}
```

**Response:** `ToggleDto`

#### Delete Toggle
```
DELETE /api/toggles/{name}
```
**Response:** 200 OK

---

### Toggle-Stage Relationship Management

#### Add Stage to Toggle
```
POST /api/toggles/{name}/stages/{stageName}
```
**Response:** `ToggleStage` object

#### Remove Stage from Toggle
```
DELETE /api/toggles/{name}/stages/{stageName}
```
**Response:** 200 OK

---

### Toggle Rule Management

#### Get Rules for Toggle Stage
```
GET /api/toggles/{name}/stages/{stageName}/rules
```
**Response:** List of `RuleDto` objects ordered by priority

#### Create Rule
```
POST /api/toggles/{name}/stages/{stageName}/rules
```
**Request Body:** `CreateRuleRequest`
```json
{
  "ruleValue": "enabled",
  "priority": 1,
  "description": "Enable for EU users",
  "criteria": {
    "country": "EU",
    "userType": "premium"
  }
}
```

**Response:** `RuleDto`

#### Update Rule
```
PUT /api/toggles/{name}/stages/{stageName}/rules/{ruleId}
```
**Request Body:** `UpdateRuleRequest`
```json
{
  "ruleValue": "beta",
  "priority": 2,
  "description": "Updated rule description",
  "criteria": {
    "country": "US",
    "age": "/^[1-9][0-9]$/"
  }
}
```

**Response:** `RuleDto`

#### Delete Rule
```
DELETE /api/toggles/{name}/stages/{stageName}/rules/{ruleId}
```
**Response:** 200 OK

---

### Stage Administration

#### List All Stages
```
GET /api/admin/stages
```
**Response:** List of `StageDto` objects

#### Create Stage
```
POST /api/admin/stages
```
**Request Body:** `CreateStageRequest`
```json
{
  "name": "staging",
  "description": "Staging environment",
  "displayOrder": 2,
  "parentStageName": "prod"
}
```

**Response:** `StageDto`

#### Update Stage
```
PUT /api/admin/stages/{name}
```
**Request Body:** `UpdateStageRequest`
```json
{
  "name": "staging-updated",
  "description": "Updated staging environment",
  "displayOrder": 3,
  "parentStageName": "prod"
}
```

**Response:** `StageDto`

#### Delete Stage
```
DELETE /api/admin/stages/{name}
```
**Response:** 200 OK

---

## Data Transfer Objects

### ToggleQueryResponse
```json
{
  "toggles": [ToggleDto],
  "queryMetadata": {
    "stage": "prod",
    "nameFilter": "feature-.*",
    "count": 5,
    "cacheHit": false
  }
}
```

### ToggleDto
```json
{
  "name": "new-feature-x",
  "stage": "prod",
  "description": "Description of the feature",
  "rules": [RuleDto]
}
```

### RuleDto
```json
{
  "priority": 1,
  "value": "enabled",
  "description": "Enable for EU users",
  "criteria": {
    "country": "EU",
    "userType": "premium"
  }
}
```

### StageDto
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "prod",
  "description": "Production environment",
  "displayOrder": 1,
  "parentStageName": null,
  "createdAt": "2023-01-01T00:00:00Z"
}
```

---

## Error Handling

All endpoints return appropriate HTTP status codes:
- `200 OK`: Successful operation
- `400 Bad Request`: Invalid input parameters
- `401 Unauthorized`: Authentication required
- `403 Forbidden`: Insufficient permissions
- `404 Not Found`: Resource not found
- `409 Conflict`: Resource already exists or constraint violation
- `500 Internal Server Error`: Server error

Error responses follow RFC 7807 Problem Details format:
```json
{
  "type": "https://example.com/errors/toggle-not-found",
  "title": "Toggle Not Found",
  "status": 404,
  "detail": "Toggle with name 'unknown-feature' was not found",
  "instance": "/api/toggles/unknown-feature"
}
```

---

## Authentication

All protected endpoints require authentication with the `USER` role:
```java
@RolesAllowed({Roles.USER})
```

Public endpoints do not require authentication.

---

## Caching

Toggle query endpoints (`/public/toggles` and `/api/toggles`) use Google Guava caching with:
- Configurable TTL (default: 60 seconds)
- Configurable maximum size (default: 5 MB)
- Cache statistics available via service methods

Cache key format: `stage:{stage}:filter:{nameFilter}:includeDisabled:{flag}`
