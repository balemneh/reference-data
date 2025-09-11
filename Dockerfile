# Multi-stage build for CBP Reference Data Service
FROM maven:3.9.6-eclipse-temurin-21 AS builder

# Set working directory
WORKDIR /app

# Copy parent pom and module poms first for better layer caching
COPY pom.xml .
COPY reference-core/pom.xml ./reference-core/
COPY reference-api/pom.xml ./reference-api/
COPY reference-events/pom.xml ./reference-events/
COPY reference-workflow/pom.xml ./reference-workflow/
COPY translation-service/pom.xml ./translation-service/
COPY catalog-integration/pom.xml ./catalog-integration/
COPY reference-loaders/common/pom.xml ./reference-loaders/common/
COPY reference-loaders/iso/pom.xml ./reference-loaders/iso/
COPY reference-loaders/genc/pom.xml ./reference-loaders/genc/

# Download dependencies
RUN mvn dependency:go-offline -B

# Copy source code
COPY reference-core/src ./reference-core/src
COPY reference-api/src ./reference-api/src
COPY reference-events/src ./reference-events/src
COPY reference-workflow/src ./reference-workflow/src
COPY translation-service/src ./translation-service/src
COPY catalog-integration/src ./catalog-integration/src
COPY reference-loaders/common/src ./reference-loaders/common/src
COPY reference-loaders/iso/src ./reference-loaders/iso/src
COPY reference-loaders/genc/src ./reference-loaders/genc/src

# Copy config and resources
COPY config ./config

# Build the application
RUN mvn clean package -DskipTests -B

# Runtime stage
FROM eclipse-temurin:21-jre-alpine AS runtime

# Install curl for health checks
RUN apk add --no-cache curl

# Create non-root user
RUN addgroup -g 1000 appgroup && adduser -u 1000 -G appgroup -s /bin/sh -D appuser

# Set working directory
WORKDIR /app

# Copy built JAR files
COPY --from=builder /app/reference-api/target/reference-api-*.jar ./reference-api.jar
COPY --from=builder /app/reference-events/target/reference-events-*.jar ./reference-events.jar
COPY --from=builder /app/reference-workflow/target/reference-workflow-*.jar ./reference-workflow.jar
COPY --from=builder /app/translation-service/target/translation-service-*.jar ./translation-service.jar

# Copy configuration
COPY --from=builder /app/config ./config

# Change ownership to non-root user
RUN chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Default command (can be overridden)
CMD ["java", "-jar", "reference-api.jar"]