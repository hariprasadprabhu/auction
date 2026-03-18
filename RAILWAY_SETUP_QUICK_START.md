# Railway + Neon Deployment - Quick Setup Guide

## ✅ Files Created/Updated

The following files have been prepared for Railway deployment:

1. **`Dockerfile`** - Docker configuration for Spring Boot
2. **`.railwayignore`** - Files to ignore during build
3. **`src/main/resources/application.yml`** - Updated with environment variables
4. **`src/main/java/com/bid/auction/config/SecurityConfig.java`** - Updated CORS for Railway
5. **`src/main/java/com/bid/auction/config/CorsConfig.java`** - Additional CORS bean (created)
6. **`RAILWAY_NEON_DEPLOYMENT.md`** - Detailed deployment guide

---

## 🚀 Quick Start (5 Steps)

### Step 1: Build Locally
```bash
cd /home/hari/proj/auction
mvn clean package -DskipTests
```
✅ This creates `target/auction-0.0.1-SNAPSHOT.jar`

### Step 2: Get Your Neon Database URL
1. Go to https://console.neon.tech
2. Select your project
3. Copy the PostgreSQL connection string
4. Example: `postgresql://user:password@ep-xxxxx.us-east-1.neon.tech/auctiondeck`

### Step 3: Push to GitHub
```bash
git add .
git commit -m "chore: prepare for Railway deployment"
git push origin main
```

### Step 4: Deploy Backend to Railway

**Option A: Using Railway Dashboard**
1. Go to https://railway.app
2. Sign in with GitHub
3. Click **New Project** → **Deploy from GitHub repo**
4. Select your auction repository
5. Wait for deployment to complete (5-10 minutes)
6. Go to service → **Variables** and add:

```
DATABASE_URL=postgresql://user:password@ep-xxxxx.neon.tech/auctiondeck
CORS_ORIGINS=http://localhost:4200
PORT=8080
```

**Option B: Using Railway CLI**
```bash
# Install Railway CLI (if not already installed)
npm install -g @railway/cli

# Login to Railway
railway login

# Initialize project
railway init

# Deploy
railway up
```

### Step 5: Deploy Frontend to Railway

1. In Railway Dashboard, click **New Service** → **GitHub repo**
2. Select your **Angular repository** (or same repo if monorepo)
3. Choose **Static Site** as deployment type
4. Configure:
   - **Build Command**: `npm install && npm run build`
   - **Publish Directory**: `dist/auction` (adjust based on your Angular build output)
5. Set environment variables:
   ```
   CORS_ORIGINS=https://auction-web.railway.app
   ```

---

## 🔑 Environment Variables for Railway Backend

Add these in Railway Dashboard → Your Backend Service → Variables:

```
# DATABASE
DATABASE_URL=postgresql://user:password@ep-xxxxx.neon.tech/auctiondeck

# CORS - Update with your frontend Railway URL
CORS_ORIGINS=https://auction-web.railway.app,http://localhost:4200

# Server Config
PORT=8080

# Logging (optional)
LOG_LEVEL=INFO
COM_BID_AUCTION_LOG_LEVEL=DEBUG

# JWT (update with your secrets)
JWT_SECRET=YourSuperSecretKeyHereMustBeAtLeast256BitsLongForHmacSha256!!
JWT_EXPIRATION=86400000

# Database behavior
JPA_HIBERNATE_DDL_AUTO=validate
JPA_SHOW_SQL=false
```

---

## 📌 Important Notes

### PostgreSQL Username/Password Issue
Your Neon connection string likely **includes username and password** in the URL:
```
postgresql://postgres:your_password@ep-xxxxx.neon.tech/auctiondeck
```

✅ You can use this **entire string** as `DATABASE_URL` in Railway. Spring Boot will parse it automatically.

### CORS Configuration
- **Local development**: `http://localhost:4200,http://localhost:3000`
- **Production (Railway)**: `https://auction-web.railway.app`
- After both are deployed, update to: `https://auction-web.railway.app,https://auction-api.railway.app`

