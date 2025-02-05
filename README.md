# loan-tracker

## This mono repo contains backend and frontend code for loan tracker application
- Backend is a spring boot application loan-tracker-service
### running the service using image
- docker pull zeeshan60/loan-tracker-service:latest
- docker run -d -p 8080:8080 -e SPRING_PROFILES_ACTIVE=dev --name loantracker zeeshan60/loan-tracker-service:latest
- docker pull postgres:latest
- docker run -d -p 5432:5432 -e POSTGRES_PASSWORD=postgres --name postgres postgres:latest
### running the Ui mobile application using image
- docker pull nomantufail/loan-tracker-ui-repo:latest
- docker run -d -p 80:80 --name loan-tracker-ui nomantufail/loan-tracker-ui-repo:latest
