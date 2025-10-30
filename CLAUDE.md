# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Metrolist** is a native Android music streaming application - a YouTube Music client that allows users to stream and download music from YouTube Music without the official app. The project is a fork of InnerTune by Zion Huang and OuterTune by Davide Garberi.

- **Package**: `com.metrolist.music`
- **Min SDK**: 26 (Android 8.0)
- **Target/Compile SDK**: 36
- **License**: GPL v3.0

## Build Commands

The project uses Gradle with Kotlin DSL and requires JDK 21.

### Standard Build Commands

```bash
# Build all variants (5 ABIs × 2 build types = 10 APKs)
./gradlew assemble

# Build specific ABI variants
./gradlew assembleUniversalRelease  # All ABIs in one APK
./gradlew assembleArm64Release      # ARM 64-bit only
./gradlew assembleArmeabiRelease    # ARM 32-bit only
./gradlew assembleX86Release        # x86 32-bit only
./gradlew assembleX86_64Release     # x86 64-bit only

# Build debug variants
./gradlew assembleUniversalDebug
./gradlew assembleArm64Debug
# etc.

# Clean build
./gradlew clean
```

### Linting

```bash
# Run lint on default variant
./gradlew lint

# Run lint on specific variants
./gradlew lintUniversalDebug
./gradlew lintUniversalRelease
./gradlew lintArm64Release

# Run lint and apply safe fixes
./gradlew lintFix
```

### Testing

```bash
# Run all unit tests
./gradlew test

# Run instrumented tests on connected device
./gradlew connectedCheck
./gradlew connectedUniversalDebugAndroidTest

# Run all checks (lint + tests)
./gradlew check
```

### Development Build

For rapid development iteration, use the universal debug variant:

```bash
./gradlew assembleUniversalDebug
```

The debug APK will be at: `app/build/outputs/apk/universal/debug/app-universal-debug.apk`

## Architecture

### Tech Stack

- **Language**: Kotlin with coroutines
- **UI**: Jetpack Compose with Material 3 (declarative UI)
- **Architecture**: MVVM with single-activity navigation
- **DI**: Hilt for dependency injection
- **Database**: Room (schema version 25 with 24 migrations)
- **Media**: Media3 (ExoPlayer) with MediaSession for playback
- **Networking**: Ktor client with OkHttp engine
- **Image Loading**: Coil 3
- **Async**: Kotlin Coroutines + Flow

### Multi-Module Structure

The project is organized into multiple Gradle modules:

1. **app** - Main Android application module
   - All UI components (Compose screens, ViewModels, themes)
   - Room database with DAOs and entities
   - Media playback service
   - Hilt DI modules

2. **innertube** - YouTube InnerTube API client (pure Kotlin/JVM)
   - Handles communication with YouTube Music's private API
   - Independent of Android framework

3. **kugou** - KuGou lyrics provider integration
   - Chinese lyrics database provider

4. **lrclib** - LRCLib lyrics provider
   - Open-source synced lyrics database

5. **kizzy** - Discord Rich Presence integration
   - Shows currently playing music on Discord

6. **lastfm** - Last.fm scrobbling client
   - Requires API keys (optional for local builds)

### Key Directories

```
app/src/main/kotlin/com/metrolist/music/
├── App.kt                    # Application class with Hilt setup
├── MainActivity.kt           # Single activity with Compose
├── db/                       # Room database
│   ├── MusicDatabase.kt      # Database definition (25 migrations!)
│   ├── entities/             # Database entities (Song, Artist, Album, Playlist, etc.)
│   └── DatabaseDao.kt        # All database queries
├── di/                       # Hilt dependency injection modules
├── playback/                 # Media3 playback service
│   ├── MediaSessionService.kt
│   └── ExoPlayer setup
├── ui/
│   ├── component/            # Reusable Compose components
│   ├── screens/              # Feature screens (Home, Library, Search, etc.)
│   ├── player/               # Music player UI
│   ├── menu/                 # Context menus and dialogs
│   └── theme/                # Material 3 theming
├── viewmodels/               # ViewModels for screen state management
├── models/                   # Data models and DTOs
├── extensions/               # Kotlin extension functions
├── utils/                    # Utility classes
└── constants/                # App constants

app/schemas/                  # Room database schema exports for testing
```

### Database Architecture

The Room database (`song.db`) is the core of the app with **25 schema versions**. It uses:

- **Entities**: SongEntity, ArtistEntity, AlbumEntity, PlaylistEntity, LyricsEntity, FormatEntity, Event, PlayCountEntity, SearchHistory, ArtistWhitelistEntity
- **Mapping tables**: SongArtistMap, SongAlbumMap, AlbumArtistMap, PlaylistSongMap, RelatedSongMap
- **Views**: SortedSongArtistMap, SortedSongAlbumMap, PlaylistSongMapPreview
- **Features**: Artist whitelist filtering, format caching, play count tracking, lyrics storage

Schema exports are located in `app/schemas/` and configured via KSP:
```kotlin
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}
```

### Dependency Injection

Hilt is used throughout the app. Key modules in `app/src/main/kotlin/com/metrolist/music/di/`:
- Database module provides `MusicDatabase` singleton
- Network modules provide Ktor clients
- Player module provides Media3 ExoPlayer
- Repository modules provide data layer access

## Build Configuration

