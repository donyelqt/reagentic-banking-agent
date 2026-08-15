# Multi-stage build for any service module.
# Build arg MODULE selects which Maven module to package (e.g. payment-service).
# The module depends only on `common`, which is built via -am.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
RUN mvn -q -pl ${MODULE} -am package -DskipTests -Dmaven.test.skip=true

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/${MODULE}/target/${MODULE}-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
