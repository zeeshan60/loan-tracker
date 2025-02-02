# loan-tracker

## Some handy commands to setup docker and run service on ec2
quick ec2 setup:

- sudo su
- yum install git -y
- yum install docker -y
- systemctl start docker
- systemctl enable docker
- usermod -aG docker ec2-user
- docker build . -t zeeshan60/loan-tracker-service
- docker push zeeshan60/loan-tracker-service:latest
- docker tag loan-tracker-service:latest zeeshan60/loan-tracker-service:latest

another way to send image because building is expensive

on your local machine
- docker build . -t loantracker
- docker save -o image.tar loantracker:latest
- scp -i "zee.pem" image.tar ec2-user@ec2-52-74-229-194.ap-southeast-1.compute.amazonaws.com:/home/ec2-user
- ssh -i "zee.pem" ec2-user@ec2-52-74-229-194.ap-southeast-1.compute.amazonaws.com
- sudo docker load -i image.tar
- sudo docker stop loantracker
- sudo docker container prune -f
- sudo docker pull zeeshan60/loan-tracker-service:latest
- sudo docker run -d -p 8080:8080 -e SPRING_PROFILES_ACTIVE=dev --name loantracker zeeshan60/loan-tracker-service:latest
- sudo docker image prune -f
- sudo docker rmi loantracker
- sudo docker logs -f loantracker