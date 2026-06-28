FROM gradle:8.14-jdk21-alpine AS build
WORKDIR /workspace
COPY --chown=gradle:gradle . .
RUN gradle :app:bootJar --no-daemon -q

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/app/build/libs/app-*.jar ./
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java -jar /app/app-*.jar"]
