# Multi-stage build for Spring Boot on Render / Cloud
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Copy maven wrapper and pom
COPY pom.xml mvnw ./
COPY .mvn .mvn
RUN chmod +x ./mvnw

# Copy source code and build package
COPY src ./src
RUN ./mvnw clean package -DskipTests

# Production runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/attendance-management-system-1.0.0.jar app.jar

ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
