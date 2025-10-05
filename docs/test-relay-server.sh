#!/bin/bash

# Simple test script for the WebSocket relay server
# Tests basic connectivity and message relay functionality

echo "WebSocket Relay Server Test"
echo "============================"
echo ""

# Check if websocat is available
if ! command -v websocat &> /dev/null; then
    echo "⚠️  websocat not found. Installing..."
    echo "You can install it with:"
    echo "  cargo install websocat"
    echo "  Or download from: https://github.com/vi/websocat"
    echo ""
    echo "Alternatively, test manually:"
    echo "1. Start the relay server: node relay-server.js"
    echo "2. Use a WebSocket client to connect to ws://localhost:8080/TEST123"
    echo "3. Send messages and verify they're relayed"
    exit 1
fi

# Check if relay server is running
echo "Testing connection to ws://localhost:8080..."
if ! websocat -t1 ws://localhost:8080/PING &> /dev/null; then
    echo "❌ Cannot connect to relay server at ws://localhost:8080"
    echo ""
    echo "Please start the relay server first:"
    echo "  node relay-server.js"
    echo "  or"
    echo "  python relay_server.py"
    exit 1
fi

echo "✅ Relay server is running!"
echo ""
echo "To test message relay:"
echo "1. Open two terminals"
echo "2. In terminal 1: websocat ws://localhost:8080/TEST123"
echo "3. In terminal 2: websocat ws://localhost:8080/TEST123"
echo "4. Type messages in either terminal"
echo "5. Verify they appear in the other terminal"
echo ""
echo "Example session messages:"
echo "  JOIN|Alice"
echo "  PRESENCE|Bob"
echo "  UPDATE|song123|0|true"
echo "  QUEUE|id1,id2,id3"
