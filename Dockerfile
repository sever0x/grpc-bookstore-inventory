FROM gradle:jdk21-alpine AS build

COPY --chown=gradle:gradle . /home/gradle/src

WORKDIR /home/gradle/src

RUN gradle build --no-daemon

EXPOSE 9090

FROM openjdk:21-jdk-slim

WORKDIR /grpc-service

COPY . .

CMD ["./gradlew", "clean", "bootJar"]

COPY build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]