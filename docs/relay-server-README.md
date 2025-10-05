# Jam Session Relay Server

This directory contains the WebSocket relay server implementations for the Jam Session feature.

## Quick Start

Choose either Node.js or Python:

### Node.js (Recommended)
```bash
npm install ws
node relay-server.js
```

### Python
```bash
pip install websockets
python relay_server.py
```

## Full Documentation

See [RELAY_SERVER_SETUP.md](RELAY_SERVER_SETUP.md) for complete setup instructions, including:
- Running the server locally
- Exposing the server over the internet
- Cloud deployment options
- Troubleshooting tips

## What This Does

The relay server enables the Jam Session feature to work over the internet, not just on local networks. It:
- Accepts WebSocket connections from app clients
- Groups clients by session code
- Relays messages between all clients in the same session
- Handles client disconnections gracefully

This allows you and your friends to listen to music together, no matter where you are!
