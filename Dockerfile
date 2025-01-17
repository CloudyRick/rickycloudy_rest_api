# Use the official OpenJDK image as the base image
FROM openjdk:21-jdk-slim

# Set the working directory
WORKDIR /app

# Copy the application jar file to the container
# Replace 'your-application.jar' with the actual jar file name
COPY build/libs/rickcloudy-api.jar /app/app.jar


ENV JAVA_OPTS="-Djava.management.disabled=true"

# Expose the port that the application will run on
# Replace '8080' with your actual application port if different
EXPOSE 8080

# Run the application
CMD ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
