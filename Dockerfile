# Multi-stage build for Spring Boot application
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

# Configure Maven to use system DNS resolver
RUN echo "networkaddress.cache.ttl=0" >> $JAVA_HOME/conf/security/java.security || true

# Copy pom.xml first for dependency caching
COPY pom.xml .

# Copy source code
COPY src ./src

# Build the application (Maven will download dependencies during build)
# Using -U to update dependencies
RUN mvn clean package -DskipTests -B -U

# Runtime stage
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Create non-root user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy the built JAR from build stage
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]

