# ✅ Railway + Neon Deployment Checklist

## Pre-Deployment (Do These First)

- [ ] **Have your Neon database connection string ready**
  - Go to: https://console.neon.tech
  - Example: `postgresql://user:password@ep-xxxxx.neon.tech/auctiondeck`

- [ ] **Build locally and verify it works**
  ```bash
  mvn clean package -DskipTests
  java -jar target/auction-0.0.1-SNAPSHOT.jar
  # Should start without errors on http://localhost:8080
  ```

- [ ] **Commit and push all changes to GitHub**
  ```bash
  git add .
  git commit -m "chore: prepare for Railway deployment"
  git push origin main
  ```

---

## Railway Backend Deployment

- [ ] **Create Railway account**
  - Go to: https://railway.app
  - Sign in with GitHub

- [ ] **Create new project**
  - Click "New Project"
  - Choose "Deploy from GitHub repo"
  - Select your auction repository

- [ ] **Configure build settings**
  - Railway should auto-detect Dockerfile
  - Build should complete successfully

- [ ] **Set environment variables** (Critical!)
  - Go to Service → Variables tab
  - Add each of these:
    ```
    DATABASE_URL=postgresql://user:password@ep-xxxxx.neon.tech/auctiondeck
    CORS_ORIGINS=http://localhost:4200
    PORT=8080
    LOG_LEVEL=INFO
    COM_BID_AUCTION_LOG_LEVEL=DEBUG
    JWT_SECRET=YourSuperSecretKeyHereMustBeAtLeast256BitsLongForHmacSha256!!
    JWT_EXPIRATION=86400000
    JPA_HIBERNATE_DDL_AUTO=validate
    JPA_SHOW_SQL=false
    ```
  - ✅ Replace DATABASE_URL with your actual Neon string!

- [ ] **Wait for deployment to complete**
  - Usually takes 5-10 minutes
  - Check logs for errors
  - Backend should be running

- [ ] **Note your backend URL**
  - Example: `https://auction-api.railway.app`
  - You'll need this for frontend

- [ ] **Test backend is working**
  ```bash
  curl -X GET "https://auction-api.railway.app/api/health"
  # Should return something (not 404 or 502)
  ```

---

## Railway Frontend Deployment

- [ ] **Create new service for Angular frontend**
  - In Railway Dashboard → New Service
  - Choose "Static Site" (or GitHub repo if separate)

- [ ] **Configure build settings**
  - Build Command: `npm install && npm run build`
  - Publish Directory: `dist/auction` (adjust for your build output)
  - Let Railway auto-detect, or set manually

- [ ] **Wait for deployment to complete**
  - Usually takes 3-5 minutes
  - Check logs for build errors

- [ ] **Note your frontend URL**
  - Example: `https://auction-web.railway.app`
  - You'll need this for CORS

- [ ] **Test frontend loads**
  - Open in browser: `https://auction-web.railway.app`
  - Should see your Angular app

---

## Post-Deployment Configuration

- [ ] **Update backend CORS_ORIGINS variable**
  - Go to Railway Backend Service → Variables
  - Update `CORS_ORIGINS`:
    ```
    https://auction-web.railway.app
    ```
  - Railway automatically redeploys with new variables
  - Wait 1-2 minutes for redeploy

- [ ] **Test API calls from frontend**
  - Open frontend in browser
  - Open DevTools (F12)
  - Go to Network tab
  - Perform an action that calls backend API
  - Verify requests go to your Railway backend URL
  - Verify no CORS errors in Console

- [ ] **Check database connectivity**
  - Do something that queries the database (login, list items, etc.)
  - If it works, your Neon database is connected! ✅

- [ ] **Monitor Railway dashboard**
  - Check Backend Service logs for errors
  - Check Frontend Service logs for errors
  - View "Usage" to confirm you're within free tier

---

## Testing & Verification

- [ ] **Backend API responds**
  ```bash
  curl -X GET "https://auction-api.railway.app/api/health"
  ```

- [ ] **Frontend loads and displays**
  - Open `https://auction-web.railway.app`
  - Page should render without blank screen

