// Example WebSocket client for testing the relay server
// This demonstrates how the Android app communicates with the relay

const WebSocket = require('ws');

// Configuration
const RELAY_URL = 'ws://localhost:8080';
const SESSION_CODE = 'TEST123';
const USER_NAME = process.argv[2] || 'TestUser';

// Connect to relay server
const ws = new WebSocket(`${RELAY_URL}/${SESSION_CODE}`);

ws.on('open', () => {
    console.log(`✅ Connected to session: ${SESSION_CODE}`);
    console.log(`👤 User: ${USER_NAME}`);
    console.log('');
    
    // Announce presence (like the app does)
    const joinMessage = `JOIN|${USER_NAME}`;
    ws.send(joinMessage);
    console.log(`📤 Sent: ${joinMessage}`);
    console.log('');
    console.log('Listening for messages... (Ctrl+C to exit)');
    console.log('');
});

ws.on('message', (data) => {
    const message = data.toString();
    console.log(`📥 Received: ${message}`);
    
    // Parse and display message
    const parts = message.split('|');
    switch(parts[0]) {
        case 'JOIN':
            console.log(`   → ${parts[1]} joined the session`);
            break;
        case 'PRESENCE':
            console.log(`   → Host is ${parts[1]}`);
            break;
        case 'UPDATE':
            console.log(`   → Playback update: song=${parts[1]}, position=${parts[2]}, playing=${parts[3]}`);
            break;
        case 'QUEUE':
            console.log(`   → Queue updated: ${parts[1]}`);
            break;
    }
    console.log('');
});

ws.on('close', () => {
    console.log('❌ Disconnected from server');
});

ws.on('error', (error) => {
    console.error('❌ Error:', error.message);
});

// Example: Send a test message after 2 seconds
setTimeout(() => {
    if (ws.readyState === WebSocket.OPEN) {
        const testMessage = `UPDATE|test-song-id|12345|true`;
        ws.send(testMessage);
        console.log(`📤 Sent test message: ${testMessage}`);
        console.log('');
    }
}, 2000);

// Handle Ctrl+C gracefully
process.on('SIGINT', () => {
    console.log('');
    console.log('👋 Closing connection...');
    ws.close();
    process.exit(0);
});
