# Splitpush - Expense Splitting Application

A Spring Boot web application similar to Splitwise where users can create accounts, form trip groups, and split expenses among group members.

## Features

- **User Authentication**: Register and login with secure password hashing
- **Trip Groups**: Create and manage trip groups with multiple members
- **Expense Management**: Add expenses to groups and split them among participants
- **Balance Dashboard**: View net amounts owed to/from other users, grouped by user
- **Expense Tracking**: See all expenses in a group with detailed participant information

## Technology Stack

- **Backend**: Spring Boot 3.2.0, Spring Security, Spring Data JPA
- **Database**: H2 (in-memory, for development)
- **Frontend**: HTML, CSS, JavaScript, Thymeleaf
- **Build Tool**: Maven

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- Docker and Docker Compose (for PostgreSQL database)

## Getting Started

### 1. Clone or navigate to the project directory

```bash
cd Splitpush
```

### 2. Start the PostgreSQL database

```bash
docker-compose up -d
```

This will start a PostgreSQL container in the background. The database will be available at `localhost:5432`.

### 3. Build the project

```bash
mvn clean install
```

### 4. Run the application

```bash
mvn spring-boot:run
```

Or run the main class `SplitpushApplication` from your IDE.

### 5. Access the application

Open your browser and navigate to:
- **Application**: http://localhost:8080

The application will automatically create the database schema on first startup.

## Usage

### 1. Register a New Account
- Navigate to http://localhost:8080/register
- Fill in your username, email, name, and password
- Click "Register"

### 2. Login
- Use your credentials to login at http://localhost:8080/login

### 3. Create a Trip Group
- Click on "Groups" in the navigation
- Click "Create Group"
- Enter group name and optional description
- The creator is automatically added as a member

### 4. Add Members to Group
- Members can be added via the API (future enhancement: UI for adding members)

### 5. Add Expenses
- Click on a group to view/add expenses
- Click "Add Expense"
- Enter description, amount, who paid, and split among participants
- Use "Split Equally" button for equal distribution
- Or manually enter amounts for each participant

### 6. View Dashboard
- Navigate to Dashboard to see your net balances
- Positive amounts = others owe you
- Negative amounts = you owe others

## API Endpoints

### Authentication
- `POST /api/auth/register` - Register a new user
- `POST /api/auth/login` - Login (form-based)
- `POST /api/auth/logout` - Logout

### Groups
- `GET /api/groups` - Get all groups for current user
- `GET /api/groups/{id}` - Get group by ID
- `POST /api/groups` - Create a new group
- `POST /api/groups/{groupId}/members` - Add member to group
- `DELETE /api/groups/{groupId}/members/{userId}` - Remove member from group

### Expenses
- `GET /api/expenses/group/{groupId}` - Get all expenses for a group
- `GET /api/expenses/{id}` - Get expense by ID
- `POST /api/expenses` - Create a new expense
- `POST /api/expenses/{expenseId}/participants/{userId}/pay` - Mark expense as paid

### Dashboard
- `GET /api/dashboard/balances` - Get balance summary for current user

## Project Structure

```
src/
├── main/
│   ├── java/com/splitpush/
│   │   ├── config/          # Security configuration
│   │   ├── controller/      # REST and view controllers
│   │   ├── dto/            # Data transfer objects
│   │   ├── model/          # Entity models
│   │   ├── repository/     # JPA repositories
│   │   ├── service/        # Business logic
│   │   └── SplitpushApplication.java
│   └── resources/
│       ├── static/
│       │   ├── css/        # Stylesheets
│       │   └── js/         # JavaScript files
│       ├── templates/      # Thymeleaf templates
│       └── application.properties
└── pom.xml
```

## Database Schema

- **users**: User accounts
- **trip_groups**: Trip groups
- **trip_group_members**: Many-to-many relationship between users and groups
- **expenses**: Expenses
- **expense_participants**: Participants in expenses with amounts owed

## Database

- The application uses PostgreSQL running in Docker.
- Data persists between application restarts.
- Database connection details are in `application.properties`.
- To stop the database: `docker-compose down`
- To remove all data: `docker-compose down -v`

## Notes

- Password encoding uses BCrypt.
- CSRF is disabled for development. Enable it for production.
- See `DOCKER_SETUP.md` for detailed Docker instructions.

## Future Enhancements

- Add member management UI
- Add expense editing and deletion
- Add payment tracking and settlement
- Add email notifications
- Add group statistics and reports
- Support for different currencies
- Mobile-responsive improvements

## License

This project is open source and available for educational purposes.

