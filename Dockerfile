# syntax=docker/dockerfile:1

# ---- Build stage -----------------------------------------------------------
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace

# Copy the POM first so dependency resolution is cached across builds even
# when only application source changes.
COPY pom.xml .
COPY src ./src

# Unit/integration tests already run as their own explicit CI gate before
# this image is ever built (see .github/workflows/reusable-build-test.yml),
# so we skip them here to keep the image build itself fast and single-purpose.
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q clean package -DskipTests

# ---- Runtime stage ----------------------------------------------------------
FROM eclipse-temurin:21-jre-jammy AS runtime

# Zero third-party runtime dependencies by design (see README) -- the JRE
# base image is all this service needs at runtime.
RUN groupadd --system app && useradd --system --gid app --home-dir /app app
WORKDIR /app
COPY --from=build /workspace/target/cicd-pipeline-demo.jar app.jar
RUN chown app:app app.jar
USER app

ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
