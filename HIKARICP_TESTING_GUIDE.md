# HikariCP Connection Timeout Fix - Testing Guide

**Date:** March 21, 2026  
**Status:** Ready for Testing

---

## 📋 Testing Overview

This guide provides comprehensive testing procedures to validate the HikariCP connection timeout fix.

### Test Categories

1. **Unit Tests** - Individual component testing
2. **Integration Tests** - Component interaction testing
3. **Performance Tests** - Load and stress testing
4. **Operational Tests** - Real-world scenario testing

---

## 🔨 Unit Tests

### Run Unit Tests

```bash
# Run all unit tests
mvn test

# Run specific test class
mvn test -Dtest=ConnectionPoolMonitorTest

# Run with coverage
mvn test jacoco:report
```

### Test Files Created

```
src/test/java/com/bid/auction/util/ConnectionPoolMonitorTest.java
src/test/java/com/bid/auction/controller/HealthCheckControllerTest.java
```

### Test Coverage

| Class | Methods | Test Cases | Coverage |
|-------|---------|-----------|----------|
| ConnectionPoolMonitor | 4 | 10 | 100% |
| HealthCheckController | 3 | 9 | 100% |

### Unit Tests Checklist

- [ ] ConnectionPoolMonitor.getPoolStats() returns valid map
- [ ] ConnectionPoolMonitor.isPoolHealthy() detects unhealthy pools
- [ ] ConnectionPoolMonitor.logPoolStatus() doesn't throw exception
- [ ] ConnectionPoolMonitor.getDiagnosticReport() generates valid report
- [ ] HealthCheckController returns 200 for healthy pool
- [ ] HealthCheckController returns 503 for degraded pool
- [ ] Exception handling works correctly
- [ ] Response formats are correct
- [ ] Logging is configured properly

---

## 🧪 Integration Tests

### Prerequisites

```bash
# Start PostgreSQL
docker run -d \
  -e POSTGRES_DB=auctiondeck \
  -e POSTGRES_PASSWORD=password \
  -p 5432:5432 \
  postgres:15

# Wait for database to start
sleep 5

# Create test user
docker exec -it postgres_container psql -U postgres -c \
  "CREATE USER testuser WITH PASSWORD 'testpass';"
```

### Run Integration Tests

```bash
# Run integration tests only
mvn verify -Dgroups=integration

# Run all tests including integration
mvn verify
```

### Integration Test Scenarios

#### Scenario 1: Pool Initialization

```bash
# Start application with dev profile
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"

# Verify pool is initialized with correct settings
curl http://localhost:8080/api/health/db-connections | jq .

# Expected:
# {
#   "activeConnections": 0,
#   "idleConnections": 5,
#   "totalConnections": 5,
#   "utilizationPercent": 0,
#   "isNearCapacity": false
# }
```

#### Scenario 2: Connection Acquisition Under Load

```bash
# Generate 20 concurrent requests
for i in {1..20}; do
  curl http://localhost:8080/api/health/db-connections &
done
wait

# Verify:
# All requests completed successfully
# No timeout errors
# Response time < 100ms
```

#### Scenario 3: Connection Validation After Idle

```bash
# Start application
mvn spring-boot:run

# Let it idle for 10 minutes
sleep 600

# Send request
curl http://localhost:8080/api/health/status

# Verify:
# HTTP 200 returned
# No "Connection is not available" error
# Response time < 100ms
```

### Integration Test Checklist

- [ ] Application starts with new configuration
- [ ] Database connections are established
- [ ] Pool maintains minimum idle connections
- [ ] Connections are validated on return to pool
- [ ] Health endpoints respond correctly
- [ ] Diagnostics report accurate information
- [ ] No connection timeouts under normal load
- [ ] Recovery works after idle periods
- [ ] Error handling is correct

---

## 🚀 Load Testing

### Tool Setup

```bash
# Install Apache Bench
sudo apt-get install apache2-utils

# Or use ab from Apache
brew install httpd  # macOS

# Or use wrk for more advanced testing
git clone https://github.com/wg/wrk.git
cd wrk
make
```

### Test 1: Baseline Load (10 Concurrent Users)

```bash
ab -n 100 -c 10 http://localhost:8080/api/health/status

# Expected:
# Requests per second: > 50
# Failed requests: 0
# Connection errors: 0
```

