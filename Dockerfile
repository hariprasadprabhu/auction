FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app

# Copy the built JAR file
COPY target/auction-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

# Set memory limits for Railway
ENV JAVA_OPTS="-Xmx256m -Xms128m"

CMD ["java", "-jar", "app.jar"]

