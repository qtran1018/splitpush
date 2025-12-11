# Deploying Splitpush to Render with Supabase

This guide walks you through deploying the Splitpush application to Render using Docker, with Supabase as the PostgreSQL database provider.

## Prerequisites

- A [Render](https://render.com) account
- A [Supabase](https://supabase.com) account
- Git repository with your Splitpush code

## Step 1: Set Up Supabase Database

### 1.1 Create a Supabase Project

1. Go to [Supabase Dashboard](https://app.supabase.com)
2. Click "New Project"
3. Fill in:
   - **Name**: Your project name (e.g., "splitpush")
   - **Database Password**: Choose a strong password (save this!)
   - **Region**: Choose the closest region to your Render deployment
4. Click "Create new project"
5. Wait for the project to be provisioned (2-3 minutes)

### 1.2 Get Database Connection Details

1. In your Supabase project dashboard, go to **Settings** → **Database**
2. Scroll down to **Connection string** section
3. Select **URI** tab
4. Copy the connection string (format: `postgresql://postgres:[YOUR-PASSWORD]@db.xxxxx.supabase.co:5432/postgres`)

### 1.3 Convert to JDBC Format

Supabase provides a URI format, but Spring Boot needs JDBC format. Extract the following:

- **Host**: `db.xxxxx.supabase.co` (from the connection string)
- **Port**: `5432` (default)
- **Database**: `postgres` (default)
- **Username**: `postgres` (default, or check in Supabase settings)
- **Password**: The password you set during project creation

**JDBC URL format:**
```
jdbc:postgresql://[host]:[port]/[database]?sslmode=require
```

**Example:**
```
jdbc:postgresql://db.abcdefghijklmnop.supabase.co:5432/postgres?sslmode=require
```

**Note:** The `?sslmode=require` parameter is important for Supabase connections.

## Step 2: Deploy to Render

### 2.1 Create a New Web Service on Render

1. Go to [Render Dashboard](https://dashboard.render.com)
2. Click "New +" → "Web Service"
3. Connect your Git repository (GitHub, GitLab, or Bitbucket)
4. Select your Splitpush repository

### 2.2 Configure Build Settings

- **Name**: `splitpush` (or your preferred name)
- **Environment**: `Docker`
- **Region**: Choose the same region as your Supabase project (for lower latency)
- **Branch**: `main` (or your default branch)
- **Root Directory**: Leave empty (or specify if your app is in a subdirectory)

### 2.3 Set Environment Variables

In the Render dashboard, add the following environment variables:

#### Required Database Variables

```
SPRING_PROFILES_ACTIVE=production
SPRING_DATASOURCE_URL=jdbc:postgresql://db.xxxxx.supabase.co:5432/postgres?sslmode=require
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=your-supabase-password
SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.postgresql.Driver
```

**Replace:**
- `db.xxxxx.supabase.co` with your actual Supabase host
- `your-supabase-password` with your Supabase database password

#### Optional Configuration Variables

```
SPRING_JPA_HIBERNATE_DDL_AUTO=update
SPRING_JPA_SHOW_SQL=false
SERVER_PORT=8080
```

**Note:** Render automatically sets the `PORT` environment variable. The application will use it if available, otherwise defaults to 8080.

### 2.4 Configure Advanced Settings

- **Dockerfile Path**: `Dockerfile` (should be in root)
- **Docker Build Context**: `.` (root directory)
- **Health Check Path**: `/` (or `/login` if you prefer)
- **Auto-Deploy**: Enable if you want automatic deployments on push

### 2.5 Deploy

1. Click "Create Web Service"
2. Render will:
   - Build your Docker image
   - Start the container
   - Run health checks
3. Monitor the build logs for any errors
4. Once deployed, your app will be available at `https://your-app-name.onrender.com`

## Step 3: Verify Deployment

### 3.1 Check Application Logs

1. In Render dashboard, go to your service
2. Click "Logs" tab
3. Look for:
   - Successful database connection messages
   - Application startup completion
   - No connection errors

### 3.2 Test the Application

1. Visit your Render URL: `https://your-app-name.onrender.com`
2. Try to:
   - Register a new user
   - Create a group
   - Add an expense
   - View the dashboard

### 3.3 Verify Database Connection

If you see database connection errors:

1. Double-check your Supabase connection details
2. Verify the JDBC URL includes `?sslmode=require`
3. Check that your Supabase project is active
4. Ensure your Supabase database password is correct

## Step 4: Post-Deployment Configuration

### 4.1 Database Schema

On first deployment, the application will automatically create tables if `SPRING_JPA_HIBERNATE_DDL_AUTO=update` is set.

**For production, consider:**
- Setting `SPRING_JPA_HIBERNATE_DDL_AUTO=validate` after initial setup
- Running database migrations manually for better control

### 4.2 Custom Domain (Optional)

1. In Render dashboard, go to your service
2. Click "Settings" → "Custom Domain"
3. Add your domain
4. Follow DNS configuration instructions

### 4.3 Environment-Specific Settings

You can create different environment variables for:
- **Staging**: Use a separate Supabase project
- **Production**: Use your main Supabase project

## Troubleshooting

### Build Fails

- **Issue**: Maven dependency download fails
  - **Solution**: Check network connectivity in build logs. The Dockerfile includes retry logic.

- **Issue**: Docker build context issues
  - **Solution**: Ensure `Dockerfile` is in the root directory

### Application Won't Start

- **Issue**: Database connection timeout
  - **Solution**: 
    - Verify Supabase host is correct
    - Check that `sslmode=require` is in the JDBC URL
    - Ensure Supabase project is not paused

- **Issue**: Port binding errors
  - **Solution**: Render handles port mapping automatically. Don't hardcode ports.

### Database Connection Errors

- **Issue**: "Connection refused" or "Host not found"
  - **Solution**: 
    - Verify Supabase hostname is correct
    - Check Supabase project status
    - Ensure you're using the correct region

- **Issue**: "SSL required" errors
  - **Solution**: Add `?sslmode=require` to your JDBC URL

## Environment Variables Reference

### Required for Production

| Variable | Description | Example |
|----------|-------------|---------|
| `SPRING_PROFILES_ACTIVE` | Spring profile to use | `production` |
| `SPRING_DATASOURCE_URL` | Full JDBC connection URL | `jdbc:postgresql://db.xxx.supabase.co:5432/postgres?sslmode=require` |
| `SPRING_DATASOURCE_USERNAME` | Database username | `postgres` |
| `SPRING_DATASOURCE_PASSWORD` | Database password | `your-password` |

### Optional

| Variable | Description | Default |
|----------|-------------|---------|
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | DDL auto mode | `validate` |
| `SPRING_JPA_SHOW_SQL` | Show SQL queries | `false` |
| `SERVER_PORT` | Server port | `8080` (Render uses `PORT`) |

## Local Development with Supabase

You can also use Supabase for local development:

1. Create a `.env` file from `.env.example`
2. Fill in your Supabase credentials
3. Run: `docker-compose up` (without the postgres service, or override env vars)

Or set environment variables directly:
```bash
export SPRING_PROFILES_ACTIVE=production
export SPRING_DATASOURCE_URL=jdbc:postgresql://db.xxx.supabase.co:5432/postgres?sslmode=require
export SPRING_DATASOURCE_USERNAME=postgres
export SPRING_DATASOURCE_PASSWORD=your-password
mvn spring-boot:run
```

## Security Best Practices

1. **Never commit `.env` files** - Already in `.gitignore`
2. **Use strong database passwords** - Generate secure passwords for Supabase
3. **Enable SSL** - Always use `sslmode=require` for Supabase
4. **Rotate credentials** - Periodically update database passwords
5. **Monitor logs** - Regularly check Render logs for suspicious activity
6. **Use environment variables** - Never hardcode credentials in code

## Support

- [Render Documentation](https://render.com/docs)
- [Supabase Documentation](https://supabase.com/docs)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)


