#!/bin/bash
set -x
# Function to check if a command exists
command_exists() {
  command -v "$1" >/dev/null 2>&1
}

# Check if jq is installed
if ! command_exists jq; then
  echo "Error: jq is not installed." >&2
  exit 1
fi

# Check if awscli is installed
if ! command_exists aws; then
  echo "Error: awscli is not installed." >&2
  exit 1
fi


# Set AWS Secret and Region
SECRET_NAME="dev/RickCloudy/API"
REGION="ap-southeast-2"
DOCKER_NETWORK="rickcloudy-app"

# Fetch the secret value from AWS Secrets Manager
SECRET_VALUE=$(aws secretsmanager get-secret-value --secret-id "$SECRET_NAME" --region "$REGION" --query 'SecretString' --output text 2>/dev/null)

# Check if the secret value was fetched
if [ -z "$SECRET_VALUE" ]; then
  echo "Error: Failed to fetch the secret from AWS Secrets Manager." >&2
  exit 1
fi

# Export environment variables for Spring Boot
export SERVER_PORT=$(echo $SECRET_VALUE | jq -r .SERVER_PORT)
export DB_URL=$(echo $SECRET_VALUE | jq -r .DB_URL)
export DB_USERNAME=$(echo $SECRET_VALUE | jq -r .DB_USERNAME)
export DB_PASSWORD=$(echo $SECRET_VALUE | jq -r .DB_PASSWORD)
export DB_NAME=$(echo $SECRET_VALUE | jq -r .DB_NAME)
export FLYWAY_URL=$(echo $SECRET_VALUE | jq -r .FLYWAY_URL)
export AWS_ACCESS_KEY_ID=$(echo $SECRET_VALUE | jq -r .AWS_ACCESS_KEY_ID)
export AWS_SECRET_ACCESS_KEY=$(echo $SECRET_VALUE | jq -r .AWS_SECRET_ACCESS_KEY)
export AWS_REGION=$(echo $SECRET_VALUE | jq -r .AWS_REGION)
export BLOG_IMAGES_BUCKET=$(echo $SECRET_VALUE | jq -r .BLOG_IMAGES_BUCKET)
export ACCESS_TOKEN_SECRET=$(echo $SECRET_VALUE | jq -r .ACCESS_TOKEN_SECRET)
export REFRESH_TOKEN_SECRET=$(echo $SECRET_VALUE | jq -r .REFRESH_TOKEN_SECRET)
export ACCESS_TOKEN_EXPIRATION_MS=$(echo $SECRET_VALUE | jq -r .ACCESS_TOKEN_EXPIRATION_MS)
export REFRESH_TOKEN_EXPIRATION_MS=$(echo $SECRET_VALUE | jq -r .REFRESH_TOKEN_EXPIRATION_MS)


# Set variables
JAR_NAME="rickcloudy-api.jar"  # Replace with your desired JAR file name
DOCKER_IMAGE_NAME="rickcloudy-api-prod"  # Replace with your Docker image name
DOCKER_CONTAINER_NAME="rickcloudy-api-prod"  # Replace with your Docker container name

# Step 1: Build the JAR file using Gradle
echo "Building the JAR file using Gradle..."
./gradlew bootJar

# Check if the JAR build was successful
if [ $? -ne 0 ]; then
  echo "Failed to build the JAR file."
  exit 1
fi

echo "JAR file built successfully."

# Step 2: Build the Docker image
echo "Building the Docker image..."
docker build -t $DOCKER_IMAGE_NAME .

# Check if the Docker image build was successful
if [ $? -ne 0 ]; then
  echo "Failed to build the Docker image."
  exit 1
fi

echo "Docker image built successfully."

# Step 3: Stop and remove the old container if it exists
if [ "$(docker ps -aq -f name=$DOCKER_CONTAINER_NAME)" ]; then
  echo "Stopping and removing existing Docker container..."
  docker rm -f $DOCKER_CONTAINER_NAME
fi


# Step 4: Run the Docker container
echo "Starting the Docker container..."
docker run --network $DOCKER_NETWORK -d -p $SERVER_PORT:$SERVER_PORT --name $DOCKER_CONTAINER_NAME \
                                                                              -e JAVA_OPTS="-Djava.management.disabled=true" \
                                                                              -e SERVER_PORT=$SERVER_PORT \
                                                                              -e DB_URL=$DB_URL \
                                                                              -e DB_USERNAME=$DB_USERNAME \
                                                                              -e DB_PASSWORD=$DB_PASSWORD \
                                                                              -e DB_NAME=$DB_NAME \
                                                                              -e FLYWAY_URL=$FLYWAY_URL \
                                                                              -e AWS_ACCESS_KEY_ID=$AWS_ACCESS_KEY_ID \
                                                                              -e AWS_SECRET_ACCESS_KEY=$AWS_SECRET_ACCESS_KEY \
                                                                              -e AWS_REGION=$AWS_REGION \
                                                                              -e BLOG_IMAGES_BUCKET=$BLOG_IMAGES_BUCKET \
                                                                              -e ACCESS_TOKEN_SECRET=$ACCESS_TOKEN_SECRET \
                                                                              -e REFRESH_TOKEN_SECRET=$REFRESH_TOKEN_SECRET \
                                                                              -e ACCESS_TOKEN_EXPIRATION_MS=$ACCESS_TOKEN_EXPIRATION_MS \
                                                                              -e REFRESH_TOKEN_EXPIRATION_MS=$REFRESH_TOKEN_EXPIRATION_MS \
                                                                              $DOCKER_IMAGE_NAME


echo "Docker container started successfully."

# End
echo "API is up and running!"
