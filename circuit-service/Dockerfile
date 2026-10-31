FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
COPY src src
RUN mvn -B -ntp package -DskipTests

FROM eclipse-temurin:21-jre-jammy
RUN groupadd --system app && useradd --system --gid app app && mkdir /app && chown app:app /app
WORKDIR /app
COPY --from=build /build/target/circuit-service-1.0.0.jar app.jar
USER app
ENV SERVER_ADDRESS=0.0.0.0 SERVER_PORT=8082
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
