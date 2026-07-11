FROM gradle:8.14-jdk21-alpine AS build
WORKDIR /workspace
COPY --chown=gradle:gradle . .
RUN gradle :payment-service:bootJar --no-daemon -q

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/payment-service/build/libs/payment-service-*.jar ./
EXPOSE 8081
ENTRYPOINT ["sh", "-c", "java -jar /app/payment-service-*.jar"]
