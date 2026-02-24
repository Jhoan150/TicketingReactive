# ─── Build Stage ───────────────────────────────────────────────────────────────
FROM eclipse-temurin:25-jdk-noble AS builder

WORKDIR /app

# Copy Maven wrapper and pom.xml first to leverage layer caching
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Ensure mvnw is executable (important when copying from Windows)
RUN chmod +x mvnw

# Download dependencies (cached unless pom.xml changes)
RUN ./mvnw dependency:go-offline -q

# Copy source and build (without -q to show errors clearly)
COPY src ./src
RUN ./mvnw package -DskipTests

# ─── Runtime Stage ─────────────────────────────────────────────────────────────
FROM eclipse-temurin:25-jre-noble AS runtime

WORKDIR /app

# Security: run as non-root user
RUN groupadd --gid 1001 ticketing && \
  useradd --uid 1001 --gid ticketing --shell /bin/bash --create-home ticketing

COPY --from=builder /app/target/*.jar app.jar
RUN chown ticketing:ticketing app.jar

USER ticketing

EXPOSE 8080

ENTRYPOINT ["java", \
  "--enable-preview", \
  "-XX:+UseZGC", \
  "-Xmx512m", \
  "-jar", "app.jar"]
