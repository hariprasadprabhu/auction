# HikariCP Connection Pool Timeout Fix

## Problem Description
```
ERROR: HikariPool-1 - Connection is not available, request timed out after 60000ms 
(total=5, active=5, idle=0, waiting=42)
```

This occurs when the application is idle and connections are not being reused or validated after they timeout from the database side.

---

## Root Causes

### 1. **Connection Pool Size Too Small**
- **Original**: `maximum-pool-size: 5` 
- **Problem**: With only 5 connections total and all being used, new requests queue up and timeout
- **Impact**: Any concurrent load causes immediate pool exhaustion

### 2. **Idle Connections Timing Out**
- **Original**: `idle-timeout: 600000ms (10 minutes)`
- **Problem**: Database/firewall closes idle connections after 10 minutes, but HikariCP doesn't know they're dead
- **Solution**: Reduce idle timeout to 5 minutes (300000ms) to detect stale connections earlier

### 3. **No Connection Validation**
- **Problem**: Connections returning from idle state aren't tested for validity
- **Solution**: Add `connection-test-query: SELECT 1` to validate connections before use

### 4. **Connection Leaks Not Detected**
- **Problem**: No leak detection threshold configured
- **Solution**: Add `leak-detection-threshold: 120000ms` (2 minutes) to detect connections not being returned

---

## Solution Applied

### Configuration Changes in `application.yml`

```yaml
hikari:
  # Increased from 5 to 20 - allows more concurrent connections
  maximum-pool-size: 20
  
  # Increased from 1 to 5 - maintains idle connections for reuse
  minimum-idle: 5
  
  # Increased from 30s to 60s - more time to acquire connections
  connection-timeout: 60000
  
  # Reduced from 600s to 300s - close idle connections sooner
  idle-timeout: 300000
  
  # How long a connection can live (30 minutes)
  max-lifetime: 1800000
  
  # Test connection validity before returning from pool
  connection-test-query: SELECT 1
  
  # Detect connections not returned after 2 minutes
  leak-detection-threshold: 120000
  
  # Enable auto-commit
  auto-commit: true
```

### Production Configuration in `application-prod.yml`

```yaml
hikari:
  maximum-pool-size: 25        # Higher for production
  minimum-idle: 8              # Keep more idle connections ready
  connection-timeout: 60000
  idle-timeout: 300000
  max-lifetime: 1800000
  connection-init-sql: SELECT 1     # Test on initialization
  connection-test-query: SELECT 1   # Test before use
  auto-commit: true
  leak-detection-threshold: 120000
  validation-timeout: 5000     # Timeout for validation query
```

---

## Key Parameter Explanations

| Parameter | Value | Meaning |
|-----------|-------|---------|
| `maximum-pool-size` | 20/25 | Maximum connections to maintain |
| `minimum-idle` | 5/8 | Always keep this many idle, ready connections |
| `connection-timeout` | 60000ms | Wait up to 1 minute for a connection to become available |
| `idle-timeout` | 300000ms | Close idle connections after 5 minutes |
| `max-lifetime` | 1800000ms | Recycle connections after 30 minutes to prevent DB-side issues |
| `connection-test-query` | SELECT 1 | SQL to test connection validity |
| `leak-detection-threshold` | 120000ms | Log a warning if connection held > 2 minutes |
| `auto-commit` | true | Enable auto-commit for transaction handling |

---

## Benefits of This Fix

✅ **More Available Connections** - 20-25 pool size handles concurrent load  
✅ **Connection Validation** - Dead connections detected and refreshed  
✅ **Faster Idle Detection** - 5 min timeout prevents long-dead connections  
✅ **Leak Detection** - Identifies connections not being properly closed  
✅ **Production Ready** - Separate config with higher pool size for prod  

---

## Monitoring & Verification

