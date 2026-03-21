# HikariCP Connection Timeout Fix - Quick Reference

## Problem Fixed
```
ERROR: HikariPool-1 - Connection is not available, request timed out after 60000ms
(total=5, active=5, idle=0, waiting=42)
```

**What was happening**: Connection pool was too small (5 connections), all were active, no idle connections available, and 42 requests were waiting.

---

## Changes Made

### 1. **Development Config** (`application.yml`)
```yaml
datasource:
  hikari:
    maximum-pool-size: 20       # ↑ increased from 5
    minimum-idle: 5             # ↑ increased from 1
    connection-timeout: 60000   # ↑ increased from 30000
    idle-timeout: 300000        # ↓ reduced from 600000
    max-lifetime: 1800000       # kept same
    connection-test-query: SELECT 1        # ✓ new
    leak-detection-threshold: 120000       # ✓ new
    auto-commit: true           # ✓ new
```

### 2. **Production Config** (`application-prod.yml`)
```yaml
datasource:
  hikari:
    maximum-pool-size: 25       # ↑ increased from 5
    minimum-idle: 8             # ↑ increased from 1
    connection-timeout: 60000   # same
    idle-timeout: 300000        # ↓ reduced from 600000
    max-lifetime: 1800000       # same
    connection-init-sql: SELECT 1         # ✓ new
    connection-test-query: SELECT 1       # ✓ new
    auto-commit: true           # already there
    leak-detection-threshold: 120000      # ↑ increased from 60000
    validation-timeout: 5000    # ✓ new
```

---

## What Each Setting Does

| Setting | Before → After | Why This Matters |
|---------|---|---|
| `maximum-pool-size` | 5 → 20/25 | Can handle more concurrent requests |
| `minimum-idle` | 1 → 5/8 | Always has spare connections ready |
| `connection-timeout` | 30s → 60s | More time to get a connection |
| `idle-timeout` | 600s → 300s | Closes stale connections faster |
| `connection-test-query` | none → SELECT 1 | Verifies connection is alive |
| `leak-detection-threshold` | 60s → 120s | Longer warning time for leaks |

---

## Test It Out

### 1. **Start the application**
```bash
mvn spring-boot:run
```

### 2. **Check pool status**
```bash
curl http://localhost:8080/api/health/db-connections
```

**Expected output:**
```json
{
  "success": true,
  "status": "HEALTHY",
  "data": {
    "activeConnections": 2,
    "idleConnections": 3,
    "totalConnections": 5,
    "waitingForConnection": 0,
    "utilizationPercent": 40,
    "isNearCapacity": false,
    "hasWaitingRequests": false
  }
}
```

### 3. **Get detailed diagnostics**
```bash
curl http://localhost:8080/api/health/db-diagnostic
```

### 4. **Test after idle time**
- Let the app sit idle for 10+ minutes
- Make a request
- Check: `curl http://localhost:8080/api/health/status`
- Should get HTTP 200 (not timeout)

---

## Files Changed

| File | Change |
|------|--------|
| `src/main/resources/application.yml` | Updated HikariCP settings |
| `src/main/resources/application-prod.yml` | Updated HikariCP settings |

## Files Created

| File | Purpose |
|------|---------|
| `src/main/java/com/bid/auction/util/ConnectionPoolMonitor.java` | Monitor pool health |
| `src/main/java/com/bid/auction/controller/HealthCheckController.java` | Health check endpoints |
| `HIKARICP_CONNECTION_TIMEOUT_FIX.md` | Detailed documentation |

---

## New Monitoring Endpoints

### `GET /api/health/db-connections`
Returns JSON with current pool statistics
- Active connections
- Idle connections
- Utilization percentage
- Warnings about capacity

### `GET /api/health/db-diagnostic`
Returns plain text diagnostic report
- Detailed breakdown of all settings
- Warning flags for issues
- Recommendations

### `GET /api/health/status`
Simple status check
- Returns 200 if healthy
- Returns 503 if degraded

---

## Why This Fixes the Problem

**Before:**
- Only 5 total connections
- 1 connection kept idle
- 10-minute idle timeout = connections die from lack of use
- No validation when connections return from idle state

**After:**
- 20-25 total connections
- 5-8 connections kept idle
- 5-minute idle timeout = detects dead connections sooner
- **Validates each connection** before use with `SELECT 1`
- **Detects leaks** so connections get returned properly

---

## Troubleshooting

### Still getting timeouts?
1. Check: `curl http://localhost:8080/api/health/db-diagnostic`
2. If `hasWaitingRequests: true` → Increase `maximum-pool-size`
3. If `utilizationPercent: 100` → Connection leak or slow queries
4. Enable debug logging:
   ```yaml
   logging:
     level:
       com.zaxxer.hikari: DEBUG
   ```

### Database connection issues?
```bash
# Test PostgreSQL connection
psql -h localhost -U postgres -d auctiondeck -c "SELECT 1"
```

### Pool keeps exhausting?
- Check for `@Transactional` methods that take too long
- Look for unclosed database operations
- Add `@Transactional(timeout = 30)` to long operations

---

## Version Info

- **Spring Boot**: 3.4.5
- **HikariCP**: Included with Spring Boot
- **Database**: PostgreSQL
- **Java**: 21

---

## Reference Documentation

- [HikariCP Configuration Docs](https://github.com/brettwooldridge/HikariCP/wiki/Configuration)
- [Spring Boot DataSource Auto-config](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.sql.datasource)
- [PostgreSQL JDBC Properties](https://jdbc.postgresql.org/documentation/head/connect.html)

