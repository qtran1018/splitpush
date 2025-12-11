# Splitpush - Expense Splitting Application

A Spring Boot web application similar to Splitwise where users can create accounts, form trip groups, and split expenses among group members.

## Features

- **User Authentication**: Register and login with email and secure password hashing
- **Trip Groups**: Create and manage trip groups with multiple members
- **Expense Management**: Add, edit, and delete expenses with flexible splitting options
- **Balance Dashboard**: View net amounts owed to/from other users with detailed group breakdowns
- **Settlement Tracking**: Record settlements between users with automatic validation
- **Expense Tracking**: See all expenses in a group with pagination and detailed participant information
- **Light/Dark Mode**: Modern theme toggle with persistent user preference
- **Backend Caching**: Optimized performance with Caffeine cache to reduce database queries
- **Group Management**: Join groups via group ID, leave groups (with balance validation), automatic cleanup of empty groups

## Technology Stack

- **Backend**: Spring Boot 3.2.0, Spring Security, Spring Data JPA, Spring Cache
- **Database**: PostgreSQL (via Docker)
- **Caching**: Caffeine cache for improved performance
- **Frontend**: HTML, CSS (with CSS variables for theming), JavaScript, Thymeleaf
- **Build Tool**: Maven

## Prerequisites

- **Docker** and **Docker Compose** (recommended for easy setup)
- Or alternatively: Java 17+ and Maven 3.6+ for local development

## Getting Started

### Option 1: Docker Compose (Recommended)

The easiest way to run the application is using Docker Compose, which will build and run both the database and the application together.

1. **Clone or navigate to the project directory**
   ```bash
   cd Splitpush
   ```