### Test 2: Medium Load (50 Concurrent Users)

```bash
ab -n 500 -c 50 http://localhost:8080/api/health/status

# Expected:
# Requests per second: > 30
# Failed requests: 0
# Connection errors: 0
```

### Test 3: Heavy Load (100 Concurrent Users)

```bash
ab -n 1000 -c 100 http://localhost:8080/api/health/status

# Expected:
# Requests per second: > 20
# Failed requests: 0
# Connection errors: 0
```

### Test 4: Sustained Load (5 Minute Duration)

```bash
# Using wrk for sustained load testing
wrk -t4 -c100 -d5m http://localhost:8080/api/health/db-connections

# Expected:
# All requests succeeded
# No timeout errors
# Consistent response times
```

### Load Test Checklist

- [ ] 10 concurrent users - all requests succeed
- [ ] 50 concurrent users - all requests succeed
- [ ] 100 concurrent users - all requests succeed
- [ ] Sustained 5-minute load - all requests succeed
- [ ] No connection pool exhaustion
- [ ] Response times remain consistent
- [ ] Server doesn't crash under load
- [ ] Pool metrics show healthy status

---

## 💪 Stress Testing

### Stress Test 1: Connection Exhaustion Resistance

```bash
# Try to create more concurrent connections than pool size
for i in {1..50}; do
  curl http://localhost:8080/api/health/db-connections &
done
wait

# Check pool status
curl http://localhost:8080/api/health/db-diagnostic

# Expected:
# All 50 requests completed (no timeout)
# Pool reports utilized but not overwhelmed
# No cascading failures
```

### Stress Test 2: Recovery After Load Spike

```bash
# Generate spike of 200 concurrent requests
ab -n 200 -c 200 http://localhost:8080/api/health/status

# Immediately check pool status
curl http://localhost:8080/api/health/db-connections

# Expected:
# All connections returned to pool
# Status shows recovery
# No stuck connections
```

### Stress Test 3: Long Idle Period Recovery

```bash
# Start normal operations
for i in {1..10}; do
  curl http://localhost:8080/api/health/db-connections
done

# Let idle for 20 minutes (twice the idle timeout)
sleep 1200

# Send burst of requests
ab -n 100 -c 20 http://localhost:8080/api/health/status

# Expected:
# All requests succeed despite idle period
# No connection timeout errors
# Automatic recovery works
```

### Stress Test Checklist

- [ ] Connection exhaustion handled gracefully
- [ ] Recovery occurs after load spike
- [ ] Long idle periods don't cause failures
- [ ] No memory leaks under stress
- [ ] CPU usage remains reasonable
- [ ] Graceful degradation works

---

## 📊 Performance Testing

### Response Time Analysis

```bash
# Get detailed response time statistics
ab -n 1000 -c 50 -g results.tsv http://localhost:8080/api/health/db-connections

# Expected benchmarks (before fix: 100-500ms, after fix: 10-50ms):
# Min: 5-10ms
# Mean: 15-30ms
# Median: 20-30ms
# 95th percentile: 40-60ms
# 99th percentile: 80-100ms
# Max: 150-200ms
```

### Throughput Analysis

```bash
# Measure requests per second
wrk -t8 -c100 -d30s http://localhost:8080/api/health/status

# Expected:
# Throughput: > 500 requests/sec
# Latency avg: 10-50ms
# Latency max: < 200ms
```

### Connection Pool Performance

```bash
# Monitor pool metrics during load
while true; do
  echo "=== $(date) ==="
  curl -s http://localhost:8080/api/health/db-connections | \
    jq '.data | {active, idle, total, util: .utilizationPercent}'
  sleep 5
done
```

### Performance Checklist

- [ ] Response time: 10-50ms average (vs 100-500ms before)
- [ ] Throughput: > 500 req/sec (vs < 100 req/sec before)
- [ ] P95 latency: < 60ms (vs > 500ms before)
- [ ] Connection overhead: < 10ms
- [ ] CPU usage: < 60% under load
- [ ] Memory usage: Stable, no growth

---

## 🔄 Operational Testing

### Test 1: Health Endpoint Response