### Verify Updated Files
```bash
# Check Dockerfile
cat /home/hari/proj/auction/Dockerfile

# Check updated application.yml
cat /home/hari/proj/auction/src/main/resources/application.yml

# Check updated SecurityConfig
cat /home/hari/proj/auction/src/main/java/com/bid/auction/config/SecurityConfig.java

# Check new CorsConfig
cat /home/hari/proj/auction/src/main/java/com/bid/auction/config/CorsConfig.java
```

---

## 🧪 Testing After Deployment

### Test Backend API
```bash
# Replace with your Railway backend URL
curl -X GET "https://auction-api.railway.app/api/health"
```

### Test from Angular Frontend
1. Open your Angular app in browser
2. Open DevTools (F12)
3. Go to Network tab
4. Perform an action that calls backend
5. Verify requests go to your Railway API URL
6. Check Console for CORS errors

### Check Railway Logs
1. Railway Dashboard → Your Service
2. Click **Logs** tab
3. Look for Spring Boot startup logs and any errors

---

## 🔧 Troubleshooting

### Build Fails: "Cannot find Dockerfile"
✅ Solution: Dockerfile is in `/home/hari/proj/auction/Dockerfile`
- Ensure you're deploying from the correct directory
- Railway should auto-detect the Dockerfile

### Database Connection Error
✅ Solution:
1. Verify `DATABASE_URL` is correct in Railway Variables
2. Check Neon whitelist allows Railway IPs:
   - Neon Console → Settings → IP Whitelist
   - Add `0.0.0.0/0` or specific Railway IP ranges

### CORS Error: "Access to XMLHttpRequest blocked"
✅ Solution:
1. Check `CORS_ORIGINS` in Railway Variables
2. Must be `https://` for production (not `http://`)
3. No trailing slashes or paths
4. Example: ✅ `https://auction-web.railway.app` ❌ `https://auction-web.railway.app/`

### Blank Angular Page
✅ Solution:
1. Check Angular build succeeded: Railway → Frontend Service → Logs
2. Verify `environment.prod.ts` has correct API URL
3. Check dist folder structure: `dist/auction/index.html` should exist
4. Publish Directory should point to the actual build output

---

## 📚 Useful Commands

```bash
# Build locally and test
mvn clean package -DskipTests
java -jar target/auction-0.0.1-SNAPSHOT.jar

# Build Docker image locally (optional)
docker build -t auction-backend .
docker run -p 8080:8080 -e DATABASE_URL="your_neon_url" auction-backend

# Test API endpoint
curl -X GET "http://localhost:8080/api/health"

# View Recent Commits
git log --oneline -5

# Push latest changes
git add .
git commit -m "chore: update for deployment"
git push origin main
```

---

## ⏱️ Expected Deployment Timeline

| Step | Time |
|------|------|
| Build Spring Boot JAR | 2-3 min |
| Push to GitHub | 1 min |
| Railway builds Docker image | 3-5 min |
| Railway deploys backend | 2-3 min |
| Backend starts up | 1-2 min |
| **Total for Backend** | **~10 min** |
| Angular build | 2-3 min |
| Railway deploys frontend | 1-2 min |
| **Total for Frontend** | **~5 min** |

---

## 🎯 Next Actions

1. ✅ Files are prepared
2. Run `mvn clean package -DskipTests` to build locally
3. Test locally with: `java -jar target/auction-0.0.1-SNAPSHOT.jar`
4. Get your Neon connection string
5. Push to GitHub
6. Deploy to Railway (follow Step 4-5 above)
7. Set environment variables in Railway Dashboard
8. Test end-to-end

---

## 📞 Support

- **Full Guide**: See `RAILWAY_NEON_DEPLOYMENT.md`
- **Railway Docs**: https://docs.railway.app
- **Neon Docs**: https://neon.tech/docs
- **Spring Boot Deployment**: https://spring.io/guides/gs/deploying-spring-boot-to-cloud/


