---
description: How to update the GNS application on the server
---

This workflow guides you through updating the deployed GNS application with the latest changes.

## 1. Local Build

First, we need to build the latest version of both backend and frontend on your local machine.

### Backend (Java)
```bash
./mvnw clean package -DskipTests
```
*Expected Output*: `BUILD SUCCESS`
*Artifact*: `target/general-notification-system-0.0.1-SNAPSHOT.jar`

### Frontend (Vue)
```bash
cd frontend
npm install
npm run build
cd ..
```
*Artifact*: `frontend/dist/` directory

## 2. Upload Artifacts

Upload the built artifacts to your server using `scp`. Replace `user@your-server-ip` with your actual server login.

### Upload Backend
```bash
scp target/general-notification-system-0.0.1-SNAPSHOT.jar user@your-server-ip:/tmp/gns-api.jar
```

### Upload Frontend
```bash
scp -r frontend/dist/* user@your-server-ip:/tmp/gns-frontend-dist/
```
*(Note: It's safer to upload to `/tmp` first and then move them on the server to avoid permission issues during transfer)*

## 3. Apply Updates on Server

SSH into your server and replace the old files.

```bash
ssh user@your-server-ip
```

### Update Backend
```bash
# Stop service
sudo systemctl stop gns-api

# Backup old jar (optional)
sudo mv /var/www/gns/backend/gns-api.jar /var/www/gns/backend/gns-api.jar.bak

# Move new jar
sudo mv /tmp/gns-api.jar /var/www/gns/backend/gns-api.jar

# Restart service
sudo systemctl start gns-api
```

### Update Frontend
```bash
# Clear old files
sudo rm -rf /var/www/gns/html/*

# Move new files (assuming you uploaded to a folder in tmp)
# If you uploaded individual files to /tmp/gns-frontend-dist/
sudo cp -r /tmp/gns-frontend-dist/* /var/www/gns/html/
```

### Verify
check the logs to ensure the new backend started correctly:
```bash
sudo journalctl -u gns-api -f
```
