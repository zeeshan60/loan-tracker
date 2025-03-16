#!/bin/bash

AUTH_TOKEN=
POSTGRES_USER=
POSTGRES_PASSWORD=
POSTGRES_DB=
DEPLOY_WEBHOOK_PORT=5000
POSTGRES_URL="postgresql://loantracker.cfk6kwwo8e2a.ap-southeast-1.rds.amazonaws.com:5432/postgres"
SPRING_PROFILES_ACTIVE="prod"
IMAGE_TAG="prod"

# Update the instance and install necessary packages
sudo yum update -y

# Install Python 3, pip3, curl, git, and Docker
sudo yum install -y python3
sudo yum install -y python3-pip
sudo yum install -y  curl
# Install Docker
sudo yum install -y docker

# Start and enable Docker service
sudo systemctl start docker
sudo systemctl enable docker

# Install Docker Compose (if not already installed)
sudo curl -L "https://github.com/docker/compose/releases/download/1.29.2/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# Install Flask using pip3
sudo pip3 install Flask

# Create the Docker Compose file (docker-compose.yml)
cat <<EOF > /home/ec2-user/docker-compose.yml
version: '3.8'

services:
  postgres:
    image: postgres
    container_name: postgres_db
    restart: unless-stopped
    environment:
      POSTGRES_USER: ${POSTGRES_USER}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
      POSTGRES_DB: ${POSTGRES_DB}
    ports:
      - "5432:5432"
    networks:
      - loan-tracker-net

  service:
    image: zeeshan60/loan-tracker-service:${IMAGE_TAG}
    container_name: loan_tracker_service
    restart: unless-stopped
    depends_on:
      - postgres
    environment:
      DATABASE_URL: ${POSTGRES_URL}
      DATABASE_USER: ${POSTGRES_USER}
      DATABASE_PASSWORD: ${POSTGRES_PASSWORD}
      SPRING_PROFILES_ACTIVE: ${ENVIRONMENT}
    ports:
      - "8080:8080"
    networks:
      - loan-tracker-net

  ui:
    image: zeeshan60/loan-tracker-ui-repo:${IMAGE_TAG}
    container_name: loan_tracker_ui
    restart: unless-stopped
    depends_on:
      - service
    ports:
      - "8081:80"
    networks:
      - loan-tracker-net

networks:
  loan-tracker-net:
    driver: bridge
EOF

# Create the Flask app script (app.py)
cat <<EOF > /home/ec2-user/app.py
from flask import Flask, request, jsonify, abort
import subprocess
import os

app = Flask(__name__)

# Set the path to the directory containing your docker-compose.yml
DOCKER_COMPOSE_DIR = '/home/ec2-user/'
TOKEN = '${AUTH_TOKEN}'

@app.route('/deploy', methods=['POST'])
def deploy():
    # Check for a valid authorization token
    if request.headers.get('Authorization') != f'Bearer {TOKEN}':
        abort(403)  # Forbidden
    try:
        # Make sure the Docker Compose directory exists
        if not os.path.isdir(DOCKER_COMPOSE_DIR):
            return jsonify({'error': 'Docker Compose directory not found'}), 404

        # Navigate to the directory where your docker-compose.yml file is located
        os.chdir(DOCKER_COMPOSE_DIR)

        # Step 1: Pull latest images (only updates changed images)
        subprocess.run(['sudo', 'docker-compose', 'pull'], check=True)

        # Step 2: Restart only services with updated images
        subprocess.run(['sudo', 'docker-compose', 'up', '-d'], check=True)

        # Step 3: Remove old, unused containers
        subprocess.run(['sudo', 'docker', 'image', 'prune', '-f'], check=False)

        return jsonify({'message': 'Deployment successful'}), 200
    except subprocess.CalledProcessError as e:
        return jsonify({'error': 'Docker Compose failed', 'details': str(e)}), 500

if __name__ == '__main__':
    # Start the Flask app on port webhook port
    app.run(host='0.0.0.0', port=${DEPLOY_WEBHOOK_PORT})
EOF

# Start the Flask application in the background
nohup python3 /home/ec2-user/app.py &