### Build Flavors

The app has 5 ABI-specific product flavors under the "abi" dimension:
- `universal` - All ABIs (armeabi-v7a, arm64-v8a, x86, x86_64)
- `arm64` - ARM 64-bit only
- `armeabi` - ARM 32-bit only
- `x86` - x86 32-bit only
- `x86_64` - x86 64-bit only

Each flavor gets a `BuildConfig.ARCHITECTURE` field with the ABI name.

### Release Builds

Release builds have:
- ProGuard minification enabled (`proguard-rules.pro`)
- Resource shrinking enabled
- PNG crunching disabled for faster builds
- Code signing via GitHub Secrets in CI/CD

### Environment Variables

The app uses environment variables for sensitive data (injected as `BuildConfig` fields):

```kotlin
buildConfigField("String", "LASTFM_API_KEY", "\"${System.getenv("LASTFM_API_KEY") ?: ""}\"")
buildConfigField("String", "LASTFM_SECRET", "\"${System.getenv("LASTFM_SECRET") ?: ""}\"")
```

For local development, Last.fm API keys are optional (will be empty strings). Get keys from: https://www.last.fm/api/account/create

For CI/CD, set GitHub Secrets:
- `LASTFM_API_KEY`
- `LASTFM_SECRET`
- `KEYSTORE` (base64-encoded)
- `KEY_ALIAS`
- `KEYSTORE_PASSWORD`
- `KEY_PASSWORD`
- `DEBUG_KEYSTORE` (base64-encoded, for persistent debug signing)

### Core Library Desugaring

The app uses Java 8+ APIs on older Android versions via desugaring:
```kotlin
compileOptions {
    isCoreLibraryDesugaringEnabled = true
}
```

## Development Workflow

### CI/CD

GitHub Actions workflows in `.github/workflows/`:

1. **build.yml** - Builds all ABI variants on every push
   - Runs lint checks
   - Signs APKs with GitHub Secrets
   - Uploads artifacts

2. **release.yml** - Automated releases on version bumps
   - Triggered when `versionCode` changes in `app/build.gradle.kts`
   - Creates GitHub releases with all ABI APKs

3. **build_pr.yml** - Pull request validation

### Versioning

Version is managed in `app/build.gradle.kts`:
```kotlin
versionCode = 128
versionName = "12.7.0"
```

Release workflow automatically detects version changes and creates releases.

### Translations

The app uses Weblate for translations (60+ languages):
- Translations: https://hosted.weblate.org/projects/Metrolist/
- Auto-generates locale config via `generateLocaleConfig = true`
- Translation status badge in README

### Signing

Debug builds use a persistent keystore (`app/persistent-debug.keystore`) for consistent signing across builds and devices. In CI, this is restored from the `DEBUG_KEYSTORE` secret.

## Important Development Notes

### Media3 Integration

The app uses Media3 (ExoPlayer) for playback with:
- MediaSession for media controls and Android Auto support
- OkHttp integration for network streaming
- Custom audio processing (normalization, tempo/pitch adjustment)
- Background playback with notification controls

### YouTube InnerTube API

The `innertube` module contains the YouTube Music API client. This is a private, undocumented API that may change without notice. The module is pure Kotlin/JVM with no Android dependencies.

### Compose Navigation

The app uses a single-activity architecture with Compose Navigation. All screens are Composables, and navigation is handled declaratively.

### DataStore Preferences

User preferences are stored using Jetpack DataStore (not SharedPreferences). See `app/src/main/kotlin/com/metrolist/music/utils/DataStoreManager.kt` or similar for preference keys.

### Room Migrations

The database has gone through 25 schema versions. When modifying entities:
1. Increment the version number in `@Database(version = X)`
2. Add an AutoMigration or manual Migration
3. Run the app to generate the new schema in `app/schemas/`
4. Commit the schema JSON file

### Timber Logging

The app uses Timber for logging. Use `Timber.d()`, `Timber.e()`, etc. instead of `Log`.

### Material 3 Dynamic Theming

The app supports Material 3 dynamic color (Material You) on Android 12+, plus custom light/dark/black themes.

### Android Auto & TV

The app has Android Auto support via MediaSession and Android TV support via leanback UI. MediaSession configuration is critical for these features.

### Region Restrictions

YouTube Music is not available in all regions. Users may need a VPN/proxy to use the app in unsupported regions.

## Common Development Patterns

### Adding a New Screen

1. Create a Composable in `app/src/main/kotlin/com/metrolist/music/ui/screens/`
2. Create a ViewModel in `viewmodels/` if needed
3. Add navigation route in the navigation graph
4. Use Hilt `@Inject` for dependencies

### Adding a Database Entity

1. Create entity in `db/entities/`
2. Add to `@Database(entities = [...])`
3. Add queries to `DatabaseDao`
4. Create a migration in `MusicDatabase.kt`
5. Increment database version
6. Build to generate schema

### Adding a Dependency

Dependencies are managed via version catalog in `gradle/libs.versions.toml`. Add entries there, then reference in `build.gradle.kts`:
```kotlin
implementation(libs.your.dependency)
```

### Working with InnerTube API

The `innertube` module handles YouTube Music API calls. Key classes:
- YouTube client initialization
- Search, browse, playback URL fetching
- Account authentication

When making changes, ensure the module remains Android-independent.