### 1. Check Connection Pool Status
Add to your controller or service to monitor:
```java
@Autowired
private DataSource dataSource;

@GetMapping("/api/health/connections")
public Map<String, Object> checkConnections() {
    HikariDataSource hikariDs = (HikariDataSource) dataSource;
    return Map.of(
        "activeConnections", hikariDs.getHikariPoolMXBean().getActiveConnections(),
        "totalConnections", hikariDs.getHikariPoolMXBean().getTotalConnections(),
        "idleConnections", hikariDs.getHikariPoolMXBean().getIdleConnections(),
        "waitingForConnection", hikariDs.getHikariPoolMXBean().getWaitingForConnection()
    );
}
```

### 2. Enable Actuator Health
Already configured in your app. Check:
```
GET http://localhost:8080/api/actuator/health
```

### 3. Monitor Logs
Watch for these messages:
```
WARN  com.zaxxer.hikari.HikariConfig - HikariPool-1 - Connection has been open for [X]ms
DEBUG - Connection validation query executed
```

---

## Additional Recommendations

### 1. **Connection Pool Sizing**
- **Dev**: 10-15 connections
- **Staging**: 15-20 connections  
- **Production**: 20-30 connections (depends on load)

Formula: `(core_count * 2) + effective_spindle_count`
For 4-core: (4 × 2) + 1 = 9, round up to 10-15

### 2. **Database Side**
Ensure PostgreSQL also has proper timeout settings:
```sql
-- Check current settings
SHOW idle_in_transaction_session_timeout;
SHOW statement_timeout;

-- Set reasonable timeouts on DB side (if needed)
ALTER SYSTEM SET idle_in_transaction_session_timeout = '5min';
ALTER SYSTEM SET statement_timeout = '30s';
```

### 3. **Connection Usage Pattern**
Ensure connections are returned quickly:
```java
// ✅ GOOD - Try-with-resources (auto-closes)
try (Connection conn = dataSource.getConnection()) {
    // use connection
}

// ✅ GOOD - Spring Data JPA/Hibernate (auto-manages)
@Repository
public interface UserRepository extends JpaRepository<User, Long> {}

// ❌ BAD - Manual connection management
Connection conn = dataSource.getConnection();
// ... if not properly closed in finally block, causes leak
```

### 4. **Thread Pool Settings**
Check Tomcat thread pool if using embedded Tomcat:
```yaml
server:
  tomcat:
    threads:
      max: 200              # Max concurrent requests
      min-spare: 10         # Min idle threads
```

---

## Troubleshooting Checklist

If you still experience timeouts after this fix:

- [ ] Verify database is running and accessible: `psql -U user -d database -c "SELECT 1"`
- [ ] Check network connectivity and firewall rules
- [ ] Monitor database active connections: `SELECT count(*) FROM pg_stat_activity;`
- [ ] Check for slow queries causing long transaction times
- [ ] Enable HikariCP debug logging:
  ```yaml
  logging:
    level:
      com.zaxxer.hikari: DEBUG
  ```
- [ ] Increase pool size further if serving high traffic
- [ ] Check for connection leaks in custom code
- [ ] Ensure JPA entities use proper transaction management

---

## Files Modified

1. `/src/main/resources/application.yml` - Development config
2. `/src/main/resources/application-prod.yml` - Production config

## Testing the Fix

After applying changes:

1. **Restart the application**
2. **Run load tests**: Send multiple concurrent requests
3. **Monitor logs** for connection pool warnings
4. **Wait for idle period** (10+ minutes) then send new request
5. **Check metrics**: `GET /api/actuator/metrics`

Expected behavior:
- No "Connection is not available, request timed out" errors
- All requests get connections within timeout period
- Pool maintains idle connections for reuse

---

## References

- [HikariCP Configuration Wiki](https://github.com/brettwooldridge/HikariCP/wiki/Configuration)
- [PostgreSQL JDBC Connection Pooling](https://jdbc.postgresql.org/documentation/head/connect.html)
- [Spring Boot DataSource Configuration](https://spring.io/blog/2015/07/07/spring-boot-goes-ga)

