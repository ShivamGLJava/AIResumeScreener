# PostgreSQL Setup Guide for Resume Screener

## Prerequisites
- PostgreSQL installed and running
- PostgreSQL port 5432 is accessible
- `psql` command-line tool available

---

## Step 1: Verify PostgreSQL Installation

```bash
psql --version
```

Should show PostgreSQL version 9.6 or higher.

---

## Step 2: Create Database

### Option A: Using psql

```bash
# Connect to PostgreSQL
psql -U postgres

# Inside psql prompt, run:
CREATE DATABASE resumescreener;

# Verify creation
\l

# Exit
\q
```

### Option B: Using setup script

**On Linux/Mac:**
```bash
chmod +x backend/setup-db.sh
./backend/setup-db.sh
```

**On Windows (PowerShell):**
```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
.\backend\setup-db.bat
```

---

## Step 3: Set Environment Variables

You need to set these **before running the application**:

### On Windows (PowerShell)

```powershell
$env:DB_URL = "jdbc:postgresql://localhost:5432/resumescreener"
$env:DB_USERNAME = "postgres"
$env:DB_PASSWORD = "postgres"
$env:HF_API_KEY = "hf_your_actual_huggingface_api_key"
```

### On Windows (Command Prompt)

```cmd
set DB_URL=jdbc:postgresql://localhost:5432/resumescreener
set DB_USERNAME=postgres
set DB_PASSWORD=postgres
set HF_API_KEY=hf_your_actual_huggingface_api_key
```

### On Linux/Mac

```bash
export DB_URL="jdbc:postgresql://localhost:5432/resumescreener"
export DB_USERNAME="postgres"
export DB_PASSWORD="postgres"
export HF_API_KEY="hf_your_actual_huggingface_api_key"
```

---

## Step 4: Verify Environment Variables

### Windows (PowerShell)
```powershell
$env:DB_URL
$env:DB_USERNAME
$env:DB_PASSWORD
```

### Linux/Mac
```bash
echo $DB_URL
echo $DB_USERNAME
echo $DB_PASSWORD
```

---

## Step 5: Run the Application

```bash
cd backend
mvn clean package
```

Or to skip tests:
```bash
mvn clean package -DskipTests
```

---

## Step 6: Verify Database Connection

Once the application starts, check the logs for:

```
=== PRIVACY CHECK ===
Original resume length: XXXX
Cleaned resume length: XXXX
```

This indicates the database is connected and the application is running.

---

## Database Schema

The application uses Hibernate's `ddl-auto=update` setting, which means:

- Tables are **automatically created** on first run
- Tables are **automatically updated** if entity definitions change
- Existing data is **preserved**

**Main table created:**
```sql
CREATE TABLE resume_analysis (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    file_name VARCHAR(255) NOT NULL,
    job_description TEXT NOT NULL,
    match_score INTEGER NOT NULL,
    structured_analysis JSONB NOT NULL,
    final_result TEXT NOT NULL,
    hr_summary TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL
);
```

---

## Troubleshooting

### Error: "url must start with jdbc"
**Cause:** Environment variables not set or incorrect

**Fix:** 
1. Verify environment variables are set: `echo $DB_URL` (Linux/Mac) or `$env:DB_URL` (Windows)
2. Ensure URL format: `jdbc:postgresql://localhost:5432/resumescreener`
3. Set them again before running

### Error: "could not connect to server"
**Cause:** PostgreSQL not running or port 5432 not accessible

**Fix:**
1. Verify PostgreSQL is running
2. Check port 5432: `netstat -an | grep 5432` (Linux/Mac) or `netstat -an | find "5432"` (Windows)
3. Restart PostgreSQL if needed

### Error: "database resumescreener does not exist"
**Cause:** Database not created

**Fix:**
```bash
psql -U postgres -c "CREATE DATABASE resumescreener;"
```

### Error: "permission denied for database"
**Cause:** PostgreSQL user doesn't have proper permissions

**Fix:**
```bash
psql -U postgres -c "GRANT ALL PRIVILEGES ON DATABASE resumescreener TO postgres;"
```

---

## Connection String Reference

| Component | Value |
|-----------|-------|
| **Protocol** | `jdbc:postgresql://` |
| **Host** | `localhost` |
| **Port** | `5432` |
| **Database** | `resumescreener` |
| **Full URL** | `jdbc:postgresql://localhost:5432/resumescreener` |

---

## Environment Variables Checklist

- [ ] DB_URL = `jdbc:postgresql://localhost:5432/resumescreener`
- [ ] DB_USERNAME = `postgres` (or your PostgreSQL user)
- [ ] DB_PASSWORD = your PostgreSQL password
- [ ] HF_API_KEY = your HuggingFace API key
- [ ] Variables set **before** running Maven

---

## Next Steps

1. ✅ Set environment variables
2. ✅ Create database
3. ✅ Run `mvn clean package`
4. ✅ Application creates tables automatically
5. ✅ Start using the application

For API endpoints, see: `RESUME_SCREENER_API.md`

---

*Last Updated: 2026-05-26*
