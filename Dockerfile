# Runtime stage
FROM docker.cbp.dhs.gov/cloud/ubi/openjdk:21-ubi9

# Set working directory
WORKDIR /app

# Copy built JAR files
COPY reference-api/target/reference-api-*.jar ./reference-api.jar
COPY reference-events/target/reference-events-*.jar ./reference-events.jar
COPY translation-service/target/translation-service-*.jar ./translation-service.jar


# Copy configuration
COPY config ./config

# Default command (can be overridden)
CMD ["java", "-jar", "reference-api.jar"]
