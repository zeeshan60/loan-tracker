# Use the official gradle image to create a build artifact
FROM --platform=linux/amd64 gradle:7-jdk17-alpine as builder
WORKDIR /home/gradle/src
COPY . .
RUN gradle bootJar

# Use a Debian-based image to run your application
FROM openjdk:17-jdk-slim

RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    libfreetype6 \
    libstdc++6 \
    fontconfig \
    && rm -rf /var/lib/apt/lists/*

EXPOSE 8080

COPY --from=builder /home/gradle/src/build/libs/*.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