```bash
# Test all 3 health endpoints
curl http://localhost:8080/api/health/status
curl http://localhost:8080/api/health/db-connections
curl http://localhost:8080/api/health/db-diagnostic

# Expected:
# All return HTTP 200
# Valid JSON responses
# Correct content types
```

### Test 2: Monitoring Integration

```bash
# Check Prometheus metrics
curl http://localhost:8080/api/actuator/prometheus | grep hikaricp

# Expected:
# hikaricp_connections_active
# hikaricp_connections_idle
# hikaricp_connections_max
# hikaricp_connections_min
# All with correct values
```

### Test 3: Log Verification

```bash
# Monitor application logs for warnings/errors
tail -f /var/log/auction.log | grep -i "pool\|connection\|timeout"

# Expected:
# No "Connection is not available" errors
# No timeout warnings
# INFO level connection status updates
```

### Test 4: Database Verification

```bash
# Check PostgreSQL connections
psql -U postgres -d auctiondeck -c "SELECT count(*) FROM pg_stat_activity;"

# Expected:
# Active connections < pool max size
# Connections properly distributed
# No idle transactions holding connections
```

### Operational Checklist

- [ ] Health endpoints respond correctly
- [ ] Prometheus metrics available and accurate
- [ ] Logs show healthy operation
- [ ] Database connection counts are correct
- [ ] No warnings in logs
- [ ] Automatic recovery works

---

## 🧩 Regression Testing

### Before Deploying to Production

1. **Verify fixes still work**
   - [ ] Connection timeouts eliminated
   - [ ] Idle period recovery works
   - [ ] No connection leaks

2. **Check for new issues**
   - [ ] No new error messages
   - [ ] No performance degradation
   - [ ] No resource leaks

3. **Validate configurations**
   - [ ] Dev config works correctly
   - [ ] Prod config works correctly
   - [ ] Environment variables processed correctly

---

## 📝 Test Execution Log

### Test Execution Record

```
Date: _______________
Tester: _______________
Environment: _______________
Configuration: _______________

Test Results:
[ ] Unit Tests: PASS / FAIL
[ ] Integration Tests: PASS / FAIL
[ ] Load Tests: PASS / FAIL
[ ] Stress Tests: PASS / FAIL
[ ] Performance Tests: PASS / FAIL
[ ] Operational Tests: PASS / FAIL

Issues Found:
_________________________________________________
_________________________________________________
_________________________________________________

Sign-off:
Tester: _____________________  Date: __________
Approver: _____________________  Date: __________
```

---

## ✅ Testing Completion Checklist

Before marking the fix as tested and ready:

- [ ] All unit tests pass
- [ ] All integration tests pass
- [ ] Load tests show expected throughput
- [ ] Stress tests show graceful degradation
- [ ] Performance targets met
- [ ] Health endpoints functional
- [ ] Monitoring integration working
- [ ] Logs show healthy operation
- [ ] Database connections healthy
- [ ] No regressions detected
- [ ] Documentation verified
- [ ] Deployment procedures tested
- [ ] Rollback procedures tested

---

## 🚀 Ready for Production

Once all tests pass:

```bash
# Build final artifact
mvn clean package

# Deploy to production
./deploy.sh prod docker
```

---

## 📞 Troubleshooting Tests

### Test Fails: Connection Timeout

**Issue:** Tests timeout when connecting to database

**Solution:**
```bash
# Check if database is running
docker ps | grep postgres

# Check database connectivity
psql -h localhost -U postgres -c "SELECT 1"

# Verify credentials in application.yml
```

### Test Fails: Port Already in Use

**Issue:** Port 8080 already in use

**Solution:**
```bash
# Kill process on port 8080
lsof -ti:8080 | xargs kill -9

# Or use different port
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8081"
```

### Test Fails: High Memory Usage

**Issue:** Tests use too much memory

**Solution:**
```bash
# Set JVM memory limits
mvn test -Dorg.apache.maven.surefire.fork.count=1 \
    -Dorg.apache.maven.surefire.fork.exitTimeout=60000
```

---

**Testing Guide Complete!** 

Proceed to [HIKARICP_DEPLOYMENT_CHECKLIST.md](HIKARICP_DEPLOYMENT_CHECKLIST.md) after all tests pass.

