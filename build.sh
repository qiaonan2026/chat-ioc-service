#!/bin/bash

# Build and Package Script for Chat IOC Service

echo "Building Chat IOC Service..."

# Compile the project
mvn clean compile

echo "Running tests..."
mvn test

echo "Packaging the application..."
mvn package

echo "Build completed successfully!"
echo "To run the application:"
echo "java -jar target/chat-ioc-service-1.0.0-SNAPSHOT-shaded.jar"