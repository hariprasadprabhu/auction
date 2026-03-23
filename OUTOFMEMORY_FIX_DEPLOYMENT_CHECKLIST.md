# OutOfMemoryError Fix - Deployment Checklist

## Pre-Deployment Verification

- [ ] All code changes compiled without errors
- [ ] Dockerfile updated with increased heap size (1024m)
- [ ] application.yml updated with optimized settings
- [ ] application-prod.yml updated with production settings
- [ ] MemoryMonitor.java created for memory tracking

---

## Files Modified

### Dockerfile
```dockerfile
# ✅ Updated
ENV JAVA_OPTS="-Xmx1024m -Xms512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+ParallelRefProcEnabled -XX:InitiatingHeapOccupancyPercent=35"
```

### application.yml (Dev)
```yaml
# ✅ Updated
hikari:
  maximum-pool-size: 15       # Was 30
  minimum-idle: 4             # Was 8
  
logging:
  com.bid.auction: INFO       # Was DEBUG
  com.zaxxer.hikari: INFO     # Was DEBUG
  org.hibernate.SQL: INFO     # Was DEBUG

hibernate:
  format_sql: false           # Was true
  jdbc:
    batch_size: 15            # Was 20
    fetch_size: 30            # Was 50
```

### application-prod.yml
```yaml
# ✅ Updated
hikari:
  maximum-pool-size: 20       # Was 40
  minimum-idle: 5             # Was 10
  preparation-cache-sql-limit: 250  # NEW
```

### New Files Created
```
✅ OUTOFMEMORY_ERROR_FIX.md
✅ src/main/java/com/bid/auction/config/MemoryMonitor.java
```

---

## Deployment Steps

### Step 1: Build New Docker Image
```bash
cd /home/hari/proj/auction

# Clean previous builds
mvn clean

# Build new JAR with all optimizations
mvn package -DskipTests

# Expected: BUILD SUCCESS (should take 3-5 minutes)
```

### Step 2: Create Docker Image
```bash
# Build with new Dockerfile
docker build -t auction:latest .
docker tag auction:latest auction:fixed-oom

# Verify image
docker inspect auction:latest | grep -i "JAVA_OPTS"
# Should show: "-Xmx1024m -Xms512m ..."
```

### Step 3: Test Locally
```bash
# Run container with port mapping
docker run -p 8080:8080 \
  -e DATABASE_URL="jdbc:postgresql://your-db:5432/auctiondeck" \
  -e DB_USERNAME="postgres" \
  -e DB_PASSWORD="your-password" \
  auction:latest

# In another terminal, check memory
docker stats auction
# Memory should stabilize < 500MB
```

### Step 4: Verify Application Health
```bash
# Wait 30 seconds for app startup
sleep 30

# Check health
curl http://localhost:8080/api/health/status
# Expected: HTTP 200 OK

# Check memory stats
curl http://localhost:8080/api/health/db-connections
# Expected: healthy connection pool with < 5 active connections
```

### Step 5: Load Test
```bash
# Run 25 concurrent requests (simulates load)
for i in {1..25}; do 
  curl http://localhost:8080/api/tournaments &
done

# Monitor memory
docker stats auction
# Should remain stable, no OutOfMemoryError

# Check logs for any OOM errors
docker logs auction | grep -i "memory\|oom\|error"
```

### Step 6: Deploy to Production

#### For Render.com:
```bash
# Push Docker image to registry
docker tag auction:latest your-registry/auction:latest
docker push your-registry/auction:latest

# Deploy via Render
# 1. Go to https://dashboard.render.com
# 2. Select your service
# 3. Click "Manual Deploy" → "Deploy latest commit"
# 4. Wait for deployment (5-10 minutes)
```

#### For Railway:
```bash
# Railway auto-deploys from git push
git add -A
git commit -m "Fix OutOfMemoryError: Increase heap to 1024m, optimize HikariCP and logging"
git push origin main

# Monitor deployment
railway logs

# Expected: App starts successfully without OutOfMemoryError
```

#### For Local/Docker Compose:
```bash
# Update compose.yaml with new image
docker-compose down
docker-compose up -d

# Check logs
docker-compose logs -f auction
```

---

## Post-Deployment Verification

### Immediate Checks (First 5 minutes)
- [ ] Application started successfully
- [ ] No errors in logs
- [ ] Health check endpoint returns 200 OK
- [ ] Memory usage stable (< 500MB in docker stats)
- [ ] No "OutOfMemoryError" messages in logs

### Extended Checks (30+ minutes)
- [ ] Request latency normal (< 100ms p99)
- [ ] No connection timeout errors
- [ ] Memory usage remains stable
- [ ] GC frequency reasonable (< 2 times per minute)
- [ ] Connection pool utilization healthy (< 30%)

