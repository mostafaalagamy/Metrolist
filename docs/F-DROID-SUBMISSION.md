# F-Droid Submission Guide for Metrolist

This document provides a step-by-step guide for submitting Metrolist to F-Droid.

## Prerequisites Checklist

- [x] **Open Source License**: GPL-3.0-only
- [x] **Source Code**: Available on GitHub
- [x] **Free Software**: No proprietary dependencies
- [x] **Build from Source**: Gradle build system
- [x] **Metadata**: Fastlane metadata available
- [x] **Screenshots**: Available in fastlane/metadata
- [x] **Icon**: High-quality icon available

## Submission Steps

### 1. Fork F-Droid Data Repository

1. Go to https://gitlab.com/fdroid/fdroiddata
2. Fork the repository to your GitLab account
3. Clone your fork locally:
   ```bash
   git clone https://gitlab.com/YOUR_USERNAME/fdroiddata.git
   cd fdroiddata
   ```

### 2. Create Metadata File

1. Copy the metadata file from this repository:
   ```bash
   cp metadata/com.metrolist.music.yml fdroiddata/metadata/
   ```

2. Review and adjust the metadata file if needed

### 3. Test Local Build

1. Install F-Droid server tools:
   ```bash
   sudo apt-get install fdroidserver
   ```

2. Test building the app:
   ```bash
   fdroid build com.metrolist.music:123
   ```

### 4. Submit Merge Request

1. Add and commit the metadata file:
   ```bash
   git add metadata/com.metrolist.music.yml
   git commit -m "Add Metrolist music player

   Material 3 YouTube Music client for Android with offline playback,
   library management, and synchronization features."
   ```

2. Push to your fork:
   ```bash
   git push origin master
   ```

3. Create a merge request on GitLab targeting the main fdroiddata repository

### 5. Follow Up

- Monitor the merge request for feedback from F-Droid maintainers
- Address any issues or requested changes
- Be patient as the review process can take several weeks

## App Information

- **Package Name**: com.metrolist.music
- **Current Version**: 12.2.0 (123)
- **Category**: Multimedia
- **License**: GPL-3.0-only
- **Min SDK**: 26
- **Target SDK**: 36

## Description

Material 3 YouTube Music client for Android with the following features:

- Play any song or video from YT Music
- Background playback
- Personalized quick picks
- Library management
- Download and cache songs for offline playback
- Search for songs, albums, artists, videos and playlists
- Live lyrics
- YouTube Music account login support
- Syncing of songs, artists, albums and playlists
- Skip silence
- Import playlists
- Audio normalization
- Adjust tempo/pitch
- Local playlist management
- Reorder songs in playlist or queue
- Light/Dark/Black/Dynamic themes
- Sleep timer
- Material 3 design

## Build Requirements

- Android SDK 36
- JDK 17 or higher
- Gradle 8.13.0+
- 2GB+ RAM for building

## Notes

- The app requires network access to stream from YouTube Music
- Local playback works offline for downloaded songs
- No tracking or analytics included
- Follows Material 3 design guidelines