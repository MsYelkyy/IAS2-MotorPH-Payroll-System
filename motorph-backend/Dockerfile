# syntax=docker/dockerfile:1

# MotorPH backend (Spring Boot REST API) container image.
#
# Build context is motorph-backend/ only. That works because this module keeps
# spring-boot-starter-parent as its real parent (relativePath is empty), so it
# resolves from the repository and never needs the aggregator pom at the repo
# root. The desktop module is deliberately not part of this image.

# ---------- stage 1: build the executable jar ----------
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /build

# Copy the pom on its own first so the dependency download layer is cached and
# only re-runs when the pom actually changes, not on every source edit.
COPY pom.xml ./
# Warm-up only: it pre-populates the local repository so later source edits do
# not re-download everything. It is allowed to fail (some plugin resolutions do)
# because the package step below fetches whatever is still missing.
RUN mvn -B -e dependency:go-offline || true

COPY src ./src
# Tests are skipped here on purpose: the image build has no database and no
# test containers. Run `mvn -pl motorph-backend test` on the host instead.
RUN mvn -B -e -DskipTests package

# ---------- stage 2: runtime ----------
FROM eclipse-temurin:21-jre

WORKDIR /app

# Run as an unprivileged account rather than root. The two data directories are
# created up front and owned by that account, because the backend writes
# profile photos and database backups into them at runtime.
RUN groupadd --system motorph \
 && useradd --system --gid motorph --home-dir /app --shell /usr/sbin/nologin motorph \
 && mkdir -p /app/profile-photos /app/backups \
 && chown -R motorph:motorph /app

# Spring Boot's repackage goal produces exactly one *.jar plus a *.jar.original,
# and the glob below matches only the executable one.
COPY --from=build --chown=motorph:motorph /build/target/*.jar /app/app.jar

USER motorph

EXPOSE 8080

# No actuator dependency exists in this project, so health is probed with a
# plain TCP connect to the HTTP port rather than by adding a new dependency.
HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=10 \
    CMD bash -c 'exec 3<>/dev/tcp/127.0.0.1/8080' || exit 1

# Exec form so the JVM is PID 1 and receives SIGTERM directly on `compose stop`.
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
