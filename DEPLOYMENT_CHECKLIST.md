# Deployment Checklist for Render + Supabase

Use this checklist to ensure everything is configured correctly before deploying.

## Pre-Deployment Checklist

### ✅ Files Created/Updated

- [x] `.env.example` - Template for environment variables
- [x] `application-production.properties` - Production Spring profile
- [x] `application-docker.properties` - Updated to support environment variables
- [x] `docker-compose.yml` - Updated to support environment variable overrides
- [x] `RENDER_DEPLOYMENT.md` - Complete deployment guide
- [x] `.gitignore` - Updated to exclude `.env` files
- [x] `.dockerignore` - Updated to allow `.env.example`

### ✅ Configuration Verification

- [x] **Dockerfile** - Multi-stage build, production-ready
- [x] **Port Configuration** - Uses `${PORT:8080}` in production (Render provides PORT)
- [x] **Database Configuration** - All credentials via environment variables
- [x] **SSL Support** - Production config includes `sslmode=require` for Supabase
- [x] **No Hardcoded Credentials** - All sensitive data in environment variables

## Supabase Setup

### Required Steps

1. [ ] Create Supabase project
2. [ ] Get database connection details:
   - Host: `db.xxxxx.supabase.co`
   - Port: `5432`
   - Database: `postgres`
   - Username: `postgres` (or check in settings)
   - Password: (the one you set during project creation)
3. [ ] Convert connection string to JDBC format:
   ```
   jdbc:postgresql://db.xxxxx.supabase.co:5432/postgres?sslmode=require
   ```

## Render Setup

### Required Steps

1. [ ] Create Render account
2. [ ] Create new Web Service
3. [ ] Connect Git repository
4. [ ] Configure build settings:
   - Environment: `Docker`
   - Dockerfile Path: `Dockerfile`
   - Build Context: `.`
5. [ ] Set environment variables in Render dashboard:

#### Required Environment Variables

```
SPRING_PROFILES_ACTIVE=production
SPRING_DATASOURCE_URL=jdbc:postgresql://db.xxxxx.supabase.co:5432/postgres?sslmode=require
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=your-supabase-password
SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.postgresql.Driver
```

#### Optional Environment Variables

```
SPRING_JPA_HIBERNATE_DDL_AUTO=update
SPRING_JPA_SHOW_SQL=false
```

**Note:** Render automatically sets `PORT` - no need to set it manually.

6. [ ] Deploy and monitor logs
7. [ ] Test the application:
   - [ ] Register a new user
   - [ ] Create a group
   - [ ] Add an expense
   - [ ] View dashboard
   - [ ] Record a settlement

## Post-Deployment Verification

### Application Health

- [ ] Application starts without errors
- [ ] Database connection successful (check logs)
- [ ] No SSL/connection errors
- [ ] Application accessible at Render URL

### Functionality Tests

- [ ] User registration works
- [ ] User login works (with email)
- [ ] Group creation works
- [ ] Expense creation works
- [ ] Dashboard loads with balances
- [ ] Settlements can be recorded
- [ ] Theme toggle works (light/dark mode)

### Database Verification

- [ ] Tables created automatically (if `ddl-auto=update`)
- [ ] Data persists between deployments
- [ ] No foreign key constraint errors

## Troubleshooting

### Common Issues

1. **Database Connection Failed**
   - Verify Supabase host is correct
   - Check that `sslmode=require` is in JDBC URL
   - Verify username and password are correct
   - Ensure Supabase project is not paused

2. **Build Fails**
   - Check Dockerfile is in root directory
   - Verify Maven can download dependencies
   - Check build logs for specific errors

3. **Application Won't Start**
   - Check environment variables are set correctly
   - Verify Spring profile is `production`
   - Review application logs in Render dashboard

4. **Port Errors**
   - Render handles port automatically via `PORT` env var
   - Don't hardcode ports in configuration

## Security Reminders

- [ ] Never commit `.env` files (already in `.gitignore`)
- [ ] Use strong passwords for Supabase
- [ ] Rotate credentials periodically
- [ ] Enable SSL (`sslmode=require`) for all Supabase connections
- [ ] Review Render logs regularly for suspicious activity

## Support Resources

- **Render Docs**: https://render.com/docs
- **Supabase Docs**: https://supabase.com/docs
- **Deployment Guide**: See `RENDER_DEPLOYMENT.md` for detailed instructions

