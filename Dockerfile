# Stage 1: Build the application
FROM maven:3.8.5-eclipse-temurin-17 AS build

# Set the working directory
WORKDIR /app

# Copy the Maven project files
COPY pom.xml .

# Download dependencies (this step helps in caching dependencies)
RUN mvn dependency:go-offline -B

# Copy the rest of the application code
COPY src ./src

# Package the application
RUN mvn clean package -DskipTests

# Stage 2: Create the image with JRE
FROM eclipse-temurin:17-jre-alpine

# Set the working directory inside the image
WORKDIR /app

# Copy the Spring Boot jar from the previous build stage
COPY --from=build /app/target/*.jar app.jar

# Expose the port on which the application will run
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]