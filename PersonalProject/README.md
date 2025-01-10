# LiveMigrate: Zero-Downtime Schema Evolution System

LiveMigrate is an innovative solution for performing zero-downtime record structure migrations in concurrent systems. It enables businesses to evolve their data structures without service interruption, supporting modern 24/7 operational requirements.

## System Overview

The system consists of several key components that work together to enable seamless data migration:

- Smart Proxy: Handles request routing and provides transparent access to records
- Migration Coordinator: Orchestrates the migration process
- Version Selector: Manages record version selection
- State Tracker: Monitors migration progress
- React Dashboard: Provides real-time visualization and control

## Prerequisites

Before you begin, ensure you have the following installed:
- Java 17 or later
- Node.js 18 or later
- Maven 3.8 or later
- Podman (or Docker)
- Redis (will be run in a container)

## Project Setup

### 1. Clone the Repository
```bash
git clone [repository-url]
cd live-migrate
```

### 2. Start Redis
We use Redis as our data store. Start it using Podman:
```bash
podman run -d \
  --name redis \
  -p 6379:6379 \
  docker.io/library/redis:latest
```

To verify Redis is running:
```bash
podman ps
```

### 3. Build the Backend
```bash
./mvnw clean package
```

### 4. Set up the Frontend
Navigate to the webapp directory and install dependencies:
```bash
cd src/main/webapp
npm install
```

## Running the Application

### 1. Start the Spring Boot Application
From the project root:
```bash
./mvnw spring-boot:run
```

### 2. Start the React Development Server
In a new terminal, from the webapp directory:
```bash
cd src/main/webapp
npm start
```

The application will be available at:
- Backend API: http://localhost:8080
- Frontend Dashboard: http://localhost:3000

## Testing the Migration Process

### 1. Generate Test Data
First, generate some test records:
```bash
curl -X POST "http://localhost:8080/livemigrate/api/v1/test-data/generate?count=1000"
```

### 2. Verify Initial Data
Check the generated records in Redis:
```bash
podman exec -it redis redis-cli
> SCARD record:all_ids
> GET record:v1:<uuid>  # Replace <uuid> with an actual ID
```

### 3. Start Migration
You can start the migration either through:

a) The Dashboard:
- Open http://localhost:3000
- Click the "Start Migration" button

b) Using curl:
```bash
curl -X POST "http://localhost:8080/livemigrate/api/v1/migration/start"
```

### 4. Monitor Progress
The migration progress can be monitored through:

a) The Dashboard:
- Real-time progress bar
- Status indicators
- Migration metrics

b) API endpoints:
```bash
# Get migration status
curl http://localhost:8080/livemigrate/api/v1/migration/status

# Check specific record
curl http://localhost:8080/livemigrate/api/v1/records/<uuid>
```

### 5. Testing Concurrent Access
While migration is running, you can test concurrent access:
```bash
# Read a record
curl http://localhost:8080/api/v1/records/<uuid>

# Update a V1 record
curl -X PUT "http://localhost:8080/livemigrate/api/v1/records/v1/<uuid>" \
  -H "Content-Type: application/json" \
  -d '{
    "id": "<uuid>",
    "customerData": {
      "name": "Updated Name",
      "email": "updated@email.com",
      "phone": "(555) 123-4567"
    }
  }'
```

### 6. Pause and Resume
Test the pause/resume functionality:

a) Through the Dashboard:
- Click "Pause Migration" button
- Click "Resume Migration" button

b) Using curl:
```bash
# Pause migration
curl -X POST "http://localhost:8080/livemigrate/api/v1/migration/pause"

# Resume migration
curl -X POST "http://localhost:8080/livemigrate/api/v1/migration/resume"
```

## Monitoring and Debugging

### Logging
Application logs can be found in:
```
./logs/application.log
```

### Redis Data Inspection
Connect to Redis and inspect data:
```bash
podman exec -it redis redis-cli

# Check migration state
GET migration:state

# Check migration progress
GET migration:progress

# List all record IDs
SMEMBERS record:all_ids

# Check migrated records count
SCARD migration:migrated_records
```

### Metrics
Application metrics are available at:
```
http://localhost:8080/actuator/metrics
```

## Troubleshooting

### Common Issues and Solutions

1. Redis Connection Issues
```bash
# Check Redis container status
podman ps

# Restart Redis if needed
podman restart redis
```

2. Migration Not Starting
- Verify Redis is running
- Check application logs for errors
- Ensure no migration is already in progress

3. Frontend Not Connecting
- Verify the proxy settings in package.json
- Check CORS configuration in Spring Boot
- Ensure both frontend and backend are running

### Reset Migration State
To reset the migration state and start fresh:

1. Clear Redis data:
```bash
podman exec -it redis redis-cli FLUSHALL
```

2. Generate new test data:
```bash
curl -X POST "http://localhost:8080/livemigrate/api/v1/test-data/generate?count=1000"
```

## Architecture Details

### Data Structures

The system manages two versions of record structures:

V1 (Original):
```json
{
    "id": "UUID",
    "createdAt": "timestamp",
    "customerData": {
        "name": "string",
        "email": "string",
        "phone": "string"
    },
    "checksum": "int32"
}
```

V2 (Enhanced):
```json
{
    "id": "UUID",
    "createdAt": "timestamp",
    "customerData": {
        "name": "string",
        "email": "string",
        "phone": "string"
    },
    "checksum": "int32",
    "version": "int16",
    "lastModified": "timestamp",
    "metadata": {
        "source": "string",
        "migrated_at": "timestamp",
        "profile_completeness": "float",
        "consent_status": "boolean"
    }
}
```