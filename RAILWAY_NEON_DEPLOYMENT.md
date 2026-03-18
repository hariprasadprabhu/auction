# Railway + Neon Deployment Guide
## Spring Boot Backend + Angular Frontend + Neon PostgreSQL

---

## 🎯 Architecture Overview

```
┌─────────────────────────────────────┐
│       Angular Frontend              │
│      (Railway Static Site)          │
│   https://auction-web.railway.app   │
└─────────────────────────────────────┘
                  ↓
        ┌─────────────────────┐
        │ HTTP/HTTPS Requests │
        └─────────────────────┘
                  ↓
┌─────────────────────────────────────┐
│    Spring Boot API Backend          │
│     (Railway Web Service)           │
│   https://auction-api.railway.app   │
└─────────────────────────────────────┘
                  ↓
        ┌─────────────────────┐
        │   Neon PostgreSQL   │
        │  (Your DB Setup)    │
        └─────────────────────┘
```

---

## 📋 Prerequisites

- [x] Railway account (sign up at https://railway.app)
- [x] GitHub account with your code pushed
- [x] Neon PostgreSQL database already set up
- [x] Your Neon database connection string (looks like: `postgresql://user:password@host/dbname`)

---

## 🚀 Step 1: Get Your Neon Database Connection Details

1. Go to https://console.neon.tech
2. Select your project
3. Copy the **Connection String** (PostgreSQL format)
4. Format example: `postgresql://user:password@ep-xxxxx.us-east-1.neon.tech/auctiondeck`

**Store this safely** - you'll need it for Railway environment variables.

---

## 🐳 Step 2: Create Dockerfile for Spring Boot

Create a `Dockerfile` in your project root:

```dockerfile
FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app

# Copy the built JAR file
COPY target/auction-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

# Set memory limits
ENV JAVA_OPTS="-Xmx256m -Xms128m"

CMD ["java", "-jar", "app.jar"]
```

---

## ⚙️ Step 3: Update Spring Boot Configuration

### Update `src/main/resources/application.yml`

```yaml
spring:
  application:
    name: auction
  
  datasource:
    url: ${DATABASE_URL}
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: 5
      minimum-idle: 1
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
  
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        jdbc:
          batch_size: 20
        order_inserts: true
        order_updates: true
  
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 10MB

server:
  port: ${PORT:8080}
  servlet:
    context-path: /api
  
  # Enable compression
  compression:
    enabled: true
    min-response-size: 1024

# Logging
logging:
  level:
    root: INFO
    com.bid.auction: DEBUG
```

### Verify `pom.xml` has PostgreSQL driver

```xml
<!-- Add this if missing -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```

---

## 🔐 Step 4: Create CORS Configuration

Create a new file: `src/main/java/com/bid/auction/config/CorsConfig.java`

```java
package com.bid.auction.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;

@Configuration
public class CorsConfig {

    @Value("${app.cors.allowed-origins:http://localhost:4200}")
    private String allowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Parse comma-separated origins from environment variable
        String[] origins = allowedOrigins.split(",");
        configuration.setAllowedOrigins(Arrays.asList(origins));
        
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setExposedHeaders(Arrays.asList("Content-Type", "Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
```

### Update `application.yml` to include CORS config

```yaml
app:
  cors:
    allowed-origins: ${CORS_ORIGINS:http://localhost:4200}
```

### Update Spring Security Config (if you have SecurityConfig)

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(request -> {
                CorsConfiguration config = new CorsConfiguration();
                config.setAllowedOrigins(Arrays.asList(
                    System.getenv("CORS_ORIGINS") != null 
                        ? System.getenv("CORS_ORIGINS").split(",") 
                        : new String[]{"http://localhost:4200"}
                ));
                config.setAllowedMethods(Arrays.asList("*"));
                config.setAllowedHeaders(Arrays.asList("*"));
                config.setAllowCredentials(true);
                return config;
            }))
            .csrf(csrf -> csrf.disable())
            // ... rest of your security config
            .build();
        return http.build();
    }
}
```

---

## 📦 Step 5: Build Your Spring Boot Application

Run this in your project root:

```bash
mvn clean package -DskipTests
```

This creates: `target/auction-0.0.1-SNAPSHOT.jar`

**Commit and push to GitHub:**
```bash
git add Dockerfile src/main/resources/application.yml src/main/java/com/bid/auction/config/CorsConfig.java
git commit -m "chore: prepare for Railway deployment with Neon database"
git push origin main
```

---

## 🚂 Step 6: Deploy Spring Boot Backend to Railway

### 6a. Create Railway Project

1. Go to https://railway.app
2. Sign in with GitHub
3. Click **New Project**
4. Select **Deploy from GitHub repo**
5. Authorize Railway to access your GitHub
6. Select your auction repository
7. Click **Deploy**

### 6b. Configure Build Settings

Railway should auto-detect Docker:
- **Service Name**: `auction-backend`
- **Environment**: Docker
- Click **Deploy** to start building

### 6c. Set Environment Variables

Once the service is created:

1. Go to **Variables** tab in your service
2. Add the following variables:

```
DATABASE_URL=postgresql://user:password@ep-xxxxx.us-east-1.neon.tech/auctiondeck

