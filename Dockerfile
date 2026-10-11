FROM eclipse-temurin:21-jre-alpine
ARG MODULE
WORKDIR /app
COPY ${MODULE}/target/${MODULE}-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
