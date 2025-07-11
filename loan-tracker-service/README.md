# loan-tracker

## Some handy commands to set up docker and run service on ec2

quick ec2 setup:

```bash
sudo su
yum install git -y
yum install docker -y
systemctl start docker
systemctl enable docker
usermod -aG docker ec2-user
docker buildx build --platform linux/amd64 -t zeeshan60/loan-tracker-service --push .
docker save -o image.tar zeeshan60/loan-tracker-service:latest
docker push zeeshan60/loan-tracker-service:latest
docker build . -t zeeshan60/loan-tracker-service && docker push zeeshan60/loan-tracker-service:latest
docker tag loan-tracker-service:latest zeeshan60/loan-tracker-service:latest
docker run -d -p 8000:8000 --name dynamo amazon/dynamodb-local:latest -jar DynamoDBLocal.jar -sharedDb
docker buildx build --build-arg BUILD_CONFIG=--prod --platform linux/amd64 -t zeeshan60/loan-tracker-ui-repo:prod --push .
```

another way to send image because building is expensive

on your local machine

```bash
docker build . -t loantracker
docker save -o image.tar loantracker:latest
scp -i "zee.pem" image.tar ec2-user@ec2-52-74-229-194.ap-southeast-1.compute.amazonaws.com:/home/ec2-user
scp -i "zee.pem" ../deploy/manual/start_script.sh ec2-user@18.141.11.231:/home/ec2-user
scp -i "zee.pem" ../deploy/docker ec2-user@ec2-13-228-157-84.ap-southeast-1.compute.amazonaws.com:/home/ec2-user
ssh -i "zee.pem" ec2-user@ec2-52-74-229-194.ap-southeast-1.compute.amazonaws.com
ssh -i "zee.pem" ec2-user@ec2-13-228-157-84.ap-southeast-1.compute.amazonaws.com #prod
ssh -i "zee.pem" ec2-user@46.137.192.133
ssh -i "zee.pem" ec2-user@13.228.157.84
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
docker logs -f --tail 50 loan_tracker_service
sudo docker pull postgres:latest
sudo docker run -d -p 5432:5432 -e POSTGRES_PASSWORD=postgres --name postgres postgres:latest
docker build . -t zeeshan60/loan-tracker-service && docker push zeeshan60/loan-tracker-service:latest
sudo docker pull zeeshan60/loan-tracker-service:latest && sudo docker stop loantracker && sudo docker container prune
  -f && sudo docker run -d -p 8080:8080 -e SPRING_PROFILES_ACTIVE=dev --name loantracker zeeshan60/loan-tracker-service:
  latest && sudo docker image prune -f
sudo docker exec loan_tracker_service env

#installing docker compose
sudo curl -L "https://github.com/docker/compose/releases/download/1.29.2/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose
sudo yum install -y libxcrypt-compat
sudo docker-compose rm -s -v ui
sudo docker-compose up -d
sudo docker rm -f $(sudo docker ps -a -q)
sudo docker image rm -f $(sudo docker images -q)
sudo netstat -tuln | grep 5000
free -h
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

```bash
sudo yum update -y
sudo yum install ruby wget -y
cd /home/ec2-user
wget https://aws-codedeploy-us-east-1.s3.us-east-1.amazonaws.com/latest/install
chmod +x install
sudo ./install auto
sudo systemctl start codedeploy-agent
sudo systemctl enable codedeploy-agent
```

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

- in PowerShell
- Set-ExecutionPolicy RemoteSigned -Scope CurrentUser
- iwr -useb get.scoop.sh | iex
- scoop install gradle
- go in project and do gradle wrapper
- git add -f gradle/wrapper/gradle-wrapper.jar

# Checking instance iam role and debugging code deploy

```bash
TOKEN=$(curl -X PUT "http://169.254.169.254/latest/api/token" -H "X-aws-ec2-metadata-token-ttl-seconds: 21600")
curl -H "X-aws-ec2-metadata-token: $TOKEN" -v http://169.254.169.254/latest/meta-data/iam/security-credentials/


TOKEN=$(curl -X PUT "http://169.254.169.254/latest/api/token" -H "X-aws-ec2-metadata-token-ttl-seconds: 21600")
curl -H "X-aws-ec2-metadata-token: $TOKEN" -v http://169.254.169.254/latest/meta-data/iam/security-credentials/Code_deploy


TOKEN=$(curl -X PUT "http://169.254.169.254/latest/api/token" -H "X-aws-ec2-metadata-token-ttl-seconds: 21600")
curl -H "X-aws-ec2-metadata-token: $TOKEN" -v http://169.254.169.254/latest/meta-data/iam

sudo tail -n 50 /var/log/aws/codedeploy-agent/codedeploy-agent.log

