# Jam Session Feature - UI Guide

## Where to Find the Jam Button

The Jam Session button (account icon) is located in the music player controls area.

### Classic Player Design

```
┌─────────────────────────────────────┐
│         🎵 Song Title               │
│         👤 Artist Name              │
├─────────────────────────────────────┤
│                                     │
│   [Share] [Jam👤] [Menu ⋮]         │
│                                     │
│   ▬▬▬▬▬▬▬▬●▬▬▬▬▬▬▬▬  Slider        │
│   0:45              3:24            │
│                                     │
│   [⏮️] [▶️] [⏭️]                    │
└─────────────────────────────────────┘
          ↑
    Jam Button
```

### New Player Design

```
┌─────────────────────────────────────┐
│         🎵 Song Title               │
│         👤 Artist Name              │
├─────────────────────────────────────┤
│                                     │
│   [Share📤][❤️][Jam👤]              │
│                                     │
│   ▬▬▬▬▬▬▬▬●▬▬▬▬▬▬▬▬  Slider        │
│   0:45              3:24            │
│                                     │
│   [⏮️] [▶️] [⏭️]                    │
└─────────────────────────────────────┘
              ↑
        Jam Button
```

## Button States

### Inactive (Not in Session)
```
┌──────┐
│ 👤   │  Standard button color
│      │  Click to create/join
└──────┘
```

### Active (In Session)
```
┌──────┐
│ 👤   │  Highlighted in primary color
│      │  Click to view session info
└──────┘
```

## Dialog Flows

### 1. Initial Dialog (Not in Session)

```
╔═══════════════════════════════╗
║     📤 Spotify Jam            ║
╟───────────────────────────────╢
║ Listen together with friends! ║
║                               ║
║ ┌──────────────────────────┐ ║
║ │ Create Jam Session       │ ║
║ └──────────────────────────┘ ║
║                               ║
║ ┌──────────────────────────┐ ║
║ │ Join Jam Session         │ ║
║ └──────────────────────────┘ ║
║                               ║
║              [Cancel]         ║
╚═══════════════════════════════╝
```

### 2. Join Session Dialog

```
╔═══════════════════════════════╗
║     📤 Spotify Jam            ║
╟───────────────────────────────╢
║ Listen together with friends! ║
║                               ║
║ ┌──────────────────────────┐ ║
║ │ Your Name: ___________   │ ║
║ └──────────────────────────┘ ║
║                               ║
║ ┌──────────────────────────┐ ║
║ │ Session Code: ______     │ ║
║ └──────────────────────────┘ ║
║                               ║
║ ┌──────────────────────────┐ ║
║ │   Join Session           │ ║
║ └──────────────────────────┘ ║
║                               ║
║          [Back]               ║
╚═══════════════════════════════╝
```

### 3. Active Session Dialog

```
╔═══════════════════════════════╗
║   📤 Active Jam Session       ║
╟───────────────────────────────╢
║                               ║
║    ┌───────────────────┐     ║
║    │     ABC123        │     ║
║    │  (Session Code)   │     ║
║    └───────────────────┘     ║
║                               ║
║      You are the host         ║
║                               ║
║      3 participant(s)         ║
║                               ║
║ ┌──────────────────────────┐ ║
║ │  Copy Session Code       │ ║
║ └──────────────────────────┘ ║
║                               ║
║    [Leave Session] [Close]    ║
╚═══════════════════════════════╝
```

## User Journey

### As Host
```
1. Tap Jam Button (👤)
   ↓
2. Dialog Opens
   ↓
3. Tap "Create Jam Session"
   ↓
4. Code Generated (e.g., ABC123)
   ↓
5. Code Auto-Copied to Clipboard
   ↓
6. Toast: "Session created! Code copied: ABC123"
   ↓
7. Share code with friends via messaging app
   ↓
8. Button now highlighted (active session)
```

### As Participant
```
1. Receive code from friend (e.g., ABC123)
   ↓
2. Tap Jam Button (👤)
   ↓
3. Dialog Opens
   ↓
4. Tap "Join Jam Session"
   ↓
5. Enter Your Name
   ↓
6. Enter Session Code (ABC123)
   ↓
7. Tap "Join Session"
   ↓
8. Toast: "Joined session ABC123"
   ↓
9. Button now highlighted (active session)
```

## Quick Actions

### While in Active Session

**View Session Info**
- Tap highlighted Jam button
- See session code, host, and participant count

**Share Code Again**
- Open session dialog
- Tap "Copy Session Code"
- Code copied to clipboard

**Leave Session**
- Open session dialog
- Tap "Leave Session"
- Button returns to normal state

## Tips

💡 **Session Code**: Always 6 uppercase letters/numbers (e.g., ABC123, XYZ789)

💡 **Clipboard**: Code is automatically copied when created - just paste in your messaging app

💡 **Visual Feedback**: Watch for the button color change to know you're in a session

💡 **Quick Share**: Use any messaging app (WhatsApp, Telegram, SMS) to share the code

💡 **Multiple Participants**: Share the code with as many friends as you want!

## Troubleshooting

**Q: I can't find the Jam button**
- A: Look for the account icon (👤) in the player controls area, between the share and menu buttons (classic) or after the heart icon (new design)

**Q: The button isn't highlighted**
- A: You're not in an active session. Tap it to create or join one!

**Q: Code didn't copy to clipboard**
- A: Try tapping "Copy Session Code" in the session info dialog

**Q: Want to start a new session**
- A: Leave your current session first, then create a new one

## Accessibility

- All buttons have content descriptions
- Dialog supports screen readers
- Keyboard navigation compatible
- High contrast mode supported
