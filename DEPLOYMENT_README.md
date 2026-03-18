# Deployment Guide - Auction Application

## Quick Overview

This application is a Spring Boot REST API for an auction system. It requires:
- **Java 21+**
- **PostgreSQL 12+**
- **Docker** (for containerized deployment)

## Critical Fix (March 18, 2026)

**Issue**: Docker deployment failing with "Connection to localhost:5432 refused"

**Fix Applied**: 
- Updated configuration to include proper environment variable fallbacks
- Created comprehensive deployment documentation
- Added troubleshooting guides

See `DOCKER_CONNECTION_TROUBLESHOOTING.md` and `DOCKER_ENVIRONMENT_SETUP.md` for details.

---

## Table of Contents

1. [Local Development](#local-development)
2. [Docker Deployment](#docker-deployment)
3. [Production Deployment](#production-deployment)
4. [Environment Variables](#environment-variables)
5. [Troubleshooting](#troubleshooting)

---

## Local Development

### Prerequisites
- Java 21+ (use `java -version` to check)
- PostgreSQL 12+ running on localhost:5432
- Maven (or use the included mvnw script)

### Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/hariprasadprabhu/auction.git
   cd auction
   ```

2. **Create database** (optional - app will auto-create)
   ```bash
   psql -U postgres -c "CREATE DATABASE auctiondeck;"
   ```

3. **Set environment variables**
   ```bash
   cp env .env.local
   # Edit .env.local with your configuration
   source .env.local
   ```

4. **Run the application**
   ```bash
   # Using Maven wrapper
   ./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"
   
   # Or using Maven
   mvn spring-boot:run -Dspring.profiles.active=dev
   ```

5. **Verify it's running**
   ```bash
   curl http://localhost:8080/api/health
   # Expected: {"status":"UP"}
   ```

---

## Docker Deployment

### Using Docker Compose (Recommended for Local/Dev)

```bash
# Start the application with PostgreSQL
docker-compose up -d

# View logs
docker-compose logs -f app

# Stop the application
docker-compose down
```

**What happens**:
- Builds the Docker image from Dockerfile
- Starts PostgreSQL container with proper configuration
- Starts the application container
- Sets up networking so app can reach database

### Using Docker Run (External Database)

```bash
# Build the image
docker build -t auction:latest .

# Run with external database
docker run -d \
  --name auctiondeck-app \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DB_URL=jdbc:postgresql://your-db-host:5432/auctiondeck \
  -e DB_USERNAME=postgres \
  -e DB_PASSWORD=your-password \
  -e JWT_SECRET=your-secret-key-here \
  -e CORS_ALLOWED_ORIGINS=https://yourdomain.com \
  auction:latest

# View logs
docker logs -f auctiondeck-app

# Stop the application
docker stop auctiondeck-app
docker rm auctiondeck-app
```

---

## Production Deployment

### Option 1: Render.io (Easiest)

1. **Connect Repository**
   - Go to https://render.com
   - Create new Web Service
   - Connect your GitHub repository
   - Use the `render.yaml` configuration

2. **Set Environment Variables** (in Render Dashboard)
   ```
   SPRING_PROFILES_ACTIVE = prod
   DB_URL = jdbc:postgresql://[your-neon-db-url]
   DB_USERNAME = [your-db-user]
   DB_PASSWORD = [your-db-password]
   JWT_SECRET = [generate-new-secret]
   CORS_ALLOWED_ORIGINS = https://yourdomain.com
   ```

3. **Deploy**
   - Render will automatically deploy on push to main
   - Monitor logs in Render dashboard

### Option 2: AWS ECS/EC2

1. **Build and push Docker image**
   ```bash
   docker build -t myregistry/auction:latest .
   docker push myregistry/auction:latest
   ```

2. **Deploy with environment variables**
   - Use AWS ECS task definitions or EC2 user data
   - Set all required environment variables
   - Point to RDS PostgreSQL database

### Option 3: Self-Hosted Server

1. **SSH into server**
   ```bash
   ssh user@your-server.com
   ```

2. **Install Docker and Docker Compose**
   ```bash
   curl -fsSL https://get.docker.com -o get-docker.sh
   sudo sh get-docker.sh
   sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
   sudo chmod +x /usr/local/bin/docker-compose
   ```

3. **Clone repository and create .env file**
   ```bash
   git clone https://github.com/hariprasadprabhu/auction.git
   cd auction
   
   # Create .env with production values
   cat > .env << 'EOF'
   SPRING_PROFILES_ACTIVE=prod
   DB_URL=jdbc:postgresql://your-db-host:5432/auctiondeck
   DB_USERNAME=postgres
   DB_PASSWORD=your-strong-password
   JWT_SECRET=your-secret-key
   CORS_ALLOWED_ORIGINS=https://yourdomain.com
   EOF
   ```

4. **Run with Docker Compose**
   ```bash
   docker-compose up -d
   ```

5. **Setup reverse proxy (Nginx)**
   ```nginx
   server {
       listen 80;
       server_name yourdomain.com;
       
       location / {
           proxy_pass http://localhost:8080;
           proxy_set_header Host $host;
           proxy_set_header X-Real-IP $remote_addr;
       }
   }
   ```

---

## Environment Variables

### Required for Production
```
SPRING_PROFILES_ACTIVE=prod
DB_URL=jdbc:postgresql://host:5432/auctiondeck
DB_USERNAME=your-username
DB_PASSWORD=your-password
JWT_SECRET=your-secret-key-min-256-bits
```

### Optional
```
PORT=8080
CORS_ALLOWED_ORIGINS=https://yourdomain.com
LOG_LEVEL=INFO
JPA_SHOW_SQL=false
JPA_HIBERNATE_DDL_AUTO=update
```

### Database Connection String Format

```
# PostgreSQL
jdbc:postgresql://host:port/database

# Examples:
jdbc:postgresql://localhost:5432/auctiondeck
jdbc:postgresql://postgres:5432/auctiondeck  (Docker)
jdbc:postgresql://my-db.c.us-east-1.rds.amazonaws.com:5432/auctiondeck  (AWS RDS)
jdbc:postgresql://ep-xxxxx.region.aws.neon.tech/auctiondeck?sslmode=require  (Neon)
```

### Generating JWT Secret

```bash
# Generate a secure 32-character secret
openssl rand -base64 32

# Or use any random secure string of at least 256 bits
```

---

## Verification

### Check if Application is Running
```bash
# Via HTTP
curl http://localhost:8080/api/health

# Via Docker
docker ps | grep auctiondeck
docker logs auctiondeck-app
```

### Expected Health Response
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

### Check Active Profile
```bash
docker logs auctiondeck-app | grep "profile is active"
# Should show: The following 1 profile is active: "prod"
```

### Test Database Connection
```bash
# From inside container
docker exec auctiondeck-app curl -s http://localhost:8080/api/health

# Check logs for database startup
docker logs auctiondeck-app | grep -i "database\|connection\|hibernate"
```

---

## Troubleshooting

### Application won't start - "Connection refused"

**Issue**: `Connection to localhost:5432 refused`

**Solution**:
1. Check `DB_URL` environment variable is set correctly
2. Verify database is running and accessible
3. For Docker: use service name `postgres` not `localhost`
4. For external DB: verify hostname and credentials

See `DOCKER_CONNECTION_TROUBLESHOOTING.md` for detailed solutions.

### Database authentication error

**Issue**: `password authentication failed for user "postgres"`

**Solution**:
1. Verify DB_USERNAME and DB_PASSWORD are correct
2. Check special characters in password
3. Ensure database user exists with correct privileges

### Slow startup / timeout

**Issue**: Application takes very long to start or times out

**Solution**:
1. Ensure database is ready before starting app
2. Check network connectivity between app and database
3. Increase connection timeout if needed
4. Check logs for specific errors

### Wrong profile active

**Issue**: Application using dev settings in production

**Solution**:
1. Set `SPRING_PROFILES_ACTIVE=prod` environment variable
2. Verify it's set: `docker logs app-container | grep "profile is active"`

---

## Configuration Files

| File | Purpose | Tracked |
|------|---------|---------|
| `application.yml` | Base configuration | ✅ Yes |
| `application-dev.yml` | Dev profile config | ✅ Yes |
| `application-prod.yml` | Prod profile config | ❌ No (in .gitignore) |
| `application-prod.yml.template` | Template for prod config | ✅ Yes |
| `.env` | Local environment variables | ❌ No (in .gitignore) |
| `env` | Environment template | ✅ Yes |
| `Dockerfile` | Docker image definition | ✅ Yes |
| `compose.yaml` | Docker Compose definition | ✅ Yes |
| `render.yaml` | Render.io deployment config | ✅ Yes |

---

## Support

For issues or questions:
1. Check `DOCKER_CONNECTION_TROUBLESHOOTING.md`
2. Check `DOCKER_ENVIRONMENT_SETUP.md`
3. Review application logs: `docker logs auctiondeck-app`
4. Check Spring Boot health: `curl http://localhost:8080/api/health`

---

## Security Best Practices

✅ **Do:**
- Store secrets in environment variables
- Use strong passwords (min 15 chars, mixed case, numbers, symbols)
- Enable HTTPS in production
- Use CORS whitelist for specific domains
- Set JPA_HIBERNATE_DDL_AUTO=validate in production
- Rotate JWT secrets periodically
- Use managed database services (AWS RDS, Neon, etc.)

❌ **Don't:**
- Commit `.env` files
- Hardcode secrets in code
- Use default passwords
- Disable HTTPS
- Allow all CORS origins
- Use Auto DDL migration after initial setup

---

**Last Updated**: March 18, 2026

**Recent Changes**:
- Fixed Docker database connection issue
- Added comprehensive environment variable documentation
- Created troubleshooting guides
- Updated deployment instructions

