# Build stage
FROM maven:3.8.4-openjdk-17-slim AS build
COPY . /app
WORKDIR /app
RUN mvn clean package -DskipTests

# Run stage
FROM openjdk:17-jdk-slim
COPY --from=build /app/target/opexy-1.0.0.jar /app/opexy-bot.jar
WORKDIR /app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "opexy-bot.jar"]
