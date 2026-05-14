# Stage 1: Build the JAR
FROM maven:3.9.9-eclipse-temurin-23 AS build
WORKDIR /app

# Copy only the pom first to cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy source and build
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Run the JAR
FROM eclipse-temurin:23-jre
WORKDIR /app

# Ensure the JAR name matches your pom.xml artifactId
COPY --from=build /app/target/*.jar app.jar

# This must match your application.yml server.port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]