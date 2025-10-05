# WebSocket Migration Guide

This document explains the migration from UDP broadcast to WebSocket relay server for the Jam Session feature.

## Overview

The Jam Session feature has been updated to use a WebSocket relay server instead of UDP broadcast. This change enables listening with friends over the internet, not just on local networks.

## Why This Change?

### Before (UDP Broadcast)
- ❌ Limited to local network only (same WiFi/LAN)
- ❌ Requires all devices on same network
- ❌ Firewall issues with UDP ports
- ✅ No server required

### After (WebSocket Relay)
- ✅ Works over the internet
- ✅ Listen with friends anywhere
- ✅ Standard WebSocket protocol
- ✅ Simple relay server (easy to run)
- ⚠️ Requires running a relay server

## Architecture Comparison

### UDP Broadcast (Old)
```
Device A (Host)  ----[UDP Broadcast]----> Local Network
                                                |
Device B (Client) <----[UDP Broadcast]----Local Network
Device C (Client) <----[UDP Broadcast]----Local Network
```

### WebSocket Relay (New)
```
Device A (Host)   ----[WebSocket]----> Relay Server <----[WebSocket]---- Device B (Client)
                                            |
                                       [WebSocket]
                                            |
                                       Device C (Client)
```

## Technical Changes

### Code Changes

**JamSessionManager.kt**:
- Replaced `DatagramSocket` with `HttpClient` + `WebSockets`
- Changed from UDP broadcast to WebSocket send/receive
- Removed port calculation and multicast addressing
- Added configurable relay server URL
- Maintained same message protocol

**Dependencies**:
- Added `ktor-client-websockets` to both app and kizzy modules
- Added `ktor-client-okhttp` to app module

### Message Protocol

The message protocol remains unchanged:
- `JOIN|username` - User joins session
- `PRESENCE|hostname` - Host announces presence
- `UPDATE|songId|position|isPlaying` - Playback state update
- `QUEUE|id1,id2,id3,...` - Queue update

This ensures backward compatibility during testing.

## Running the Relay Server

### Quick Start

**Node.js**:
```bash
cd docs
npm install ws
node relay-server.js
```

**Python**:
```bash
cd docs
pip install websockets
python relay_server.py
```

The server will run on `ws://localhost:8080` by default.

### Production Deployment

For internet access with friends, you can:

1. **Use ngrok** (easiest for testing):
   ```bash
   ngrok http 8080
   ```
   Share the generated URL with friends.

2. **Deploy to cloud**:
   - Heroku (free tier available)
   - DigitalOcean ($5/month)
   - AWS EC2
   - Google Cloud Platform
   - Any VPS with WebSocket support

See [RELAY_SERVER_SETUP.md](RELAY_SERVER_SETUP.md) for detailed instructions.

## Configuration

### Default Server URL

The app connects to `ws://localhost:8080` by default.

### Changing Server URL

To use a different server (e.g., for cloud deployment), you can call:
```kotlin
jamSessionManager.setRelayServerUrl("ws://your-server.com:8080")
```

Or update the default in `JamSessionManager.kt`:
```kotlin
private var relayServerUrl = "ws://your-server.com:8080"
```

## For Developers

### Relay Server Logic

The relay server is extremely simple:
1. Accept WebSocket connections at `/{sessionCode}`
2. Group connections by session code
3. When a message arrives, forward it to all other connections in the same session
4. Clean up disconnected clients

This simple design makes it easy to:
- Deploy anywhere
- Scale horizontally
- Add features (auth, logging, etc.)
- Understand and debug

### Testing Locally

1. Start the relay server:
   ```bash
   cd docs
   node relay-server.js
   ```

2. Build and run the app on devices/emulators

3. Create a session on one device, join from another

4. Observe server logs to see connections and message relays

### Message Flow Example

1. Device A creates session "ABC123"
   - Connects to `ws://localhost:8080/ABC123`
   - Sends: `PRESENCE|Alice`

2. Device B joins session "ABC123"
   - Connects to `ws://localhost:8080/ABC123`
   - Sends: `JOIN|Bob`
   - Receives: `PRESENCE|Alice` (relayed by server)

3. Device A plays a song
   - Sends: `UPDATE|songId123|0|true`
   - Device B receives: `UPDATE|songId123|0|true`

4. Both devices stay in sync!

## Migration Impact

### For Users
- No breaking changes to the UI
- Same session codes (6 characters)
- Same features (sync playback, queue, etc.)
- New capability: listen with friends over internet
- Requires relay server to be running

### For Contributors
- Simpler code (no UDP port management)
- Standard WebSocket protocol (well documented)
- Easy to extend (add encryption, auth, etc.)
- Better error handling with WebSocket events

## Future Enhancements

With WebSocket relay in place, future features become easier:
- End-to-end encryption
- Session authentication
- Chat between listeners
- Session history/replay
- Public/private session modes
- WebRTC upgrade for lower latency

## Support

For issues or questions:
- Check [RELAY_SERVER_SETUP.md](RELAY_SERVER_SETUP.md) for troubleshooting
- Open an issue on GitHub
- Review server logs for connection issues

## Credits

This migration was made possible by:
- Ktor WebSocket client library
- Node.js `ws` package
- Python `websockets` package
