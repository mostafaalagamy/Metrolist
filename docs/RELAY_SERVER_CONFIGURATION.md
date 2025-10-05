# Configuring the Relay Server URL

## Overview

The Jam Session feature now allows you to configure the WebSocket relay server URL directly in the app settings. This makes it easy to switch between local testing, local network sessions, and internet-based sessions with friends.

## Accessing Settings

1. Open the Metrolist app
2. Go to **Settings** (gear icon)
3. Navigate to **Integrations**
4. Select **Jam Session**
5. Enter your relay server URL

## Configuration Options

### Default (Local Testing)
```
ws://localhost:8080
```
Use this when:
- Testing the relay server on the same device
- Development and debugging

### Local Network (Same WiFi)
```
ws://192.168.1.100:8080
```
Replace `192.168.1.100` with your computer's local IP address.

Use this when:
- Friends are on the same WiFi network
- Home or office listening sessions
- No internet connection needed

**To find your IP:**
- **Windows**: `ipconfig` in Command Prompt
- **Mac/Linux**: `ifconfig` or `ip addr` in Terminal

### Internet (Friends Anywhere)

#### Using ngrok (Quick Testing)
```
wss://abc123.ngrok.io
```
1. Start relay server: `node relay-server.js`
2. Start ngrok: `ngrok http 8080`
3. Copy the ngrok URL and enter it in settings
4. Use secure WebSocket (`wss://`) not `ws://`

#### Using Cloud Deployment
```
wss://your-app.herokuapp.com
```
After deploying to Heroku, DigitalOcean, AWS, etc., enter your domain.

**Important:** Use `wss://` (secure WebSocket) for HTTPS domains.

## Changing the URL

The relay server URL can be changed at any time:

1. Leave any active session first
2. Go to **Settings > Integrations > Jam Session**
3. Update the URL
4. Create or join a new session

The new URL takes effect immediately for new sessions.

## Troubleshooting

### "Cannot connect to relay server"

**Check:**
- ✓ Relay server is running
- ✓ URL is correct (including `ws://` or `wss://`)
- ✓ Port is correct (default: 8080)
- ✓ Firewall allows connections
- ✓ For cloud: Server is deployed and accessible

**Test connection:**
```bash
# On the server machine
curl http://localhost:8080

# From another device (replace with your IP)
curl http://192.168.1.100:8080
```

### "Connection drops frequently"

**Solutions:**
- Use a stable internet connection
- Deploy server to a reliable cloud provider
- Check server logs for errors
- Ensure server has enough resources

### "Friends can't connect"

**For local network:**
- Ensure all devices are on the same WiFi
- Check router doesn't block connections
- Verify IP address is correct

**For internet:**
- Verify URL is accessible from outside your network
- For ngrok: Check tunnel is still active
- For cloud: Verify deployment is running

## Examples

### Home Network Session
1. Start relay server on your computer: `node relay-server.js`
2. Find your IP: `192.168.1.100`
3. Set URL in app: `ws://192.168.1.100:8080`
4. Share session code with friends on same WiFi

### Internet Session with ngrok
1. Start relay server: `node relay-server.js`
2. Start ngrok: `ngrok http 8080`
3. Copy ngrok URL: `https://abc123.ngrok.io`
4. Set URL in app: `wss://abc123.ngrok.io`
5. Share session code with friends anywhere

### Cloud Deployment
1. Deploy relay server to Heroku
2. Note your app URL: `https://my-jam-relay.herokuapp.com`
3. Set URL in app: `wss://my-jam-relay.herokuapp.com`
4. Share session code with friends worldwide

## Security Notes

- Use `wss://` (secure WebSocket) when connecting over the internet
- For production, consider adding authentication to your relay server
- Keep your relay server updated
- Monitor server logs for unusual activity

## See Also

- [Relay Server Setup Guide](RELAY_SERVER_SETUP.md) - Complete deployment instructions
- [WebSocket Migration Guide](WEBSOCKET_MIGRATION.md) - Technical details
- [Jam Session Feature](JAM_SESSION_FEATURE.md) - Feature overview
