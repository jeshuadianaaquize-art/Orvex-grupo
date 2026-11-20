# syntax=docker/dockerfile:1
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
# La caché de ~/.m2 se reutiliza entre builds: las dependencias se descargan una sola vez.
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -DskipTests package

FROM eclipse-temurin:21-jre
ARG MODULO
WORKDIR /app
# /data/fotos: volumen de fotos del catálogo, escribible sin ser root.
RUN useradd --system --uid 10001 orvex \
    && mkdir -p /data/fotos && chown -R orvex /data
COPY --from=build /app/${MODULO}/target/servicio.jar app.jar
USER orvex
# Memoria acotada: son 10 JVM en la misma máquina.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -Xss512k"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
