#!/bin/bash
cd /home/ec2-user/app

sudo docker pull zeeshan60/loan-tracker-service:latest
sudo docker stop loantracker || true
sudo docker container prune -f || true
sudo docker run -d -p 8080:8080 -e SPRING_PROFILES_ACTIVE=dev --name loantracker zeeshan60/loan-tracker-service:latest
sudo docker image prune -f || true