CORS_ORIGINS=https://auction-web.railway.app

PORT=8080

JAVA_OPTS=-Xmx256m -Xms128m
```

⚠️ **Replace `postgresql://user:password@...`** with your actual Neon connection string!

### 6d. Verify Deployment

1. Wait for the build to complete (5-10 minutes)
2. Click the generated URL (e.g., `https://auction-api.railway.app`)
3. You should see an error or API response, confirming it's running
4. Test an endpoint: `https://auction-api.railway.app/api/health` (if you have a health endpoint)

---

## 🎨 Step 7: Build Angular Application

In your **Angular project directory** (not the Spring Boot one):

```bash
ng build --configuration production
```

Or if using npm scripts:
```bash
npm run build
```

This creates a `dist/` folder with static files.

**Verify the output directory**:
```bash
ls -la dist/
# Should show: auction/ (or your app name)
```

---

## 🌐 Step 8: Update Angular Environment Files

### Update `src/environments/environment.ts` (development)

```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api'
};
```

### Update `src/environments/environment.prod.ts` (production)

```typescript
export const environment = {
  production: true,
  apiUrl: 'https://auction-api.railway.app/api'  // Your Railway backend URL
};
```

⚠️ **Replace with your actual Railway backend URL!**

### Update your API service to use the environment

Example in your service:

```typescript
import { environment } from '@environments/environment';
import { HttpClient } from '@angular/common/http';

@Injectable({ providedIn: 'root' })
export class AuctionService {
  private apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  getAuctions() {
    return this.http.get(`${this.apiUrl}/auctions`);
  }

  createAuction(data: any) {
    return this.http.post(`${this.apiUrl}/auctions`, data);
  }
}
```

---

## 📝 Step 9: Create .railwayignore (Optional)

Create `.railwayignore` in your project root:

```
node_modules/
.git/
.gitignore
*.md
src/test/
src/main/java/**/*Test.java
.angular/
coverage/
```

---

## 🚀 Step 10: Deploy Angular Frontend to Railway

### 10a. Create a new Railway service for Angular

1. Go back to your Railway project dashboard
2. Click **New Service** → **GitHub repo**
3. Select your **Angular repository** (if separate) or same repo
4. Choose **Static Site** deployment

### 10b. Configure Static Site

1. **Service Name**: `auction-frontend`
2. **Build Command**: 
   ```
   npm install && npm run build
   ```
   Or if using Angular CLI:
   ```
   npm install && ng build --configuration production
   ```

