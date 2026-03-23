# 🚀 Database Resilience - Operations Quick Reference

## Emergency Health Check (One-Liners)

```bash
# Is the application healthy?
curl -s http://localhost:8080/api/health/status | grep -q "UP" && echo "✅ HEALTHY" || echo "❌ DEGRADED"

# What's the pool utilization?
curl -s http://localhost:8080/api/health/db-connections | jq '.data.utilizationPercent'

# Is circuit breaker open?
curl -s http://localhost:8080/api/health/circuit-breaker | jq '.state'

# How many requests queued?
curl -s http://localhost:8080/api/health/queue-status | jq '.queueSize'

# Full system health in one check
curl -s http://localhost:8080/api/health/full-report
```

---

## Alert Conditions

### 🟢 Green (Healthy)
```
✓ Status: UP
✓ Pool Utilization: < 60%
✓ Circuit Breaker: CLOSED
✓ Queue Size: < 10
✓ Heap Memory: < 70%
```

### 🟡 Yellow (Warning)
```
⚠ Pool Utilization: 60-80%       → Monitor closely
⚠ Queue Size: > 50                → Check database
⚠ Heap Memory: 70-85%             → May need restart
⚠ Circuit Breaker: HALF_OPEN      → Database recovering
```

### 🔴 Red (Critical)
```
🚨 Status: DEGRADED               → Application stressed
🚨 Pool Utilization: > 95%        → Scale up or optimize queries
🚨 Circuit Breaker: OPEN          → Database is down!
🚨 Queue Size: > 500              → Sustained issues
🚨 Heap Memory: > 95%             → OOM crash imminent
```

---

## Common Issues & Fixes

### Issue: Pool Utilization 95%+
```bash
# Step 1: Check active connections
curl http://localhost:8080/api/health/db-connections | jq '.data.activeConnections'

# Step 2: If all connections busy, check:
# - Is database responsive? Test connection
# - Are there slow queries? Check query logs
# - Is there a connection leak? Monitor over time

# Step 3: Increase pool size
# Edit: src/main/resources/application.yml
# Change: datasource.hikari.maximum-pool-size: 30 (or higher)
# Restart application

# Step 4: Verify recovery
curl http://localhost:8080/api/health/status
```

### Issue: Circuit Breaker OPEN
```bash
# This means: Database connection failures detected

# Step 1: Check if database is running
psql -h your-db-host -U your-db-user -c "SELECT 1"

# Step 2: Check database logs for errors
# Look for: connection timeouts, resource exhaustion

# Step 3: Database will auto-recover
# CircuitBreaker waits 30 seconds, then tests recovery

# Step 4: Monitor recovery
watch -n 5 'curl -s http://localhost:8080/api/health/circuit-breaker | jq ".state"'

# When it shows "CLOSED" again, recovery is complete
```

### Issue: Queue Size Growing (> 100)
```bash
# Step 1: Determine if it's temporary or sustained
# Run this 3 times with 60-second intervals
curl http://localhost:8080/api/health/queue-status | jq '.queueSize'

# Step 2: If consistently growing:
# Database is likely struggling. Check:
# - Database CPU/Memory
# - Long-running queries
# - Table locks
# - Index missing

# Step 3: If temporary spike:
# Just wait, queue will empty as connections free up
# Monitor: curl http://localhost:8080/api/health/queue-status
```

### Issue: Application Startup Slow
```bash
# This is normal on first start due to:
# - HikariCP creating connections (1-2 minutes)
# - Hibernate schema validation
# - Table initialization

# Step 1: Don't restart yet, wait 2-3 minutes

# Step 2: Check health endpoint
curl http://localhost:8080/api/health/status

# Step 3: If still failing after 5 minutes, check logs
grep ERROR logs/application.log | head -20
```

---

## Monitoring Setup

### For Prometheus

```yaml
# Add to prometheus.yml
- job_name: 'auction-health'
  static_configs:
    - targets: ['localhost:8080']
  metrics_path: '/api/health/db-connections'
  scrape_interval: 30s

# Create alerts
- alert: DatabasePoolDegraded
  expr: pool_utilization > 80
  for: 5m
  
- alert: CircuitBreakerOpen
  expr: circuit_breaker_state == 'OPEN'
  for: 1m
```

### For Grafana

Create dashboard with:
- Graph: Pool Utilization %
- Graph: Active Connections
- Graph: Queue Size
- Status: Circuit Breaker State
- Gauge: Heap Memory %

Refresh every 30 seconds.

### For Kubernetes Liveness Probe

```yaml
livenessProbe:
  httpGet:
    path: /api/health/status
    port: 8080
  initialDelaySeconds: 60
  periodSeconds: 30
  timeoutSeconds: 5
  failureThreshold: 3
```

---

## Performance Baselines

### Normal Operation
```
Pool Utilization:    20-40%
Active Connections:  4-10
Queue Size:          0-5
Response Time:       <100ms
CPU Usage:           <30%
Memory Usage:        <60%
```

### Under Load (Expected)
```
Pool Utilization:    60-80%
Active Connections:  15-20
Queue Size:          20-100
Response Time:       100-500ms
CPU Usage:           50-80%
Memory Usage:        70-85%
```

### Overload (Should Not Happen)
```
Pool Utilization:    >95%
Active Connections:  24-25 (all)
Queue Size:          >100
Response Time:       >1000ms
CPU Usage:           >90%
Memory Usage:        >90%
⚠ ACTION NEEDED: Scale database or connections
```

---

## Log Patterns to Watch

### Normal Patterns ✅
```
[DEBUG] DB Pool Healthy: 5/25 active, 15 idle, 20% util
[INFO] ✓ Connection pool recovered after 1 failure
[DEBUG] Circuit Breaker: CLOSED
```