### Load Testing (If Available)
```bash
# Run 50 concurrent requests for 5 minutes
ab -n 5000 -c 50 http://production-url/api/tournaments

# Expected results:
# - All requests succeed (0 failures)
# - No timeout errors
# - Response time stable
# - Server memory < 800MB
```

---

## Monitoring After Fix

### Logs to Watch For
```bash
# Good signs:
✅ "Heap Memory: 200MB / 1024MB (19%)"
✅ "activeConnections: 3, idleConnections: 12"
✅ "Request completed in 45ms"

# Bad signs (indicate further optimization needed):
❌ "⚠️ HIGH HEAP USAGE: 850MB / 1024MB (83%)"
❌ "🚨 CRITICAL HEAP USAGE: 950MB / 1024MB (93%)"
❌ "Connection is not available"
❌ "OutOfMemoryError"
```

### Key Metrics to Monitor
```yaml
1. Heap Memory Usage
   - Target: < 70%
   - Warning: > 85%
   - Critical: > 95%

2. Connection Pool
   - Active connections: < 10 (dev) or < 15 (prod)
   - Idle connections: > 0
   - Waiting requests: 0

3. Garbage Collection
   - Frequency: < 2 times per minute
   - Pause time: < 200ms

4. Request Latency
   - Average: < 100ms
   - p99: < 500ms
   - Max: < 2s
```

---

## If OutOfMemoryError Still Occurs

### Step 1: Increase Heap Further
```dockerfile
# Try 1.5GB
ENV JAVA_OPTS="-Xmx1536m -Xms768m -XX:+UseG1GC ..."

# Or 2GB for high-traffic
ENV JAVA_OPTS="-Xmx2048m -Xms1024m -XX:+UseG1GC ..."
```

### Step 2: Further Reduce Connection Pool
```yaml
# If 1024m still overflows:
hikari:
  maximum-pool-size: 10    # Was 15/20
  minimum-idle: 2          # Was 4/5
```

### Step 3: Check for Connection Leaks
```bash
# Monitor active connections
curl http://localhost:8080/api/health/db-connections

# If activeConnections ≈ maximumPoolSize and keeps growing:
# ❌ Connection leak detected
# ✅ Check code for unclosed connections
```

### Step 4: Check Database
```bash
# SSH to database server
psql -U postgres -d auctiondeck

# Check active connections
SELECT COUNT(*) FROM pg_stat_activity;
# Should be < 20

# Check for long-running queries
SELECT pid, state, query 
FROM pg_stat_activity 
WHERE state != 'idle' 
LIMIT 10;
```

---

## Rollback Plan

If OutOfMemoryError still occurs after deployment:

```bash
# Option 1: Revert Docker image
docker run -p 8080:8080 auction:previous-tag

# Option 2: Revert environment variable (quick fix)
# In deployment environment, set:
# JAVA_OPTS="-Xmx1536m -Xms768m ..." (increase heap even more)

# Option 3: Revert git commit
git revert <commit-hash>
git push origin main

# Option 4: Rollback to previous deployment
# Railway: Click "Previous Deploy"
# Render: Click "Deploy Previous"
```

---

## Summary of Changes

| Component | Before | After | Benefit |
|-----------|--------|-------|---------|
| **JVM Max Heap** | 512MB | 1024MB | 2x more memory |
| **Dev Pool Size** | 30 | 15 | 50% less memory |
| **Prod Pool Size** | 40 | 20 | 50% less memory |
| **Min Idle Conn** | 8 (dev) / 10 (prod) | 4 / 5 | Faster startup |
| **Debug Logging** | DEBUG | INFO | Less overhead |
| **Batch Size** | 20 | 15 | Less memory per query |
| **OutOfMemoryError** | ❌ Frequent | ✅ Fixed | Stable operation |

---

## Success Criteria

After deployment, the application should:

✅ Start without OutOfMemoryError  
✅ Handle 25+ concurrent requests (dev) or 50+ (prod)  
✅ Maintain memory < 800MB under normal load  
✅ Respond to requests in < 100ms (p99)  
✅ Show 0 connection timeout errors  
✅ Have 0 "waiting for connection" scenarios  

If all above are met, **the OutOfMemoryError fix is successful!** 🎉

---

## Questions or Issues?

Review these files for details:
- **OUTOFMEMORY_ERROR_FIX.md** - Detailed technical explanation
- **HIKARICP_BEFORE_AFTER.md** - Connection pool configuration details
- **MemoryMonitor.java** - Real-time memory monitoring

