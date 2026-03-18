# Docker Database Connection Troubleshooting Guide

## Recent Fix (March 18, 2026)

**Issue**: Application failing to start in Docker with error:
```
Connection to localhost:5432 refused. Check that the hostname and port are correct 
and that the postmaster is accepting TCP/IP connections.
```

**Root Cause**: 
The `application-prod.yml` was missing fallback values for database configuration. When the `DB_URL` environment variable wasn't set, the application couldn't establish a database connection.

**Solution Applied**:
Updated `application-prod.yml` to include proper fallback chain:
```yaml
spring:
  datasource:
    url: ${DB_URL:${DATABASE_URL:jdbc:postgresql://localhost:5432/auctiondeck}}
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:postgres}
```

This fallback chain:
1. First tries to use `DB_URL` if set
2. Falls back to `DATABASE_URL` if DB_URL is not set
3. Uses `jdbc:postgresql://localhost:5432/auctiondeck` as last resort

## How to Deploy Correctly

### Option 1: Using Docker Compose (Recommended for Development)

```bash
# The compose.yaml file already has everything configured
docker-compose up -d

# Verify it's running
docker-compose logs -f app
```

### Option 2: External Database (Render, Railway, AWS RDS, etc.)

When deploying to a hosting platform with an external PostgreSQL database:

```bash
docker run -d \
  --name auctiondeck-app \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DB_URL=jdbc:postgresql://your-db-host.com:5432/auctiondeck \
  -e DB_USERNAME=your_username \
  -e DB_PASSWORD=your_password \
  -e JWT_SECRET=your-secret-key \
  -e CORS_ALLOWED_ORIGINS=https://yourdomain.com \
  auction:latest
```

### Option 3: Render.io Deployment

1. Set environment variables in Render dashboard (NOT in render.yaml):
   - `SPRING_PROFILES_ACTIVE` = `prod`
   - `DB_URL` = Your Neon/Render Postgres connection string
   - `JWT_SECRET` = Your JWT secret
   - Any other required variables

2. The application will automatically:
   - Use the production profile (`application-prod.yml`)
   - Connect to the provided database
   - Migrate the schema automatically (ddl-auto: update)

## Verification Steps

### 1. Check if Container is Running
```bash
docker ps | grep auctiondeck
```

### 2. Check Application Logs
```bash
docker logs auctiondeck-app | tail -50
```

### 3. Test Health Endpoint
```bash
curl -s http://localhost:8080/api/health
```

Expected response:
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "PostgreSQL"
      }
    }
  }
}
```

### 4. Check Database Connection String
```bash
docker inspect auctiondeck-app | grep -A 5 "Env"
```

Look for `DB_URL` in the environment variables.

### 5. Verify Profile Being Used
```bash
docker logs auctiondeck-app | grep "profile is active"
```

Should show: `The following 1 profile is active: "prod"`

## Common Issues and Fixes

### Issue 1: Connection Refused with localhost:5432
**Symptom**: 
```
Connection to localhost:5432 refused
```

**Cause**: DB_URL environment variable not properly set

**Fix**:
- For Docker Compose: Make sure you're running `docker-compose up -d`
- For External DB: Explicitly pass the `-e DB_URL=jdbc:postgresql://...` flag
- Check that the database host is accessible from the container

### Issue 2: Authentication Failed
**Symptom**:
```
password authentication failed for user "postgres"
```

**Cause**: Wrong DB_USERNAME or DB_PASSWORD

**Fix**:
- Verify credentials in environment variables
- Check for special characters in password (may need URL encoding)
- Ensure the database user exists with the correct password

### Issue 3: Database Not Found
**Symptom**:
```
database "auctiondeck" does not exist
```

**Cause**: Database hasn't been created

**Fix**:
- For local setup: Database is auto-created by Postgres service
- For external DB: Create the database first
  ```sql
  CREATE DATABASE auctiondeck;
  ```

### Issue 4: Connection Timeout
**Symptom**:
```
Connection timeout (timeout for execution)
```

**Cause**: Database not ready when app starts

**Fix**:
- For Docker Compose: Add healthcheck to postgres service (already in compose.yaml)
- For External DB: Add connection retry configuration or wait before starting
- Increase connection timeout in HikariCP (already set to 30000ms)

### Issue 5: Wrong Profile Active
**Symptom**: Application uses dev settings in production

**Cause**: SPRING_PROFILES_ACTIVE not set

**Fix**:
```bash
# Make sure to set the profile
-e SPRING_PROFILES_ACTIVE=prod

# Verify it worked
docker logs auctiondeck-app | grep "profile is active"
```

## Environment Variable Reference

### Required Variables for Production
```bash
SPRING_PROFILES_ACTIVE=prod
DB_URL=jdbc:postgresql://host:5432/auctiondeck
DB_USERNAME=username
DB_PASSWORD=password
JWT_SECRET=your-secret-key
```

### Database URL Formats
```bash
# Local Docker
jdbc:postgresql://postgres:5432/auctiondeck

# Local Machine
jdbc:postgresql://localhost:5432/auctiondeck

# Remote with SSL (Neon, Supabase, etc.)
jdbc:postgresql://ep-xxxxx.region.aws.neon.tech/dbname?sslmode=require

# Remote RDS
jdbc:postgresql://xxxxxx.rds.amazonaws.com:5432/auctiondeck
```

## Database Connection Pool Settings (HikariCP)

Current configuration in `application-prod.yml`:
```yaml
hikari:
  maximum-pool-size: 5        # Max connections to DB
  minimum-idle: 1             # Min idle connections to maintain
  connection-timeout: 30000   # 30 seconds to acquire connection
  idle-timeout: 600000        # 10 minutes idle timeout
  max-lifetime: 1800000       # 30 minutes max lifetime
```

These are optimized for production. Adjust if needed for your workload.

## Next Steps

1. **For Local Development**:
   - Use `docker-compose up -d`
   - No additional configuration needed

2. **For Render/Railway/Production**:
   - Set all required environment variables in platform dashboard
   - Restart the application
   - Monitor logs for successful startup

3. **For Custom Deployment**:
   - Create a deployment script with proper environment variables
   - Example: `deploy.sh` with docker run command

## Additional Resources

- See `DOCKER_ENVIRONMENT_SETUP.md` for detailed configuration guide
- See `env` file for environment variable template
- Check `application-prod.yml` for profile-specific settings
- Review `Dockerfile` for build and runtime configuration

