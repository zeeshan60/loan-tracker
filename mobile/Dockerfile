# Use a Node.js base image suitable for your Ionic version.
# Alpine is smaller, but can have compatibility issues with some native dependencies.
# Debian or Ubuntu are more common and generally more compatible.

# For Ionic 6 and later, Node 16 or later is recommended. Adjust as needed.
FROM node:20-alpine AS build-stage

# Set the working directory inside the container
WORKDIR /app

# Copy package.json and package-lock.json (if it exists) first for caching
COPY package*.json ./

# Install project dependencies.  Use --omit=dev for production builds.
RUN npm install --omit=dev --force  # Or npm ci if you have a package-lock.json

# Install Angular CLI globally
RUN npm install -g @angular/cli@latest
RUN npm install @angular/cdk@latest
RUN npm install @angular/material@latest

# Copy the rest of the application source code
COPY . .

# Define a build argument for the build configuration
ARG BUILD_CONFIG=--prod

# Build the Ionic app for production.  Use the appropriate command for your project.
RUN npm run build $BUILD_CONFIG

# --- Production Stage ---
# Use a smaller image for the final deployment.  Nginx is a common choice for serving static files.
FROM nginx:alpine

# Copy the built Ionic app from the build stage to the Nginx HTML directory.
COPY --from=build-stage /app/www /usr/share/nginx/html

COPY nginx.conf /etc/nginx/conf.d/default.conf

# Expose the port your app is running on (usually 80 for web apps).
EXPOSE 80

# (Optional) Add any necessary Nginx configuration if needed.
# COPY nginx.conf /etc/nginx/conf.d/default.conf

# Start Nginx
CMD ["nginx", "-g", "daemon off;"]
