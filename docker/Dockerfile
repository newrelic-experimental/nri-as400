# syntax=docker/dockerfile:1

# Build the nri-as400 shaded jar
FROM maven:3.9-eclipse-temurin-8 AS build
WORKDIR /build
COPY pom.xml LICENSE README.md .
COPY src ./src
RUN mvn -B -q -DskipTests package \
    && cp target/nri-as400-*.jar /build/nri-as400.jar

# Layer the integration onto the New Relic Infrastructure agent image.
# Base image is Alpine and ships without a JRE, so one is added for the
# jt400 (AS/400 access) library the integration depends on.
FROM newrelic/infrastructure:latest

RUN apk add --no-cache openjdk17-jre-headless

COPY --from=build /build/nri-as400.jar /var/db/newrelic-infra/newrelic-integrations/bin/nri-as400.jar
COPY src/main/resources/nri-as400-definition.yml /var/db/newrelic-infra/newrelic-integrations/nri-as400-definition.yml
