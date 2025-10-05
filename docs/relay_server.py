import asyncio
import websockets
from collections import defaultdict

# Store sessions: session_code -> set of websocket connections
sessions = defaultdict(set)

async def handle_client(websocket, path):
    session_code = path.strip('/')
    if not session_code:
        await websocket.close()
        return
    
    print(f"New connection to session: {session_code}")
    sessions[session_code].add(websocket)
    
    try:
        async for message in websocket:
            # Relay message to all other clients in the same session
            for client in sessions[session_code]:
                if client != websocket and not client.closed:
                    await client.send(message)
    finally:
        sessions[session_code].discard(websocket)
        if not sessions[session_code]:
            del sessions[session_code]
            print(f"Session {session_code} closed (no clients)")

async def main():
    port = 8080
    async with websockets.serve(handle_client, "0.0.0.0", port):
        print(f"WebSocket relay server running on port {port}")
        await asyncio.Future()  # run forever

if __name__ == "__main__":
    asyncio.run(main())