### Warning Patterns ⚠️
```
[WARN] ⚠️  DB POOL WARNING: 20/25 active, 0 idle, 80% util
[WARN] ⚠️  CIRCUIT BREAKER HALF-OPEN - Testing recovery
[DEBUG] Request queue: 50 items processing
```

### Critical Patterns 🚨
```
[ERROR] 🚨 DB POOL CRITICAL: 25/25 active, 0 idle, 100% util
[ERROR] 🚨 CIRCUIT BREAKER OPEN - Database failures detected: 5 failures
[ERROR] 🚨 Request queue is FULL - rejecting request
```

---

## Scaling Guide

### When to Increase Pool Size

```
Signs:
✓ Pool utilization consistently > 80%
✓ Queue size growing
✓ Circuit breaker opening frequently

Action:
1. Edit application.yml
2. Increase maximum-pool-size (by +5)
3. Restart application
4. Monitor for 1 hour
5. Repeat if needed
```

### When to Scale Database

```
Signs:
✓ Query response time slow (>500ms)
✓ Circuit breaker OPEN
✓ Database CPU at 100%
✓ Increasing pool size doesn't help

Action:
1. Scale database CPU/Memory
2. Add read replicas if applicable
3. Optimize slow queries
4. Add indexes
5. Consider connection pooling at database level
```

### When to Optimize Queries

```
Signs:
✓ Some specific queries timing out
✓ Pool drains quickly
✓ Circuit breaker opens during batch operations

Action:
1. Identify slow query (check logs)
2. Profile query execution
3. Add missing indexes
4. Rewrite query if needed
5. Add pagination if querying large datasets
```

---

## Emergency Procedures

### Restart Application Cleanly

```bash
# Step 1: Enable read-only mode (optional)
# - Stop accepting new writes
# - Return 503 to new requests

# Step 2: Graceful shutdown (30 second timeout)
kill -TERM $PID

# Step 3: Verify shutdown
sleep 10 && ps aux | grep java

# Step 4: Remove old connections from pool
# (happens automatically on restart)

# Step 5: Start application
java -jar auction.jar

# Step 6: Wait for health check to pass
# Typically 60-90 seconds
curl http://localhost:8080/api/health/status

# When you see {"status":"UP"}, it's ready
```

### Emergency Pool Reset (Last Resort)

```bash
# Only if:
# - CircuitBreaker stuck OPEN for > 5 minutes
# - Queue full and not processing
# - Application unresponsive

# In Java application logs (or make HTTP endpoint):
# connectionPoolResilienceManager.emergencyPoolReset()

# This will:
# 1. Soft evict all connections
# 2. Force reconnection
# 3. Reset circuit breaker
# 4. Clear failure counts

# Takes 2-3 seconds
# Monitor: curl http://localhost:8080/api/health/status
```

---

## Database Maintenance Windows

### What to Do Before Restart

```bash
# 1. Drain queue
watch -n 5 'curl http://localhost:8080/api/health/queue-status'
# Wait until queueSize < 10

# 2. Check pool
curl http://localhost:8080/api/health/db-connections
# Wait until activeConnections < 3

# 3. Now safe to restart database
# Application will gracefully handle brief outage
```

### What to Monitor During Restart

```bash
# Watch circuit breaker
watch -n 1 'curl http://localhost:8080/api/health/circuit-breaker | jq ".state"'

# When database comes back up:
# - CircuitBreaker goes HALF_OPEN
# - Then back to CLOSED
# - All automatic!
```

---

## Runbook for On-Call

### 📋 Pre-Incident
- [ ] Bookmark health endpoints
- [ ] Know how to check logs
- [ ] Know how to restart app
- [ ] Know database connection string

### 🚨 During Incident

**Alert: Database Status DEGRADED**
```
1. Run: curl http://localhost:8080/api/health/full-report
2. If Pool Utilization > 95%:
   - Check database (is it slow?)
   - If yes: Wait or scale database
   - If no: Increase pool size in config
   
3. If Circuit Breaker OPEN:
   - Check if database is responding
   - If not: Wait for auto-recovery (30 sec)
   - If yes: Check database logs for issues
   
4. If Queue > 500:
   - Database is likely struggling
   - Check database health
   - May need to scale or optimize queries
```

**Alert: Application Unresponsive**
```
1. Check health endpoint: curl http://localhost:8080/api/health/status
2. If timeout: App may be crashed or hanging
   - Check logs: tail -f logs/application.log
   - Restart: kill and restart Java process
   
3. If returns DEGRADED:
   - Read full report: curl http://localhost:8080/api/health/full-report
   - Follow troubleshooting above
```

### ✅ Post-Incident
- [ ] Review logs for root cause
- [ ] Document what happened
- [ ] Plan preventive measures
- [ ] Update runbook if needed

---

## Quick Commands Summary

```bash
# Health Check
curl http://localhost:8080/api/health/status

# Full Report
curl http://localhost:8080/api/health/full-report

# Pool Stats
curl http://localhost:8080/api/health/db-connections | jq

# Circuit Breaker
curl http://localhost:8080/api/health/circuit-breaker | jq

# Queue Stats
curl http://localhost:8080/api/health/queue-status | jq

# Watch Pool
watch -n 5 'curl -s http://localhost:8080/api/health/db-connections | jq ".data.utilizationPercent"'

# Watch Circuit
watch -n 1 'curl -s http://localhost:8080/api/health/circuit-breaker | jq ".state"'

# Tail Logs
tail -f logs/application.log | grep -E "Circuit|Queue|Pool|CRITICAL"
```

---

**Keep this handy! Your application is now resilient. 🚀**

