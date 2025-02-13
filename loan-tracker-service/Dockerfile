FROM --platform=linux/amd64 gradle:7-jdk17-alpine AS builder
WORKDIR /home/gradle/src
COPY . .
RUN gradle build -x test

FROM eclipse-temurin:17-jre

EXPOSE 8080

COPY --from=builder /home/gradle/src/build/libs/*.jar app.jar
COPY firebase-secret.json /firebase-secret.json
ENTRYPOINT ["java","-jar","/app.jar"]