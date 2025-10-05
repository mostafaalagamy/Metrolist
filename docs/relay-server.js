const WebSocket = require('ws');
const http = require('http');

const server = http.createServer();
const wss = new WebSocket.Server({ server });

// Store sessions: sessionCode -> Set of WebSocket connections
const sessions = new Map();

wss.on('connection', (ws, req) => {
    const sessionCode = req.url.slice(1); // Remove leading '/'
    console.log(`New connection to session: ${sessionCode}`);
    
    if (!sessionCode) {
        ws.close();
        return;
    }
    
    // Add client to session
    if (!sessions.has(sessionCode)) {
        sessions.set(sessionCode, new Set());
    }
    sessions.get(sessionCode).add(ws);
    
    ws.on('message', (message) => {
        // Relay message to all other clients in the same session
        const sessionClients = sessions.get(sessionCode);
        if (sessionClients) {
            sessionClients.forEach((client) => {
                if (client !== ws && client.readyState === WebSocket.OPEN) {
                    client.send(message);
                }
            });
        }
    });
    
    ws.on('close', () => {
        // Remove client from session
        const sessionClients = sessions.get(sessionCode);
        if (sessionClients) {
            sessionClients.delete(ws);
            if (sessionClients.size === 0) {
                sessions.delete(sessionCode);
                console.log(`Session ${sessionCode} closed (no clients)`);
            }
        }
    });
});

const PORT = process.env.PORT || 8080;
server.listen(PORT, () => {
    console.log(`WebSocket relay server running on port ${PORT}`);
});
