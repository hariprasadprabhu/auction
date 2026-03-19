# Railway PostgreSQL Connection Fix

## Problem
Your application is failing to connect to the Railway PostgreSQL database with this error:
```
java.net.UnknownHostException: postgres:KqhsxlBWpagkSSMaTLqZUgqRDGoSFaEf@postgres.railway.internal
```

This occurs because the `.env` file still contained old Neon database credentials, causing a malformed connection string.

## Solution

### Step 1: Get Your Railway PostgreSQL Credentials

1. Go to your **Railway Dashboard**: https://railway.app
2. Open your **Auction project**
3. Click on the **PostgreSQL** plugin/service
4. Go to the **Connect** tab or **Variables** section
5. You should see connection details. Copy the following information:

   - **PGHOST**: The database hostname (e.g., `postgres.railway.internal` for internal or a public host)
   - **PGPORT**: Usually `5432`
   - **PGDATABASE**: Database name (e.g., `railway`)
   - **PGUSER**: Username (e.g., `postgres`)
   - **PGPASSWORD**: Your password

### Step 2: Update Your `.env` File

Edit `/home/hari/proj/auction/.env` and ensure it looks like this:

```dotenv
SPRING_PROFILES_ACTIVE=prod

# ── Railway PostgreSQL DB ─────────────────────────────────────────────────────
# Format: jdbc:postgresql://<host>:<port>/<database>
DB_URL=jdbc:postgresql://postgres.railway.internal:5432/railway
DB_USERNAME=postgres
DB_PASSWORD=<YOUR_ACTUAL_RAILWAY_PASSWORD>

JWT_SECRET=YourSuperSecretKeyHereMustBeAtLeast256BitsLongForHmacSha256!!

CORS_ALLOWED_ORIGINS=http://localhost:4200
```

**Important**: Replace `<YOUR_ACTUAL_RAILWAY_PASSWORD>` with your actual Railway database password.

### Step 3: Verify Connection Format

The correct JDBC URL format for Railway PostgreSQL is:
```
jdbc:postgresql://<HOST>:<PORT>/<DATABASE>
```

**Examples:**

**Internal Connection (within Railway):**
```
jdbc:postgresql://postgres.railway.internal:5432/railway
```

**Public Connection (from outside Railway):**
```
jdbc:postgresql://your-railway-host.railway.app:5432/railway
```

### Step 4: Rebuild and Deploy

After updating `.env`, rebuild your Docker image:

```bash
# Rebuild the Docker image
docker build -t auction:latest .

# Stop old container
docker stop auction

# Run new container
docker run -d --name auction --env-file .env -p 8080:8080 auction:latest
```

Or if using Docker Compose:

```bash
docker-compose down
docker-compose up -d --build
```

### Step 5: Verify Connection

Check the logs to see if the connection succeeds:

```bash
docker logs auction -f
```

Look for these success indicators:
```
HikariPool-1 - Starting...
[Pool stats: size = 1, connections = 1 ...]
Hibernate ORM core version 6.6.13.Final
```

## Connection String Components Explained

| Component | Example | Description |
|-----------|---------|-------------|
| `postgres.railway.internal` | hostname | Railway internal DNS (only works within Railway) |
| `5432` | port | PostgreSQL default port |
| `railway` | database | Database name |
| `postgres` | username | Database user |
| `password` | password | Database password |

## Troubleshooting

### If you see "UnknownHostException: postgres:KqhsxlBWpagkSSMaTLqZUgqRDGoSFaEf@..."
- The connection string is being parsed incorrectly
- Check that your `DB_URL` is in the correct format
- Verify you're not mixing username:password into the hostname

### If you see "FATAL: password authentication failed"
- Your `DB_PASSWORD` is incorrect or has special characters
- Double-check the password from Railway dashboard
- If the password has special characters, it may need URL encoding

### If you see "Database does not exist" error
- Verify the `DB_URL` has the correct database name
- Check that the database exists in Railway PostgreSQL service

### If using public Railway host (not internal)
- Get the public host from Railway: Dashboard → PostgreSQL → Connect → Public URL
- Update your connection string to use the public host instead of `postgres.railway.internal`
- This may affect latency/performance compared to internal connection

## Additional Notes

- The `.env` file is already added to `.gitignore`, so it won't be committed to version control ✓
- For production on Railway, these variables should be set in Railway's project environment variables
- Internal Railway connections (`postgres.railway.internal`) are faster but only work within the Railway network
- Public connections work from anywhere but may have network overhead

## Need Help?

If you're still having issues:

1. **Verify credentials** one more time from Railway dashboard
2. **Check format** matches exactly: `jdbc:postgresql://host:port/database`
3. **Test locally** before deploying to Railway
4. **Check Railway logs** in the Railway dashboard for any errors on the database side

---

**Last Updated**: 2026-03-19
**Status**: Database configuration migrated from Neon to Railway PostgreSQL

