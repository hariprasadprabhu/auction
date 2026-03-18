# Docker Environment Configuration Guide

## Overview
This guide explains how to properly configure environment variables when running the Auction application in Docker.

## Environment Variables Required

### Production Environment Variables

When running with `SPRING_PROFILES_ACTIVE=prod`, ensure the following environment variables are set:

```bash
# Database Configuration (REQUIRED in production)
DB_URL=jdbc:postgresql://<db-host>:<db-port>/<db-name>
DB_USERNAME=<postgres-username>
DB_PASSWORD=<postgres-password>

# JWT Configuration (REQUIRED)
JWT_SECRET=<your-secret-key-min-256-bits>

# CORS Configuration (Optional - defaults to localhost:4200)
CORS_ALLOWED_ORIGINS=http://localhost:4200,https://yourdomain.com

# Other Configuration (Optional)
LOG_LEVEL=INFO
COM_BID_AUCTION_LOG_LEVEL=DEBUG
JPA_SHOW_SQL=false
```

## Running with Docker Compose

### Option 1: Using Local Postgres (Development-like setup)

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:16-alpine
    container_name: auctiondeck-db
    environment:
      POSTGRES_DB: auctiondeck
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 10s
      timeout: 5s
      retries: 5

  app:
    build: .
    container_name: auctiondeck-app
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: prod
      DB_URL: jdbc:postgresql://postgres:5432/auctiondeck
      DB_USERNAME: postgres
      DB_PASSWORD: postgres
      JWT_SECRET: your-secret-key-here-min-256-bits-long
      CORS_ALLOWED_ORIGINS: http://localhost:4200
    depends_on:
      postgres:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/api/health"]
      interval: 30s
      timeout: 3s
      retries: 3

volumes:
  postgres_data:
```

Run with:
```bash
docker-compose up -d
```

### Option 2: Connecting to External Database

When using a managed database service (e.g., AWS RDS, Railway Postgres, Neon):

```bash
docker run -d \
  --name auctiondeck-app \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DB_URL=jdbc:postgresql://db.example.com:5432/auctiondeck \
  -e DB_USERNAME=dbuser \
  -e DB_PASSWORD=dbpassword \
  -e JWT_SECRET=your-secret-key-here-min-256-bits-long \
  -e CORS_ALLOWED_ORIGINS=https://yourdomain.com \
  auction:latest
```

## Database Connection Strings

### PostgreSQL Local
```
jdbc:postgresql://localhost:5432/auctiondeck
```

### PostgreSQL in Docker Network (when using docker-compose)
```
jdbc:postgresql://postgres:5432/auctiondeck
```

### PostgreSQL Remote (AWS RDS, Railway, Neon, etc.)
```
jdbc:postgresql://host.example.com:5432/auctiondeck
```

### PostgreSQL with Connection Parameters
```
jdbc:postgresql://host:5432/dbname?sslmode=require&connectTimeout=10
```

## Configuration Fallback Chain

The application uses the following fallback chain for configuration:

1. **Environment Variable**: `DB_URL`
2. **Alternative Variable**: `DATABASE_URL`
3. **Default**: `jdbc:postgresql://localhost:5432/auctiondeck`

Example:
```bash
# If DB_URL is set, it takes precedence
DB_URL=jdbc:postgresql://prod-db.example.com:5432/auction

# If DB_URL is not set but DATABASE_URL is, it will use DATABASE_URL
DATABASE_URL=jdbc:postgresql://fallback-db.example.com:5432/auction

# If neither is set, defaults to localhost
# (useful for local development only)
```

## Common Issues and Solutions

### Issue 1: Connection Refused
**Error**: `Connection to localhost:5432 refused`

**Cause**: The environment variable `DB_URL` is not set, and the app is trying to connect to the localhost default.

**Solution**: 
- Set the `DB_URL` environment variable
- If using Docker network, use the service name: `jdbc:postgresql://postgres:5432/auctiondeck`

### Issue 2: Authentication Failed
**Error**: `FATAL: password authentication failed for user "postgres"`

**Cause**: Wrong username or password in environment variables.

**Solution**:
- Verify `DB_USERNAME` and `DB_PASSWORD` match the database credentials
- Check for special characters in password (may need URL encoding)

### Issue 3: Database Not Found
**Error**: `database "auctiondeck" does not exist`

**Cause**: Database hasn't been created yet.

**Solution**:
- Ensure the database is created before starting the app
- Or use a managed database service that handles this automatically
- For Docker Compose, wait for Postgres to be healthy before starting app

### Issue 4: Slow Connection Startup
**Error**: Application takes long time to start or timeout

**Cause**: Database might not be ready when app starts.

**Solution**:
- Use `depends_on` with `condition: service_healthy` in docker-compose
- Implement connection retry logic (already configured in HikariCP)

## Testing Database Connection

### From Inside Container
```bash
docker exec auctiondeck-app curl -s http://localhost:8080/api/health
```

### From Host Machine
```bash
curl -s http://localhost:8080/api/health
```

### Check Logs
```bash
docker logs auctiondeck-app
```

## Production Deployment Best Practices

1. **Use Environment Variables**: Never hardcode sensitive information
2. **Use Secrets Management**: Use Docker secrets, AWS Secrets Manager, or similar
3. **Set Proper Timeouts**: Ensure database connection timeout is appropriate
4. **Enable Health Checks**: Monitor application health
5. **Use Connection Pooling**: HikariCP is already configured
6. **Log Rotation**: Implement log rotation for production logs
7. **Resource Limits**: Set appropriate memory and CPU limits

Example with Docker Secrets (Swarm):
```bash
docker secret create db_password - < db_password.txt
docker service create \
  --secret db_password \
  -e DB_PASSWORD_FILE=/run/secrets/db_password \
  auction:latest
```

## Environment Variable Priority Order

When the application starts:

1. Check for `SPRING_PROFILES_ACTIVE` environment variable
   - Default: `dev`
   - For production: Set to `prod`

2. Load `application.yml` (base configuration)

3. Load `application-<profile>.yml` (profile-specific configuration)
   - Example: `application-prod.yml` when `SPRING_PROFILES_ACTIVE=prod`

4. Override with environment variables
   - Example: `DB_URL` overrides configuration file values

## Quick Reference

### Development
```bash
docker-compose up -d  # Uses compose.yaml with local Postgres
```

### Production with External DB
```bash
docker run -d \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DB_URL=jdbc:postgresql://your-db-host:5432/auctiondeck \
  -e DB_USERNAME=your-username \
  -e DB_PASSWORD=your-password \
  -e JWT_SECRET=your-secret-key \
  -p 8080:8080 \
  auction:latest
```

### Production with Docker Compose
```bash
# Create .env file with production values
cat > .env << EOF
SPRING_PROFILES_ACTIVE=prod
DB_URL=jdbc:postgresql://postgres:5432/auctiondeck
DB_USERNAME=postgres
DB_PASSWORD=your-secure-password
JWT_SECRET=your-secret-key
CORS_ALLOWED_ORIGINS=https://yourdomain.com
EOF

# Use custom compose file or update compose.yaml with environment variables
docker-compose up -d
```

## Verifying Configuration

Check that the application is using the correct profile:
```bash
docker logs auctiondeck-app | grep "profile is active"
# Output: The following 1 profile is active: "prod"
```

Check database connection:
```bash
docker logs auctiondeck-app | grep "Database"
# Should show successful connection info
```

Check health endpoint:
```bash
curl http://localhost:8080/api/health
# Should return: {"status":"UP"}
```