2. **Build and start all services**
   ```bash
   docker-compose up -d --build
   ```
   
   **Note**: If you encounter network/DNS issues during the build (Maven can't download dependencies), try:
   
   **Option A: Build with host network (Linux/Mac)**
   ```bash
   docker-compose build --network=host
   docker-compose up -d
   ```
   
   **Option B: Configure Docker DNS**
   Create/edit `~/.docker/daemon.json` (or Docker Desktop settings) and add:
   ```json
   {
     "dns": ["8.8.8.8", "8.8.4.4"]
   }
   ```
   
   This will:
   - Build the Spring Boot application Docker image
   - Start PostgreSQL database
   - Start the application
   - Wait for the database to be healthy before starting the app

3. **View logs**
   ```bash
   # View all logs
   docker-compose logs -f
   
   # View app logs only
   docker-compose logs -f app
   
   # View database logs only
   docker-compose logs -f postgres
   ```

4. **Access the application**
   - Open your browser and navigate to: http://localhost:8080
   - The database is available at `localhost:5432`

5. **Stop the services**
   ```bash
   docker-compose down
   ```

6. **Stop and remove all data**
   ```bash
   docker-compose down -v
   ```

### Option 2: Local Development

For local development without Docker:

1. **Start the database only**
   ```bash
   docker-compose up -d postgres
   ```

2. **Build the project**
   ```bash
   mvn clean install
   ```

3. **Run the application**
   ```bash
   mvn spring-boot:run
   ```
   
   Or run the main class `SplitpushApplication` from your IDE.

4. **Access the application**
   - Open your browser and navigate to: http://localhost:8080

The application will automatically create the database schema on first startup.

### Troubleshooting Docker Build Issues

If you encounter network connectivity issues during Docker build:

1. **Check Docker DNS settings** - Configure DNS in Docker Desktop settings or `~/.docker/daemon.json`
2. **Try building with host network** (Linux/Mac): `docker-compose build --network=host`
3. **Pre-build JAR locally** - Build with `mvn clean package -DskipTests` first, then rebuild Docker image
4. **Check network connectivity** - Ensure Docker can reach Maven Central repositories

## Usage

### 1. Register a New Account
- Navigate to http://localhost:8080/register
- Fill in your username, email, name, and password
- Click "Register"

### 2. Login
- Use your **email** and password to login.

### 3. Create a Trip Group
- Click on "Groups" in the navigation
- Click "Create Group"
- Enter group name and optional description
- The creator is automatically added as a member
- Copy the Group ID to share with others

### 4. Join a Group
- Use the Group ID to join a group via the join functionality
- Or be added by an existing group member

### 5. Add Expenses
- Click on a group to view/add expenses
- Click "Add Expense"
- Enter description, amount, who paid, and split among participants
- Use "Split Equally" button for equal distribution
- Use "Select All" or "Deselect All" to quickly manage participants
- Or manually enter amounts for each participant
- Edit or delete expenses as needed

### 6. View Dashboard
- Navigate to Dashboard to see your net balances
- Positive amounts = others owe you
- Negative amounts = you owe others
- View detailed breakdowns by group with links to group expense pages
- Record settlements directly from the dashboard

### 7. Record Settlements
- From the Dashboard, click "Record Settlement" next to a user
- Select the group and enter the settlement amount
- System validates that you can only record settlements for amounts you owe
- Settlement amounts cannot exceed what is owed

### 8. Leave a Group
- From the Groups page, click "Leave Group" on any group
- You cannot leave a group if you have outstanding balances
- If you're the last member and have no balances, the group will be automatically deleted

### 9. Theme Toggle
- Click the theme toggle button (🌙/☀️) in the top right corner
- Your preference is saved and persists across sessions

## API Endpoints

### Authentication
- `POST /api/auth/register` - Register a new user
- `POST /api/auth/login` - Login (form-based, uses email)
- `POST /api/auth/logout` - Logout

### Users
- `GET /api/users/me` - Get current user information
- `GET /api/users/search?query={username}` - Search users by username

### Groups
- `GET /api/groups` - Get all groups for current user
- `GET /api/groups/{id}` - Get group by ID
- `POST /api/groups` - Create a new group
- `PUT /api/groups/{groupId}` - Update group (name, description)
- `POST /api/groups/{groupId}/members` - Add member to group
- `POST /api/groups/{groupId}/join` - Join a group by ID
- `POST /api/groups/{groupId}/leave` - Leave a group (validates no outstanding balances)
- `DELETE /api/groups/{groupId}/members/{userId}` - Remove member from group

### Expenses
- `GET /api/expenses/group/{groupId}?page={page}&size={size}` - Get paginated expenses for a group
- `GET /api/expenses/{id}` - Get expense by ID
- `POST /api/expenses` - Create a new expense
- `PUT /api/expenses/{id}` - Update an expense
- `DELETE /api/expenses/{id}` - Delete an expense
- `POST /api/expenses/{expenseId}/participants/{userId}/pay` - Mark expense participant as paid

### Settlements
- `GET /api/settlements` - Get all settlements for current user
- `GET /api/settlements/group/{groupId}` - Get settlements for a specific group
- `POST /api/settlements` - Record a new settlement (validates amount and membership)

### Dashboard
- `GET /api/dashboard/balances` - Get balance summary for current user with group breakdowns

## Project Structure

```
src/
├── main/
│   ├── java/com/splitpush/
│   │   ├── config/          # Security and cache configuration
│   │   ├── controller/      # REST and view controllers
│   │   ├── dto/            # Data transfer objects
│   │   ├── model/          # Entity models
│   │   ├── repository/     # JPA repositories
│   │   ├── service/        # Business logic (with caching)
│   │   └── SplitpushApplication.java
│   └── resources/
│       ├── static/
│       │   ├── css/        # Stylesheets (with CSS variables for theming)
│       │   └── js/         # JavaScript files
│       ├── templates/      # Thymeleaf templates
│       └── application.properties
└── pom.xml
```

## Database Schema

- **users**: User accounts (username, email, password, name)
- **trip_groups**: Trip groups (ULID-based IDs, name, description, created_by)
- **trip_group_members**: Many-to-many relationship between users and groups
- **expenses**: Expenses (description, amount, paid_by, trip_group, created_at)
- **expense_participants**: Participants in expenses with amounts owed and payment status
- **settlements**: Recorded settlements between users in specific groups

## Database

### Local Development
- PostgreSQL running in Docker (via docker-compose)
- Data persists between application restarts (stored in Docker volume `postgres_data`)
- Database connection details:
  - Docker: Uses service name `postgres` on internal network
  - Local: Uses `localhost:5432`
- Configuration files:
  - `application.properties` - Local development
  - `application-docker.properties` - Docker environment (uses `postgres` hostname, supports environment variables)
- To stop services: `docker-compose down`
- To remove all data: `docker-compose down -v` or use `TRUNCATE` commands

### Production Deployment (Supabase)
- External PostgreSQL database hosted on Supabase
- SSL connection required (`sslmode=require`)
- Configuration via environment variables (see `RENDER_DEPLOYMENT.md`)
- Supports both local Docker PostgreSQL and external databases via environment variable overrides

### Docker Configuration
- **Dockerfile**: Multi-stage build for optimized image size
- **docker-compose.yml**: Orchestrates database and application services (supports environment variable overrides)
- **.dockerignore**: Excludes unnecessary files from build context
- Application waits for database health check before starting
- Services run on internal Docker network for secure communication

## Key Features & Implementation Details

### Caching
- **Backend caching** implemented using Caffeine cache
- Caches user lookups, group lists, expenses, balances, and settlements
- Cache eviction on write operations to ensure data consistency
- Configurable cache sizes and expiration times

### Authentication
- **Email-based authentication** (changed from username)
- BCrypt password encoding
- Session-based authentication
- CSRF is disabled for development. Enable it for production.

### Data Validation
- Settlement amounts cannot exceed what is owed
- Users cannot leave groups with outstanding balances
- Both users must be members of a group to record settlements
- Expense participant amounts must sum to total expense amount

### UI/UX Features
- **Light/Dark Mode**: Toggle with persistent localStorage preference
- **Pagination**: 10 items per page for expenses and settlements
- **Modal Improvements**: Centered modals with proper dark mode support
- **Quick Actions**: Select All/Deselect All for expense participants
- **Group Links**: Direct links from dashboard to group expense pages

### Group Management
- Groups use ULID for unique, URL-friendly IDs
- Automatic deletion when last member leaves (if no balances)
- Group ID sharing for easy joining
- Balance validation prevents leaving groups with debts


## Deployment

### Deploy to Render with Supabase

The application is configured to deploy to Render using Docker, with Supabase as the PostgreSQL database provider.

**Quick Start:**
1. See `RENDER_DEPLOYMENT.md` for detailed step-by-step instructions
2. Set up a Supabase project and get connection details
3. Create a Web Service on Render
4. Configure environment variables (see `.env.example` for reference)
5. Deploy!

**Key Files:**
- `RENDER_DEPLOYMENT.md` - Complete deployment guide
- `.env.example` - Environment variable template
- `application-production.properties` - Production configuration profile
- `Dockerfile` - Multi-stage Docker build

**Required Environment Variables for Render:**
- `SPRING_PROFILES_ACTIVE=production`
- `SPRING_DATASOURCE_URL` - Supabase JDBC connection string
- `SPRING_DATASOURCE_USERNAME` - Supabase database username
- `SPRING_DATASOURCE_PASSWORD` - Supabase database password

See `RENDER_DEPLOYMENT.md` for complete setup instructions.

## Future Enhancements

- Enhanced member management UI
- Email notifications for expenses and settlements
- Group statistics and reports
- Support for different currencies
- Mobile-responsive improvements
- Expense categories and tags
