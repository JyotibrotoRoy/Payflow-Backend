# Stage 1: Build the JAR
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Copy only the pom first to cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy source and build
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Run the JAR
FROM eclipse-temurin:21-jre
WORKDIR /app

# Ensure the JAR name matches your pom.xml artifactId
COPY --from=build /app/target/*.jar app.jar

COPY --from=build /app/src/main/resources/client.truststore.jks /app/client.truststore.jks
COPY --from=build /app/src/main/resources/client.keystore.jks /app/client.keystore.jks

EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]