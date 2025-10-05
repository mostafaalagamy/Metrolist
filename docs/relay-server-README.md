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

The server will start on `ws://localhost:8080` by default.

## Testing the Server

### Option 1: Test Script
```bash
./test-relay-server.sh
```

### Option 2: Example Client
```bash
# Terminal 1
node example-websocket-client.js Alice

# Terminal 2
node example-websocket-client.js Bob
```

Watch messages relay between the two clients!

## Full Documentation

See [RELAY_SERVER_SETUP.md](RELAY_SERVER_SETUP.md) for complete setup instructions, including:
- Running the server locally
- Exposing the server over the internet
- Cloud deployment options
- Troubleshooting tips

See [WEBSOCKET_MIGRATION.md](WEBSOCKET_MIGRATION.md) for technical details about the WebSocket implementation.

## What This Does

The relay server enables the Jam Session feature to work over the internet, not just on local networks. It:
- Accepts WebSocket connections from app clients
- Groups clients by session code
- Relays messages between all clients in the same session
- Handles client disconnections gracefully

This allows you and your friends to listen to music together, no matter where you are!

## Files in This Directory

- `relay-server.js` - Node.js relay server implementation
- `relay_server.py` - Python relay server implementation
- `example-websocket-client.js` - Example client for testing
- `test-relay-server.sh` - Simple connectivity test script
- `RELAY_SERVER_SETUP.md` - Complete setup guide
- `WEBSOCKET_MIGRATION.md` - Technical migration details
- `JAM_SESSION_FEATURE.md` - Feature documentation
