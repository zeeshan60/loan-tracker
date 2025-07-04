```nginx
# Redirect all HTTP traffic to HTTPS for main domain
server {
    listen 80;
    server_name loanapp.codewithzeeshan.com loanapi.codewithzeeshan.com;
    return 301 https://$host$request_uri;
}

# Frontend (Main Domain)
server {
    listen 443 ssl;
    server_name loanapp.codewithzeeshan.com;

    ssl_certificate /etc/letsencrypt/live/loanapp.codewithzeeshan.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/loanapp.codewithzeeshan.com/privkey.pem;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;

    # Route all traffic to frontend (port 8081)
    location / {
        proxy_pass http://127.0.0.1:8081/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}

# Deploy webhook
server {
    listen 443 ssl;
    server_name loandeploy.codewithzeeshan.com;

    ssl_certificate /etc/letsencrypt/live/loandeploy.codewithzeeshan.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/loandeploy.codewithzeeshan.com/privkey.pem;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;

    # Route all traffic to deploy (port 5000)
    location / {
        proxy_pass http://127.0.0.1:5000/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}

# Backend API (Subdomain)
server {
    listen 443 ssl;
    server_name loanapi.codewithzeeshan.com;

    ssl_certificate /etc/letsencrypt/live/loanapi.codewithzeeshan.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/loanapi.codewithzeeshan.com/privkey.pem;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;

    # Route API requests to backend service (port 8080)
    location / {
        proxy_pass http://127.0.0.1:8080/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```

```bash
sudo yum install -y certbot python3-certbot-nginx

sudo certbot certonly --standalone -d loanapi.codewithzeeshan.com
sudo certbot certonly --standalone -d loanapp.codewithzeeshan.com
sudo certbot certonly --standalone -d loandeploy.codewithzeeshan.com

sudo certbot certonly --standalone -d loanapi-prod.codewithzeeshan.com
sudo certbot certonly --standalone -d loanapp-prod.codewithzeeshan.com
sudo certbot certonly --standalone -d loandeploy-prod.codewithzeeshan.com

sudo yum install -y nginx
sudo systemctl enable nginx
#add nginx config in this file
sudo rm /etc/nginx/conf.d/reverse-proxy.conf
sudo nano /etc/nginx/conf.d/reverse-proxy.conf

#make sure port 80 is free
sudo systemctl start nginx

sudo systemctl stop nginx
sudo systemctl start nginx
sudo nginx -t
sudo systemctl restart nginx

#Renwe certificates
sudo systemctl stop nginx
sudo certbot renew
sudo systemctl start nginx

#script to cron certs renewal
nano renew_cert.sh
#!/bin/bash
sudo systemctl stop nginx
sudo certbot renew
sudo systemctl start nginx

cat <<'EOF' > renew_cert.sh
#!/bin/bash
sudo systemctl stop nginx
sudo certbot renew
sudo systemctl start nginx
EOF

chmod +x renew_cert.sh

sudo crontab -e
0 2 1 * * /home/ec2-user/renew_cert.sh

#install crontab if not available
sudo yum install -y cronie
sudo systemctl enable crond
sudo systemctl start crond

#check expiry
sudo openssl x509 -enddate -noout -in /etc/letsencrypt/live/loanapi.codewithzeeshan.com/fullchain.pem

#check existing crons
sudo crontab -l
#check cron schedule using given format
https://crontab.guru/
```