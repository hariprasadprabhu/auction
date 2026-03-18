# AuctionDeck - Hosting & Deployment Guide
## Free & Low-Cost Solutions for Spring Boot + Angular

---

## 🚀 Quick Comparison Table

| Platform | Backend | Frontend | DB | Cost | Performance | Setup |
|----------|---------|----------|-----|------|-------------|-------|
| **Render** | ✅ FREE | ✅ FREE | ✅ FREE | $0/mo | ⭐⭐⭐⭐ | Easy |
| **Railway** | ✅ FREE* | ✅ FREE* | ✅ FREE* | $5-7/mo | ⭐⭐⭐⭐⭐ | Easy |
| **Heroku** | ❌ Paid | ✅ Paid | ✅ Paid | $7+/mo | ⭐⭐⭐ | Easy |
| **Oracle Cloud** | ✅ FREE | ✅ FREE | ✅ FREE | $0/mo | ⭐⭐⭐ | Medium |
| **DigitalOcean** | ✅ Paid | ✅ Paid | ✅ Paid | $5-12/mo | ⭐⭐⭐⭐ | Medium |
| **AWS Free Tier** | ✅ Paid* | ✅ Paid* | ✅ Paid* | $0-1/mo | ⭐⭐⭐⭐⭐ | Hard |
| **Fly.io** | ✅ Paid | ✅ Paid | ✅ Paid | $3-10/mo | ⭐⭐⭐⭐⭐ | Medium |

---

## 🏆 RECOMMENDED: Render.com (Best Free Option)

### Why Render?
✅ **Completely Free Tier** for both backend and frontend  
✅ **PostgreSQL Free Tier** (includes 90-day limit, auto-spins down)  
✅ **Easy GitHub integration** for auto-deployment  
✅ **Built-in SSL/HTTPS**  
✅ **Good performance** for small-medium apps  
✅ **Simple CORS handling** for Angular-Spring Boot communication  

### Render Setup Steps

#### 1. **Build & Package Your Spring Boot App**
```bash
# In your project root
mvn clean package -DskipTests
```
This creates: `target/auction-0.0.1-SNAPSHOT.jar`

#### 2. **Create Dockerfile for Spring Boot Backend**
Create a file named `Dockerfile` in your project root:
```dockerfile
FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app
COPY target/auction-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENV JAVA_OPTS="-Xmx256m"
CMD ["java", "-jar", "app.jar"]
```

#### 3. **Update application.yml for Production**
```yaml
# src/main/resources/application.yml
spring:
  application:
    name: auction
  datasource:
    url: jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}
    username: ${DB_USER}
    password: ${DB_PASSWORD}
    driver-class-name: org.postgresql.Driver
  
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
  
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 10MB

server:
  port: ${PORT:8080}
  servlet:
    context-path: /api

# Enable CORS for Angular
app:
  cors:
    allowed-origins: ${CORS_ORIGINS:http://localhost:3000,http://localhost:4200}
```

#### 4. **Create .renderignore**
```
# .renderignore
target/
.git/
.gitignore
*.md
src/test/
```

#### 5. **Deploy Backend to Render**
- Go to https://render.com (sign up with GitHub)
- Click "New +" → "Web Service"
- Connect your GitHub repository
- Configure:
  - **Name**: auction-backend
  - **Environment**: Docker
  - **Branch**: main
  - **Build Command**: (leave empty - uses Dockerfile)
  - **Start Command**: (leave empty - uses Dockerfile)
  - **Plan**: Free
  - **Region**: Select closest to your users

#### 6. **Configure Environment Variables**
In Render dashboard, set:
```
DB_HOST=your-postgres-instance.c9akciq32.ng.orgsql.render.com
DB_PORT=5432
DB_NAME=auctiondeck
DB_USER=auctiondeck
DB_PASSWORD=your-secure-password
CORS_ORIGINS=https://your-frontend.onrender.com
```

#### 7. **Deploy Frontend to Render**
- Create `angular.json` build output configuration (usually done)
- Click "New +" → "Static Site"
- Connect GitHub repository (Angular project)
- Configure:
  - **Name**: auction-frontend
  - **Build Command**: `npm run build` (or `ng build --configuration production`)
  - **Publish Directory**: `dist/auction` (or your Angular dist folder)
  - **Environment Variables**: 
    ```
    API_BASE_URL=https://auction-backend.onrender.com/api
    ```

#### 8. **Update Angular API Service**
```typescript
// src/environments/environment.prod.ts
export const environment = {
  production: true,
  apiUrl: 'https://auction-backend.onrender.com/api'
};

// src/environments/environment.ts
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api'
};
```

#### 9. **Update Angular app.module.ts or main API interceptor**
```typescript
import { environment } from './environments/environment';

// In your HTTP service
constructor(private http: HttpClient) {
  this.apiUrl = environment.apiUrl;
}

// Example: fetchAuctions()
fetchAuctions() {
  return this.http.get(`${this.apiUrl}/auctions`);
}
```

---

## 🚂 ALTERNATIVE: Railway.app (Best Performance/Cost)

### Why Railway?
✅ **$5 monthly credit** (free tier)  
✅ **Better performance** than Render  
✅ **Automatic deployments** from GitHub  
✅ **Easy database provisioning**  
✅ **Good for production** apps  

