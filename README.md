# loan-tracker

quick ec2 setup:

- sudo su
- yum install git -y
- yum install docker -y
- systemctl start docker
- systemctl enable docker
- usermod -aG docker ec2-user
- sudo docker build . -t loantracker

another way to send image because building is expensive

on your local machine
- docker build . -t loantracker
- docker save -o image.tar loantracker:latest
- scp -i "zee.pem" image.tar ec2-user@ec2-52-74-229-194.ap-southeast-1.compute.amazonaws.com:/home/ec2-user
- sudo docker load -i image.tar
- sudo docker stop loantracker
- sudo docker container prune -f
- sudo docker run -d -p 8080:8080 -e SPRING_PROFILES_ACTIVE=dev --name loantracker loantracker
- sudo docker image prune -f