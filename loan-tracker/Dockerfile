FROM --platform=linux/amd64 gradle:7-jdk17-alpine AS builder
WORKDIR /home/gradle/src
COPY . .
RUN gradle bootJar

FROM eclipse-temurin:17-jre

EXPOSE 8080

COPY --from=builder /home/gradle/src/build/libs/*.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]