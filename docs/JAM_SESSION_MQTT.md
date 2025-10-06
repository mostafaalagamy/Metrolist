# Jam Session with MQTT

## Overview

The Jam Session feature uses MQTT (Message Queuing Telemetry Transport) for real-time synchronization between users. MQTT is a lightweight messaging protocol perfect for scenarios like this where multiple devices need to communicate efficiently.

## Why MQTT?

- **Easy to set up**: Many public MQTT brokers available (HiveMQ, Eclipse, Mosquitto)
- **Scalable**: Can handle many users in multiple sessions
- **Flexible**: Works over the internet, not limited to local networks
- **Reliable**: QoS (Quality of Service) levels ensure message delivery
- **Topic-based**: Each jam session uses its own MQTT topic (jam room)

## How It Works

### Topics as Jam Rooms

Each jam session code becomes a unique MQTT topic:
```
metrolist/jam/ABC123
```

When you create or join a session with code "ABC123", all participants subscribe to this topic. Any playback changes broadcast to this topic are received by all subscribers in real-time.

### Message Format

The same message protocol is maintained:
- `JOIN|username` - User joins the session
- `PRESENCE|hostname` - Host announces presence
- `UPDATE|songId|position|isPlaying` - Playback state update
- `QUEUE|id1,id2,id3,...` - Queue synchronization

## Configuration

### In-App Settings

1. Open **Settings** → **Integrations** → **Jam Session**
2. Configure your MQTT Broker URL
3. Default: `tcp://broker.hivemq.com:1883` (public broker)

### MQTT Broker Options

#### Public Brokers (Free)

**HiveMQ Public Broker** (Default):
```
tcp://broker.hivemq.com:1883
```

**Eclipse Mosquitto**:
```
tcp://test.mosquitto.org:1883
```

**EMQX Public Broker**:
```
tcp://broker.emqx.io:1883
```

#### Self-Hosted Brokers

##### Option 1: Mosquitto (Recommended)

Install Mosquitto MQTT broker:

**Linux/Ubuntu**:
```bash
sudo apt-get install mosquitto mosquitto-clients
sudo systemctl start mosquitto
sudo systemctl enable mosquitto
```

**macOS**:
```bash
brew install mosquitto
brew services start mosquitto
```

**Docker**:
```bash
docker run -d -p 1883:1883 eclipse-mosquitto
```

Then configure your broker URL:
```
tcp://your-server-ip:1883
```

##### Option 2: HiveMQ Community Edition

Download from: https://www.hivemq.com/downloads/community/

##### Option 3: EMQX

Download from: https://www.emqx.io/downloads

### Cloud Deployment

#### AWS IoT Core
1. Create an AWS account
2. Set up AWS IoT Core
3. Configure the broker endpoint

#### Azure IoT Hub
1. Create an Azure account
2. Set up IoT Hub
3. Use the connection string as broker URL

#### CloudMQTT
1. Sign up at https://www.cloudmqtt.com/
2. Create an instance
3. Use the provided server URL

## Security Considerations

### Public Brokers
- Anyone can subscribe to your topic if they know the session code
- Messages are not encrypted by default
- Suitable for casual use with friends

### Private Brokers
For better security with your own broker:

1. **Enable Authentication**:
   ```bash
   mosquitto_passwd -c /etc/mosquitto/passwd username
   ```

2. **Enable TLS/SSL**:
   - Use `ssl://` or `wss://` instead of `tcp://`
   - Configure certificates

3. **Access Control Lists (ACL)**:
   - Restrict topic access by user

## Troubleshooting

### Cannot connect to broker

**Check**:
- Broker URL is correct
- Broker is running and accessible
- Port 1883 (or 8883 for SSL) is not blocked by firewall
- Internet connection is stable

**Test connection**:
```bash
# Install mosquitto-clients
sudo apt-get install mosquitto-clients

# Test connection
mosquitto_sub -h broker.hivemq.com -p 1883 -t "test/topic"
```

### Messages not syncing

**Check**:
- All devices are using the same broker URL
- All devices are in the same session (same session code)
- Broker is accessible from all devices
- Check app logs for connection errors

### High latency

**Solutions**:
- Use a broker closer to your location
- Deploy your own broker in your region
- Use a paid/premium broker service
- Check your internet connection speed

## Best Practices

1. **For friends on same network**: Use a local Mosquitto broker for lowest latency
2. **For internet sessions**: Use a reliable public broker or deploy your own
3. **For regular use**: Consider setting up your own broker with authentication
4. **For privacy**: Use a private broker with TLS/SSL

## Advanced: Running Your Own Broker

### Simple Mosquitto Setup

1. **Install Mosquitto**:
   ```bash
   sudo apt-get install mosquitto
   ```

2. **Configure** (`/etc/mosquitto/mosquitto.conf`):
   ```
   listener 1883
   allow_anonymous true
   ```

3. **Start the service**:
   ```bash
   sudo systemctl restart mosquitto
   ```

4. **Configure in app**:
   ```
   tcp://your-ip-address:1883
   ```

### Docker Compose Setup

Create `docker-compose.yml`:
```yaml
version: '3'
services:
  mosquitto:
    image: eclipse-mosquitto
    ports:
      - "1883:1883"
    volumes:
      - ./mosquitto.conf:/mosquitto/config/mosquitto.conf
    restart: unless-stopped
```

Create `mosquitto.conf`:
```
listener 1883
allow_anonymous true
```

Run:
```bash
docker-compose up -d
```

## FAQ

**Q: Can I use any MQTT broker?**  
A: Yes, any MQTT v3.1.1 compatible broker works.

**Q: Is there a limit to how many people can join?**  
A: Depends on your broker. Public brokers typically allow 100+ connections. Your own broker is only limited by your server resources.

**Q: Do I need to keep the broker running?**  
A: Yes, the broker must be running for sessions to work. Public brokers are always available.

**Q: Can I use MQTT over websockets?**  
A: Currently, the app uses TCP. WebSocket support can be added in the future.

**Q: How much data does it use?**  
A: Very little! MQTT is extremely lightweight. Typical usage is only a few KB per session.

## Support

For issues or questions:
- Check the broker connection in app logs
- Test the broker with `mosquitto_sub` command
- Try a different public broker
- Open an issue on GitHub
