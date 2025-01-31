# loan-tracker

## This mono repo contains backend and frontend code for loan tracker application
- Backend is a spring boot application loan-tracker-service
### running the service using image
- docker pull zeeshan60/loan-tracker-service:latest
- docker run -d -p 8080:8080 -e SPRING_PROFILES_ACTIVE=dev --name loantracker zeeshan60/loan-tracker-service:latest