# loan-tracker

## Some handy commands to setup docker and run service on ec2

quick ec2 setup:

```shell
sudo su
yum install git -y
yum install docker -y
systemctl start docker
systemctl enable docker
usermod -aG docker ec2-user
docker build . -t zeeshan60/loan-tracker-service
docker save -o image.tar zeeshan60/loan-tracker-service:latest
docker push zeeshan60/loan-tracker-service:latest
docker build . -t zeeshan60/loan-tracker-service && docker push zeeshan60/loan-tracker-service:latest
docker tag loan-tracker-service:latest zeeshan60/loan-tracker-service:latest
docker run -d -p 8000:8000 --name dynamo amazon/dynamodb-local:latest -jar DynamoDBLocal.jar -sharedDb
```

another way to send image because building is expensive

on your local machine

```shell
docker build . -t loantracker
docker save -o image.tar loantracker:latest
scp -i "zee.pem" image.tar ec2-user@ec2-52-74-229-194.ap-southeast-1.compute.amazonaws.com:/home/ec2-user
ssh -i "zee.pem" ec2-user@ec2-52-74-229-194.ap-southeast-1.compute.amazonaws.com
sudo docker load -i image.tar
sudo docker stop loantracker
sudo docker container prune -f
sudo docker pull zeeshan60/loan-tracker-service:latest
sudo docker run -d -p 8080:8080 -e SPRING_PROFILES_ACTIVE=dev --name loantracker zeeshan60/loan-tracker-service:latest
sudo docker run --network host -d -p 8080:8080 -e SPRING_PROFILES_ACTIVE=dev --name loantracker
zeeshan60/loan-tracker-service:latest
sudo docker image prune -f
sudo docker rmi loantracker
sudo docker logs -f loantracker
sudo docker pull postgres:latest
sudo docker run -d -p 5432:5432 -e POSTGRES_PASSWORD=postgres --name postgres postgres:latest
docker build . -t zeeshan60/loan-tracker-service && docker push zeeshan60/loan-tracker-service:latest
sudo docker pull zeeshan60/loan-tracker-service:latest && sudo docker stop loantracker && sudo docker container prune
  -f && sudo docker run -d -p 8080:8080 -e SPRING_PROFILES_ACTIVE=dev --name loantracker zeeshan60/loan-tracker-service:
  latest && sudo docker image prune -f
```

### (Below commands run container on host network)

- sudo docker pull zeeshan60/loan-tracker-service:latest && sudo docker stop loantracker && sudo docker container prune
  -f && sudo docker run --network host -d -p 8080:8080 -e SPRING_PROFILES_ACTIVE=dev --name loantracker
  zeeshan60/loan-tracker-service:latest && sudo docker image prune -f
- sudo docker pull nomantufail/loan-tracker-ui-repo:latest && sudo docker stop loan-tracker-ui && sudo docker container
  prune -f && sudo docker run -d -p 80:80 --name loan-tracker-ui nomantufail/loan-tracker-ui-repo:latest && sudo docker
  image prune -f

# Code deploy learning

- created iam role with s3 and code deploy permissions
- attached to ec2 instance

## installing codedeploy agent

    sudo yum update -y
    sudo yum install ruby wget -y
    cd /home/ec2-user
    wget https://aws-codedeploy-us-east-1.s3.us-east-1.amazonaws.com/latest/install
    chmod +x install
    sudo ./install auto
    sudo systemctl start codedeploy-agent
    sudo systemctl enable codedeploy-agent

### Verify installation

    sudo systemctl status codedeploy-agent

## Creating code deploy s3 bucket

## Created applciation on code deploy.

- created deployment group
- updated iam role created earlier trust relationships to this

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Service": "codedeploy.amazonaws.com"
      },
      "Action": "sts:AssumeRole"
    }
  ]
}
```    

## Add AppSpec File for Deployment in code

```yaml  appspec.yml
version: 0.0
os: linux
files:
  - source: /
    destination: /home/ec2-user/app
permissions:
  - object: /home/ec2-user/app
    owner: ec2-user
    group: ec2-user
hooks:
  ApplicationStart:
    - location: scripts/deploy.sh
      timeout: 300
      runas: ec2-user
```
# Installing gradle on windows
- in power shell
- Set-ExecutionPolicy RemoteSigned -Scope CurrentUser
- iwr -useb get.scoop.sh | iex
- scoop install gradle
- go in project and do gradle wrapper
- git add -f gradle/wrapper/gradle-wrapper.jar