3. **Start Command**: Leave empty (it's static)

4. **Publish Directory**: 
   ```
   dist/auction
   ```
   (Adjust based on your Angular app's dist folder name)

### 10c. Set Environment Variables (if needed)

If your Angular app reads from environment variables:
```
API_BASE_URL=https://auction-api.railway.app/api
```

### 10d. Verify Deployment

1. Wait for build to complete (3-5 minutes)
2. Click the generated URL
3. You should see your Angular app loaded
4. Open browser DevTools and test API calls

---

## 🧪 Step 11: Test End-to-End

### Test Backend API

```bash
curl -X GET "https://auction-api.railway.app/api/health"
```

### Test Angular + Backend Communication

1. Open your Angular app in browser: `https://auction-web.railway.app`
2. Open DevTools (F12)
3. Go to Network tab
4. Perform an action that calls the backend (e.g., load auctions list)
5. Verify requests go to `auction-api.railway.app/api/*`
6. Check for CORS errors in Console

---

## 🔗 Step 12: Connect Neon Database (If Not Already)

If your Neon database isn't connected yet:

### In Neon Console

1. Go to https://console.neon.tech
2. Select your project
3. Copy **Connection String** (PostgreSQL)
4. Make sure you have the right database and credentials

### In Railway

1. Go to your `auction-backend` service
2. Click **Variables**
3. Paste the connection string as `DATABASE_URL`
4. Click **Deploy** to apply changes

---

## 🔄 Step 13: Enable Auto-Deployment

Railway auto-deploys when you push to GitHub!

### Deployment Workflow

```bash
# Make changes locally
echo "// your code change" >> src/main/java/com/bid/auction/Example.java

# Build and test locally
mvn clean package

# Commit and push
git add .
git commit -m "feat: add new feature"
git push origin main

# Railway automatically:
# 1. Detects the push
# 2. Rebuilds Docker image
# 3. Redeploys backend service (5-10 mins)
# 4. Frontend redeploys if you pushed Angular changes
```

---

## 📊 Railway Dashboard

Access your Railway dashboard:
- **View Logs**: Click service → **Logs** tab
- **View Environment**: Click service → **Variables** tab
- **View Deployments**: Click service → **Deployments** tab
- **Monitor Usage**: Project → **Usage** tab (check $5 monthly credit)

---

## 🆘 Troubleshooting

### Issue: "Cannot connect to database"

**Solution:**
```bash
# 1. Verify Neon connection string in Railway Variables
# Format: postgresql://user:password@host/dbname

# 2. Check if IP whitelisting is enabled in Neon
# Go to Neon Console → Project → Settings → IP Whitelist
# Add: 0.0.0.0/0 (allow all, or specific Railway IPs)

# 3. Test connection locally
PGPASSWORD=yourpassword psql -h ep-xxxxx.neon.tech -U postgres -d auctiondeck -c "SELECT 1;"
```

### Issue: CORS errors in Angular app

**Error:** `Access to XMLHttpRequest blocked by CORS`

**Solution:**
```bash
# 1. Verify CORS_ORIGINS in Railway backend Variables
# Should be: https://auction-web.railway.app

# 2. Check if requests include credentials
# Update your HTTP interceptor if needed

# 3. Verify CorsConfig.java is deployed
# Check Railway logs for errors
```

### Issue: Angular shows blank page

**Solution:**
```bash
# 1. Check build output
# Railway → Frontend Service → Logs
# Look for build errors

# 2. Verify environment.prod.ts API URL
# Should point to your Railway backend

# 3. Open DevTools Console
# Check for JavaScript errors
```

### Issue: Backend returns 502 Bad Gateway

**Solution:**
```bash
# 1. Check Railway backend logs
# Should see Spring Boot startup logs

# 2. Verify DATABASE_URL and CORS_ORIGINS are set

# 3. Check for errors in logs:
# - Database connection failures
# - Port binding issues

# 4. Restart the service:
# Railway Dashboard → Backend Service → ... → Restart
```

---

## 💰 Cost with Railway + Neon

| Component | Cost |
|-----------|------|
| Railway Backend | Included in $5 monthly credit* |
| Railway Frontend | Included in $5 monthly credit* |
| Neon PostgreSQL | Free tier (5GB storage) |
| **Total** | **~$0-5/month** |

*Railway gives $5/month free credit. Check your usage in the dashboard.

---

## 📚 Additional Resources

- **Railway Docs**: https://docs.railway.app
- **Railway Environments**: https://docs.railway.app/guides/environments
- **Neon Docs**: https://neon.tech/docs
- **Spring Boot & Neon**: https://neon.tech/guides/frameworks/spring-boot
- **Angular Build**: https://angular.io/guide/deployment
- **CORS Guide**: https://developer.mozilla.org/en-US/docs/Web/HTTP/CORS

---

## ✅ Final Checklist

Before going live:

- [ ] Spring Boot builds locally without errors
- [ ] Angular builds locally without errors
- [ ] Dockerfile created in project root
- [ ] application.yml uses environment variables
- [ ] CorsConfig.java is in your project
- [ ] environment.prod.ts points to Railway backend
- [ ] Neon database credentials are correct
- [ ] DATABASE_URL set in Railway Variables
- [ ] CORS_ORIGINS set in Railway Variables
- [ ] Git repository is public or Railway has access
- [ ] Backend service deployed and running
- [ ] Frontend service deployed and running
- [ ] Test API calls from Angular app
- [ ] Verify database queries work
- [ ] Check logs for any errors
- [ ] Monitor Railway dashboard for usage

---

## 🎉 You're All Set!

Your application should now be live on Railway with Neon database!

**Frontend**: `https://auction-web.railway.app`  
**Backend**: `https://auction-api.railway.app/api`  
**Database**: Your Neon PostgreSQL instance

Any changes you push to GitHub will auto-deploy within 5-10 minutes.


