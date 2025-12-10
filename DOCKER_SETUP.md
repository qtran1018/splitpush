# Docker PostgreSQL Setup

## Starting the PostgreSQL Container

1. **Start the PostgreSQL container:**
   ```bash
   docker-compose up -d
   ```

2. **Verify the container is running:**
   ```bash
   docker-compose ps
   ```

3. **View logs (optional):**
   ```bash
   docker-compose logs -f postgres
   ```

4. **Stop the container:**
   ```bash
   docker-compose down
   ```

5. **Stop and remove volumes (deletes all data):**
   ```bash
   docker-compose down -v
   ```

## Database Connection Details

- **Host:** localhost
- **Port:** 5432
- **Database:** splitpush
- **Username:** splitpush_user
- **Password:** splitpush_pass

## Running the Application

After starting the PostgreSQL container, run the Spring Boot application:

```bash
mvn spring-boot:run
```

The application will automatically:
- Connect to the PostgreSQL database
- Create/update the database schema on first startup
- Persist all data between application restarts

## Data Persistence

The PostgreSQL data is stored in a Docker volume named `postgres_data`. This means:
- Data persists even if you stop the container
- Data is removed only if you use `docker-compose down -v`
- The volume can be backed up or migrated

## Troubleshooting

If you get connection errors:
1. Make sure Docker is running
2. Verify the container is up: `docker-compose ps`
3. Check container logs: `docker-compose logs postgres`
4. Ensure port 5432 is not already in use

