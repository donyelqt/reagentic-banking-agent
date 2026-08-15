# Image for any service module.
# Expects prebuilt fat jars produced by `mvnw package` (see README).
# Build arg MODULE selects which jar to copy (e.g. payment-service).
ARG MODULE
FROM eclipse-temurin:21-jre
ARG MODULE
WORKDIR /app
# Install curl for container healthchecks (Actuator /actuator/health probes).
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
COPY ${MODULE}/target/${MODULE}-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
