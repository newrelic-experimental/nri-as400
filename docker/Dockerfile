# syntax=docker/dockerfile:1

# Build with a modern JDK; pom.xml's <release>8</release> targets JDK 8
# bytecode regardless (JDK 8's own compiler doesn't support --release at all,
# so building directly with JDK 8 here would fail).
FROM maven:3.9-eclipse-temurin-21 AS build
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
