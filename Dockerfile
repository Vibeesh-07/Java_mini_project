# Stage 1: Build & verify application using JDK 21 and Maven Wrapper
FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /build

# Copy Maven Wrapper and POM first to leverage Docker layer caching
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Make wrapper script executable
RUN chmod +x ./mvnw

# Pre-fetch project dependencies
RUN ./mvnw dependency:go-offline -B || true

# Copy project source code
COPY src/ src/

# Build and verify JAR with automated test suite
RUN ./mvnw clean package -DskipTests=false -B

# Stage 2: Minimal, secure JRE 21 runtime
FROM eclipse-temurin:21-jre-jammy AS runner

WORKDIR /app

# Create unprivileged non-root user and group
RUN groupadd -g 1001 appgroup && \
    useradd -u 1001 -g appgroup -s /bin/bash -m appuser

# Copy built JAR from build stage
COPY --from=builder /build/target/refind-*.jar /app/app.jar

# Adjust ownership
RUN chown -R appuser:appgroup /app

# Switch to unprivileged non-root user
USER 1001:1001

# Default environment variables (overridden by Railway or Docker Compose)
ENV PORT=8080 \
    JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -Djava.security.egd=file:/dev/./urandom"

# Expose port (documented; Railway dynamically maps via $PORT)
EXPOSE 8080

# Health check using Spring Boot Actuator
HEALTHCHECK --interval=30s --timeout=5s --start-period=35s --retries=3 \
    CMD curl -f http://localhost:${PORT:-8080}/actuator/health || exit 1

# Exec-form entrypoint ensuring signals (SIGTERM) are handled cleanly
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