### Railway Setup (Quick)
1. Sign up at https://railway.app
2. Create new project → GitHub import
3. Select your monorepo (or split into 2 repos)
4. Railway auto-detects Spring Boot & Angular
5. Database: Railway → PostgreSQL (auto-configured)
6. Push to GitHub → Auto-deploys

---

## 🆓 FREE: Oracle Cloud Always-Free Tier

### Why Oracle?
✅ **100% Free Forever** (not just trial)  
✅ **2 ARM Compute instances** + **PostgreSQL DB**  
✅ **1TB storage**  
✅ **High performance** VMs  
⚠️ More complex setup (requires Linux knowledge)

### Oracle Setup (Overview)
1. Create Oracle Cloud account (free)
2. Launch 2 ARM instances:
   - Instance 1: Spring Boot backend
   - Instance 2: Angular frontend (or Nginx reverse proxy)
3. Create Autonomous PostgreSQL Database (free tier)
4. SSH into instances and deploy via Docker

---

## 🎯 RECOMMENDED PRODUCTION SETUP

### Architecture Diagram
```
┌─────────────────────────────────────┐
│       Angular Frontend              │
│    (Render Static Site)             │
│   https://auction-ui.onrender.com   │
└─────────────────────────────────────┘
                  ↓
        ┌─────────────────────┐
        │ HTTP/HTTPS Requests │
        └─────────────────────┘
                  ↓
┌─────────────────────────────────────┐
│    Spring Boot API Backend          │
│   (Render Web Service)              │
│ https://auction-api.onrender.com/api│
└─────────────────────────────────────┘
                  ↓
        ┌─────────────────────┐
        │  PostgreSQL DB      │
        │ (Render PostgreSQL) │
        └─────────────────────┘
```

### Deployment Workflow
```bash
# Local development
npm install (Angular)
mvn clean package (Spring Boot)

# Commit to GitHub
git add .
git commit -m "deployment: prepare for production"
git push origin main

# Render auto-deploys both services
# Within 2-5 minutes, your app is live!
```

---

## 📋 Pre-Deployment Checklist

- [ ] Remove hardcoded localhost URLs from code
- [ ] Set environment variables for production database
- [ ] Enable CORS in Spring Boot for frontend domain
- [ ] Update Angular `environment.prod.ts` with backend API URL
- [ ] Test locally with production configuration
- [ ] Add `Dockerfile` to repository
- [ ] Update `pom.xml` with PostgreSQL driver (if missing)
- [ ] Create `.renderignore` or `.gitignore`
- [ ] Test API endpoints with Angular frontend locally
- [ ] Verify database connection pooling is configured

---

## 🔧 Required Dependencies in pom.xml

Ensure these are included:
```xml
<!-- PostgreSQL Driver -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>

<!-- CORS Configuration -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```

---

## 🌐 CORS Configuration in Spring Boot

Add this to your Spring Security config:
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            // ... other config
            .build();
        return http.build();
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(
            System.getenv("CORS_ORIGINS").split(",")
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
```

---

## 💰 Cost Estimation (Monthly)

| Solution | Backend | Frontend | Database | Total |
|----------|---------|----------|----------|-------|
| **Render Free** | $0 | $0 | $0 | **$0** |
| **Railway** | $3-5 | FREE | $3-5 | **$6-10** |
| **DigitalOcean App Platform** | $5 | $5 | $15 | **$25** |
| **AWS EC2 + RDS** | $5 | FREE (S3) | $20 | **$25+** |

---

## 🚨 Important Notes

### Render Free Tier Limitations
- Free PostgreSQL auto-spins down after 90 days of inactivity
- Free web service spins down after 15 minutes of no traffic
- Solution: Use paid plans or ping your endpoint every 14 days

### Production Recommendations
1. Start with **Render Free** for MVP/testing
2. Upgrade to **Railway** when needing 24/7 uptime
3. Move to **DigitalOcean** when traffic increases
4. Scale to **AWS** when you need advanced features

---

## 📚 Next Steps

1. Choose your hosting platform (recommend: **Render**)
2. Create `Dockerfile` in project root
3. Update `application.yml` with environment variables
4. Update Angular environment files
5. Push to GitHub
6. Create accounts on chosen platform
7. Deploy backend, frontend, and database
8. Test your application end-to-end

---

## 🆘 Troubleshooting

| Issue | Solution |
|-------|----------|
| **CORS errors in browser** | Check `CORS_ORIGINS` env var, verify domain is https |
| **Database connection fails** | Verify `DB_HOST`, `DB_PORT`, credentials match Render dashboard |
| **Blank Angular page** | Check that `npm run build` completes successfully, verify dist folder |
| **Spring Boot 502 error** | Check logs in Render dashboard, verify port is 8080 |
| **File uploads failing** | Increase `max-file-size` in application.yml |

---

## 📞 Support Resources

- **Render Docs**: https://render.com/docs
- **Railway Docs**: https://docs.railway.app
- **Spring Boot Deployment**: https://spring.io/guides/gs/deploying-spring-boot-to-cloud/
- **Angular Build**: https://angular.io/guide/deployment