- [ ] **Angular makes API calls successfully**
  - Open DevTools Network tab
  - No 403, 401, 404, or 502 errors
  - No CORS errors in Console

- [ ] **Database queries work**
  - Login with valid credentials
  - Load data from database
  - Create/update/delete records

- [ ] **No error logs in Railway**
  - Check Backend Service → Logs
  - Check Frontend Service → Logs

---

## Production Hardening (Optional but Recommended)

- [ ] **Update JWT_SECRET** in Railway
  - Don't use the default secret
  - Use something strong and random

- [ ] **Enable HTTPS** (Already done by Railway)
  - All Railway URLs are HTTPS by default ✅

- [ ] **Set JPA_HIBERNATE_DDL_AUTO to "validate"**
  - Already set in application.yml ✅
  - Prevents accidental schema changes

- [ ] **Limit JPA_SHOW_SQL to false**
  - Already set in application.yml ✅
  - Reduces noise in logs

---

## Troubleshooting Checklist

If something isn't working:

### Backend Won't Start
- [ ] Check DATABASE_URL is set and correct
- [ ] Check port isn't conflicting (PORT=8080)
- [ ] Review build logs for Java/compilation errors
- [ ] Verify Neon IP whitelist allows Railway (or use 0.0.0.0/0)

### CORS Errors
- [ ] Check CORS_ORIGINS is set to your frontend URL
- [ ] Must be `https://` not `http://` for production
- [ ] No trailing slashes (✅ `https://auction-web.railway.app` ❌ `https://auction-web.railway.app/`)
- [ ] Restart backend after updating CORS_ORIGINS

### Blank Angular Page
- [ ] Check Angular build succeeded (check Frontend logs)
- [ ] Verify `environment.prod.ts` has correct API URL
- [ ] Check dist folder structure is correct
- [ ] Open browser DevTools and check for JavaScript errors

### Database Connection Error
- [ ] Verify DATABASE_URL matches your Neon string exactly
- [ ] Check credentials are correct
- [ ] Try connecting to Neon directly from your computer:
  ```bash
  psql postgresql://user:password@ep-xxxxx.neon.tech/auctiondeck
  ```
- [ ] If connection succeeds locally but fails in Railway, check IP whitelist

### API Returns 502 Bad Gateway
- [ ] Check Backend Service logs for startup errors
- [ ] Verify all required environment variables are set
- [ ] Restart the service from Railway dashboard

---

## Success Indicators ✅

You'll know everything is working when:

- ✅ Backend URL responds: `https://auction-api.railway.app/api/health`
- ✅ Frontend URL loads: `https://auction-web.railway.app`
- ✅ Frontend makes API calls to backend without errors
- ✅ Database operations work (login, list data, etc.)
- ✅ No CORS errors in browser console
- ✅ No 502 or 503 errors in API responses
- ✅ Railway dashboard shows green checkmarks for both services

---

## Next Steps After Deployment

1. **Share your app URL** with users
2. **Monitor Railway dashboard** regularly
3. **Update code** and push to GitHub for auto-deployment
4. **Upgrade to paid plan** if you exceed $5/month usage
5. **Keep secrets secure** (JWT_SECRET, DB credentials)

---

## Quick Reference: Environment Variables

Copy-paste this and replace with YOUR values:

```
DATABASE_URL=postgresql://YOUR_USER:YOUR_PASSWORD@ep-xxxxx.us-east-1.neon.tech/YOUR_DB_NAME
CORS_ORIGINS=https://auction-web.railway.app
PORT=8080
LOG_LEVEL=INFO
COM_BID_AUCTION_LOG_LEVEL=DEBUG
JWT_SECRET=USE_A_STRONG_SECRET_HERE
JWT_EXPIRATION=86400000
JPA_HIBERNATE_DDL_AUTO=validate
JPA_SHOW_SQL=false
```

---

## Support & Help

- **Railway Docs**: https://docs.railway.app
- **Neon Docs**: https://neon.tech/docs  
- **Detailed Guide**: See `RAILWAY_NEON_DEPLOYMENT.md`
- **Quick Reference**: See `RAILWAY_SETUP_QUICK_START.md`

---

**Happy Deploying! 🚀**

