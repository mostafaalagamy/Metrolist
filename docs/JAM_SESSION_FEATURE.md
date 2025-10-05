# Spotify Jam Feature

## Overview
The Spotify Jam feature allows users to listen to music together with friends. It's a simple, lightweight implementation that enables creating and joining listening sessions without requiring complex backend infrastructure.

## Features

### 1. Create a Jam Session
- Users can create a new jam session by clicking the jam button (account icon) in the player
- A unique 6-character session code is automatically generated
- The session code is automatically copied to the clipboard for easy sharing
- The host can share this code with friends

### 2. Join a Jam Session
- Users can join an existing session by entering:
  - Their name
  - The 6-character session code shared by the host
- Once joined, they become part of the listening session

### 3. Visual Indicators
- The jam button changes color when you're in an active session
  - Inactive: Uses standard button color
  - Active: Highlights with primary container color
- Both new and classic player designs are supported

### 4. Session Management
- View current session information including:
  - Session code
  - Host information
  - Number of participants
- Copy session code to clipboard at any time
- Leave session with a single button click

## Technical Implementation

### Components

#### JamSessionManager (`utils/JamSessionManager.kt`)
- Manages session state using Kotlin StateFlow
- Handles session creation, joining, and leaving
- Generates unique session codes
- Tracks host status and participant information

#### JamSessionDialog (`ui/component/JamSessionDialog.kt`)
- Material 3 dialog UI for session management
- Two modes:
  - Session creation/joining screen (when not in a session)
  - Session info screen (when in an active session)
- Clipboard integration for easy code sharing
- Toast notifications for user feedback

#### PlayerConnection Integration
- JamSessionManager instance added to PlayerConnection
- Available throughout the app via LocalPlayerConnection

#### Player UI Integration
- Jam button added to both classic and new player designs
- Button highlights when in active session
- Opens JamSessionDialog on click

## Usage

### As a Host:
1. Open the player and click the jam button (account icon)
2. Click "Create Jam Session"
3. Your session code is automatically copied to clipboard
4. Share the code with friends via any messaging app

### As a Participant:
1. Get the session code from your friend
2. Click the jam button in the player
3. Click "Join Jam Session"
4. Enter your name and the session code
5. Click "Join Session"

### Managing Your Session:
- Click the jam button while in a session to view session info
- Use "Copy Session Code" to share with more friends
- Click "Leave Session" to exit

## Future Enhancements
Potential improvements for future versions:
- Real-time playback synchronization
- Server-based session management
- In-app messaging between participants
- Session history and favorites
- Playlist collaboration
- Vote skipping functionality

## Notes
- Current implementation uses in-memory storage for simplicity
- Sessions are local to each device
- No database migrations required
- Minimal code changes for easy maintenance
