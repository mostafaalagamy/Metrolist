# WebSocket Relay Server Setup Guide

This guide explains how to set up and run the WebSocket relay server for the Jam Session feature.

## What is the Relay Server?

The relay server is a simple WebSocket server that forwards messages between clients in the same session. It enables friends to listen to music together over the internet, not just on a local network.

## Quick Start

### Option 1: Node.js (Recommended)

1. **Install Node.js** from [nodejs.org](https://nodejs.org/)

2. **Navigate to the docs directory**:
   ```bash
   cd docs
   ```

3. **Install dependencies**:
   ```bash
   npm install ws
   ```

4. **Run the server**:
   ```bash
   node relay-server.js
   ```

The server will start on port 8080. You should see:
```
WebSocket relay server running on port 8080
```

### Option 2: Python

1. **Install Python 3.7+** from [python.org](https://www.python.org/)

2. **Navigate to the docs directory**:
   ```bash
   cd docs
   ```

3. **Install dependencies**:
   ```bash
   pip install websockets
   ```

4. **Run the server**:
   ```bash
   python relay_server.py
   ```

The server will start on port 8080. You should see:
```
WebSocket relay server running on port 8080
```

## Using with the App

### Local Network (Same WiFi)

If all devices are on the same WiFi network:

1. Find your computer's local IP address:
   - **Windows**: Open Command Prompt and run `ipconfig`, look for "IPv4 Address"
   - **Mac/Linux**: Open Terminal and run `ifconfig` or `ip addr`, look for your network adapter's IP
   - Example: `192.168.1.100`

2. The app will connect to: `ws://192.168.1.100:8080`

### Over the Internet

To allow friends outside your network to join:

#### Option A: Expose Local Server (Temporary)

Use a tunneling service like:
- **ngrok** (easiest): Download from [ngrok.com](https://ngrok.com/)
  ```bash
  ngrok http 8080
  ```
  You'll get a URL like `wss://abc123.ngrok.io` to share with friends

- **localhost.run**:
  ```bash
  ssh -R 80:localhost:8080 localhost.run
  ```

#### Option B: Cloud Deployment (Permanent)

Deploy the relay server to a cloud provider:

**Heroku**:
1. Create a `Procfile`:
   ```
   web: node relay-server.js
   ```
2. Create `package.json`:
   ```json
   {
     "name": "metrolist-relay",
     "version": "1.0.0",
     "dependencies": {
       "ws": "^8.0.0"
     }
   }
   ```
3. Deploy:
   ```bash
   git init
   git add .
   git commit -m "Initial commit"
   heroku create
   git push heroku main
   ```

**DigitalOcean/AWS/GCP**:
1. Create a VPS/VM instance
2. Install Node.js or Python
3. Copy the relay server file
4. Run the server (use `pm2` or `systemd` for persistence)
5. Configure firewall to allow port 8080

## Configuring the App

The app defaults to `ws://localhost:8080`. To use a different server URL, you'll need to update the `relayServerUrl` in the `JamSessionManager` class or add a UI setting to configure it.

## Server Logs

The server logs when clients connect and disconnect:
```
New connection to session: ABC123
Session ABC123 closed (no clients)
```

This helps you monitor active sessions.

## Troubleshooting

### Server won't start
- Check if port 8080 is already in use
- Try a different port by setting the `PORT` environment variable:
  ```bash
  PORT=3000 node relay-server.js
  ```

### Clients can't connect
- Make sure firewall allows connections on port 8080
- Verify the server is accessible from the client's network
- Check that you're using the correct IP address or domain

### Messages not relaying
- Check server logs for errors
- Verify clients are connecting to the same session code
- Ensure WebSocket connection is stable

## Security Considerations

This is a basic relay server suitable for personal use with friends. For production use, consider adding:

- Authentication/authorization
- Rate limiting
- Session timeouts
- SSL/TLS encryption (use `wss://` instead of `ws://`)
- Logging and monitoring

## Support

For issues or questions, please open an issue on the GitHub repository.