sudo systemctl restart codedeploy-agent
sudo systemctl status codedeploy-agent

sudo systemctl stop codedeploy-agent
sudo systemctl start codedeploy-agent

sudo service codedeploy-agent stop
sudo rm -f /opt/codedeploy-agent/state/.pid/codedeploy-agent.pid.lock
sudo service codedeploy-agent start
sudo service codedeploy-agent status
```

# Domain transfer research
We updated ns records in domain
Here is how u look up records live. removing 8.8.8.8 will look up records coming from your router
```bash
nslookup -type=A loantracker.zflashstudios.com 8.8.8.8
```
```bash
#kill a task running on port in mac
lsof -i :8081 | grep LISTEN | awk '{print $2}' | xargs kill -9
```

```bash
# if deployment stops most likely deploy script is killed
nohup python3 /home/ec2-user/app.py &
ps aux | grep app.py
tail -f nohup.out
#smaple curl
curl --location --request POST 'https://loandeploy.codewithzeeshan.com/deploy' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer 49cf14928048efa5569abb0b6330251d'
```

# Apple sign in setup
- Configured moneyrabbit app id to use apple sign in in apple developer account
- Added firebase server url as redirect url: https://money-rabbit-6024.firebaseapp.com/__/auth/handler
- Now need to create sign in with apple private key. This was keys section under certificates, ids and profiles. added a new key and enabled sign in with apple. Downloaded the key and saved it as AuthKey_XXXXXXXX.p8 in gdrive
- Now using blank xcode project to test firebase integration
- getting error: CoreStore.framework/_CodeSignature" failed: Operation not permitted #500
- Aparently its an open issue reported here: https://github.com/JohnEstropia/CoreStore/issues/500
  - solution: Change your project Build Settings->User Script Sandboxing from Yes to No can solve this issue.
  - while fixing this we ended up disabling csrutil security. we need to now run recovery mode by holding power button on start and type csrutil enable in terminal again.
  - we also added full disk access to xcode and terminal in system preferences.but that didn't help. so removing them now

we also added this part in podfile to fix the issue but didnt help:
```yaml
# ✅ Fully replace the rsync shell script to avoid SIP issues
post_install do |installer|
  installer.pods_project.targets.each do |target|
    target.build_phases.each do |phase|
      next unless phase.respond_to?(:name)
      next unless phase.name == '[CP] Embed Pods Frameworks'

      phase.shell_script = <<~EOS
        set -e
        set -u
        echo "✅ Using cp instead of rsync to avoid SIP issues"
        mkdir -p "${TARGET_BUILD_DIR}/${FRAMEWORKS_FOLDER_PATH}/"
        find "${BUILT_PRODUCTS_DIR}" -name '*.framework' -type d | while read framework; do
          cp -R "$framework" "${TARGET_BUILD_DIR}/${FRAMEWORKS_FOLDER_PATH}/"
        done
      EOS
    end
  end
end
```
µ
We had to add capabilities in xcode project to allow apple sign in. under signing and capabilities tab, we added sign in with apple capabilities

```bash
#allow docker commands without sudo
sudo usermod -aG docker ec2-user
newgrp docker
```

Installed on android trhough playstore. google login not working
trying to debug
on android device settings, about, build number pressed multiple times to enable developer mode
then search auto block in settings and turn that off than in developer options, enabled usb debugging.
now run this command 
```bash
# -s and device id to select device id
adb -s R5CY31JS5BT install app-release.apk
adb -s R5CY31JS5BT install app-debug.apk

adb -s R5CY31JS5BT logcat | grep -i com.zeenom
```

we made some progress. found this error log:
- One Tap sign-in failed: 10: [28444] Developer console is not set up correctly.

Most likely this means that we have not set up the SHA-1 fingerprint in firebase console.
To get the SHA-1 fingerprint, we can use the following command:
```bash
keytool -list -v -keystore ./user.keystore -alias zflash -storepass password -keypass password
```
found this sha-1
53:7B:63:BA:43:7A:11:B2:13:A0:F2:8D:EA:B2:28:EF:A2:A3:73:5E
added it to console
problem solved but only for release apks.
for playstore builds we need to add the sha-1 fingerprint of playstore managed certificate.
this can be found in test and release -> appintegrity -> appsigning section in play console.
copy the sha-1 and add to firebase console.

requesting new keystore cert
$ keytool -export -rfc -keystore user.keystore -alias zflash -storepass password -keypass password -file upload_certificate.pem
existing sha-1 for the game was:
38:A9:76:31:60:9F:D3:BC:87:B1:CA:18:58:07:E1:3B:FB:1E:C1:12
trying to change to correct one.

