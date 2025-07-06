# Build stage with optimized caching
FROM maven:3.8-openjdk-17 AS build

# Set working directory
WORKDIR /home/app

# Copy pom.xml first for better layer caching
# This layer will be cached if pom.xml doesn't change
COPY pom.xml .

# Download dependencies (this layer will be cached if pom.xml doesn't change)
# This is the key optimization - dependencies are cached separately from source code
RUN mvn dependency:go-offline -B \
    && mvn dependency:resolve -B \
    && mvn dependency:resolve-plugins -B

# Copy source code (this layer changes when source code changes)
COPY src ./src

# Build the application (this layer changes when source code or dependencies change)
RUN mvn -f pom.xml clean package -DskipTests=true \
    && mvn dependency:purge-local-repository

# Package stage
FROM openjdk:21-ea-17-slim-buster

# Install curl for health checks and create app user for security
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/* \
    && groupadd -r appuser && useradd -r -g appuser appuser

# Set working directory
WORKDIR /app

# Copy the built jar
COPY --from=build /home/app/target/PetCarePlus-0.0.1-SNAPSHOT.jar app.jar

# Create directories for logs and cache
RUN mkdir -p /app/logs /app/cache && chown -R appuser:appuser /app

# Switch to app user
USER appuser

# JVM optimizations
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:+UseStringDeduplication -XX:+OptimizeStringConcat -XX:+UseCompressedOops -XX:+UseCompressedClassPointers -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/app/logs/"

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
