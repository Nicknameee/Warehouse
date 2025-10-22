#!/bin/bash

# Run docker build with the token as a build-arg
docker build -t warehouse .

read -p "Press Enter to create network..."

docker network create backend-network

read -p "Press Enter to start service..."

docker-compose up -d

read -p "Press Enter to close..."
