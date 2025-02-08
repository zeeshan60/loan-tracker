#!/bin/bash
cd /home/ec2-user/appui

sudo docker pull zeeshan60/loan-tracker-ui-repo:latest
sudo docker stop loan-tracker-ui || true
sudo docker container prune -f || true
sudo docker run -d -p 80:80 --name loan-tracker-ui zeeshan60/loan-tracker-ui-repo:latest
sudo docker image prune -f || true
