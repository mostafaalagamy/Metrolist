# Implementation Plan: Local Sync

## 1. Architectural Analysis and Key Integration Points

### 1.1. Playback Management
- **`playback/MusicService.kt`**: This is the core of audio playback. It manages the `ExoPlayer` instance, handles audio focus, and updates notifications. Crucially, it implements `Player.Listener`, giving it direct access to all playback events like `onMediaItemTransition`, `onPlaybackStateChanged`, and `onPlayWhenReadyChanged`.
- **Integration Strategy**: `MusicService` is the ideal location to host the `PlaybackServer` (the WebSocket server). Its lifecycle as a foreground service ensures the server remains active while music is playing, even if the app is in the background. The existing player listeners will be used to broadcast state changes to connected clients.

### 1.2. UI and State Layer
- **`playback/PlayerConnection.kt`**: This class acts as a bridge between `MusicService` and the UI. It wraps the `Player` instance and exposes its state as Kotlin `Flow`s (e.g., `isPlaying`, `mediaMetadata`, `queueWindows`).
- **`MainActivity.kt`**: The main activity is responsible for binding to the `MusicService` and creating the `PlayerConnection`. The Jetpack Compose UI, particularly the `BottomSheetPlayer`, directly collects the `Flow`s from `PlayerConnection` to update its state. There is no central `PlayerViewModel`.
- **Integration Strategy**: For a "controller" device, the UI will need a new component (e.g., a `BottomSheet` or a dedicated screen) to display available devices. The `PlayerConnection` will be extended or a new "controller" class will be created to manage the WebSocket client connection and receive state updates, which will then be fed into the existing UI components.

### 1.3. Dependency Injection (Hilt)
- **`di/AppModule.kt` & `di/NetworkModule.kt`**: These Hilt modules provide application-scoped dependencies. `AppModule` handles databases and caches, while `NetworkModule` is currently minimal but is the logical place for network-related components.
- **Integration Strategy**: A new `SyncModule.kt` should be created within the `di` package. This module will provide singleton instances of `LocalSyncManager` (for NSD), `PlaybackServer`, and `PlaybackController`. This keeps the new dependencies organized and separate from the existing modules.

### 1.4. Lifecycle Management and Coroutines
- The app uses `lifecycleScope` in `MainActivity` and a custom `CoroutineScope` in `MusicService`. The `MusicService` runs as a foreground service, which is suitable for managing long-lived network connections.
- **Integration Strategy**: The `LocalSyncManager` and `PlaybackServer` should be managed within the `MusicService`'s `CoroutineScope`. Their lifecycle will be tied directly to the service's lifecycle: started in `onCreate` and stopped in `onDestroy`. This ensures that the discovery and server components are only running when the player is active.

### 1.5. Module Structure
- The project is structured with several modules (`app`, `innertube`, `kizzy`). However, all the core application logic, including UI, playback, and database, resides within the `app` module.
- **Recommendation**: The "Local Sync" functionality should be implemented entirely within the `app` module. Creating a new, separate module for this feature would introduce unnecessary complexity for a feature that is tightly coupled with the `MusicService` and the main UI. The logic can be neatly organized into a new `sync` package (e.g., `com.metrolist.music.sync`).

## 2. Detailed Map of the New Architecture

### 2.1. Discovery Flow
- **Player (Host)**:
  - **Class**: `sync/LocalSyncManager.kt` will encapsulate all `NsdManager` logic (registration, unregistration).
  - **Initiation**: An instance of `LocalSyncManager` will be injected into `MusicService`.
  - **Lifecycle**: `MusicService.onCreate()` will call `localSyncManager.start()` to register the service. `MusicService.onDestroy()` will call `localSyncManager.stop()` to unregister it. The service will be advertised with a name derived from the user's device.
- **Controller (Client)**:
  - **UI Component**: A new `BottomSheet` composable, launched from an icon in the player UI, will be created to display a list of available devices.
  - **Initiation**: The ViewModel associated with the player screen (or a new `SyncViewModel`) will inject `LocalSyncManager` and call a method like `localSyncManager.discoverDevices()`. The manager will expose a `Flow<List<DiscoveredDevice>>` that the UI will collect to display the list.

### 2.2. Communication Flow
- **WebSocket Server (Host)**:
  - **Class**: `sync/PlaybackServer.kt`. It will be injected as a singleton into `MusicService`.
  - **Events**: Inside `MusicService`, the existing `Player.Listener` methods (`onMediaItemTransition`, `onPlaybackStateChanged`, `onIsPlayingChanged`, etc.) will be modified. They will call corresponding methods on the `PlaybackServer` instance (e.g., `playbackServer.broadcastState(newState)`), which will serialize the current player state into the `PlaybackState` data class and send it to all connected clients.
- **WebSocket Client (Client)**:
  - **Class**: `sync/PlaybackController.kt`. When a user selects a host device from the discovery UI, an instance of this class will be created.
  - **State Updates**: `PlaybackController` will connect to the selected server's WebSocket. It will deserialize incoming JSON messages into `PlaybackState` objects. It will expose a `Flow<PlaybackState>` that the `PlayerConnection` (or a similar controller-specific class) will consume to update the UI, making the controller device mirror the host's player state.
- **Command Flow (Controller -> Player)**:
  - A user action (e.g., clicking "Play") on the controller's UI will call a method on the `PlaybackController` (e.g., `playbackController.sendCommand(PlayerCommand.Play)`).
  - `PlaybackController` will serialize the `PlayerCommand` into JSON and send it over the WebSocket to the `PlaybackServer`.
  - `PlaybackServer` will receive the command, deserialize it, and call the appropriate method on the `ExoPlayer` instance within `MusicService` (e.g., `player.play()`). This completes the loop, and the resulting state change will be broadcast back to all clients.

## 3. Communication Contract (API Definition)

### 3.1. State Model (`PlaybackState.kt`)
```kotlin
package com.metrolist.music.sync.models

import kotlinx.serialization.Serializable

@Serializable
data class PlaybackState(
    val isPlaying: Boolean,
    val mediaMetadata: MediaMetadata?, // A serializable version of the existing MediaMetadata
    val position: Long,
    val duration: Long,
    val repeatMode: Int,
    val shuffleModeEnabled: Boolean,
    val queue: List<MediaMetadata>
)
```

### 3.2. Command Model (`PlayerCommand.kt`)
```kotlin
package com.metrolist.music.sync.models

import kotlinx.serialization.Serializable

@Serializable
sealed class PlayerCommand {
    @Serializable
    object Play : PlayerCommand()

    @Serializable
    object Pause : PlayerCommand()

    @Serializable
    object SkipNext : PlayerCommand()

    @Serializable
    object SkipPrevious : PlayerCommand()

    @Serializable
    data class SeekTo(val position: Long) : PlayerCommand()
}
```

## 4. Implementation Guide (Step-by-Step)

**Step 1: Create Core Sync Components and DI**
1.  **Create `sync` package**: `app/src/main/kotlin/com/metrolist/music/sync/`
2.  **Define Data Models**: Create `sync/models/PlaybackState.kt` and `sync/models/PlayerCommand.kt` as defined above. Add `kotlinx-serialization` annotations.
3.  **Create `sync/LocalSyncManager.kt`**: Implement `NsdManager` logic for service registration and discovery. Use a `Flow` to emit discovered services.
4.  **Create `sync/PlaybackServer.kt`**: Implement a Ktor WebSocket server. Add methods to broadcast `PlaybackState` and to handle incoming `PlayerCommand`s.
5.  **Create `sync/PlaybackController.kt`**: Implement a Ktor WebSocket client. Add methods to send `PlayerCommand`s and a `Flow` to emit received `PlaybackState`.
6.  **Create `di/SyncModule.kt`**: Create a new Hilt module to provide `@Singleton` instances of the three classes above.

**Step 2: Integrate with `MusicService` (Host)**
1.  **Modify `playback/MusicService.kt`**:
    -   `@Inject` `LocalSyncManager` and `PlaybackServer`.
    -   In `onCreate`, call `localSyncManager.start()` and `playbackServer.start()`.
    -   In `onDestroy`, call `localSyncManager.stop()` and `playbackServer.stop()`.
    -   In the `Player.Listener` methods, call the appropriate `playbackServer` methods to broadcast state changes.
    -   Implement a handler in `PlaybackServer` to listen for commands and execute them on the `player` instance.

**Step 3: Modify the UI (Controller)**
1.  **Create Device Discovery UI**: Create a new Composable `BottomSheet` (e.g., `DeviceSyncSheet.kt`) that gets a `SyncViewModel`.
2.  **Create `SyncViewModel.kt`**:
    -   `@Inject` `LocalSyncManager` and `PlaybackController`.
    -   Expose a `Flow` of discovered devices from `LocalSyncManager`.
    -   When a device is selected, connect the `PlaybackController` to it.
3.  **Integrate with Player UI**:
    -   Add a "Device Sync" icon to the main player UI. Clicking it will show the `DeviceSyncSheet`.
    -   Modify the `PlayerConnection` or create a wrapper around it that can switch between listening to the local player and the remote `PlaybackController`'s state flow. When in "controller" mode, UI actions (play, pause, seek) should be routed to the `PlaybackController` instead of the local player.

## 5. Challenges and Points of Attention
- **Error Handling**: The connection can be lost at any time. The UI must react gracefully, showing a "disconnected" state and providing an option to reconnect. Both client and server should implement robust error handling and retry logic.
- **Network Permissions**: The implementation will require `android.permission.ACCESS_WIFI_STATE` and `android.permission.CHANGE_WIFI_STATE`. For Android 13 (API 33) and above, `android.permission.NEARBY_WIFI_DEVICES` will also be necessary and must be requested at runtime.
- **Battery Consumption**: `NsdManager` can be battery-intensive. Discovery should only be active when the user is actively looking for devices. The WebSocket connection should be managed efficiently, ensuring it is closed when the app is no longer in controller mode.
- **Security**: Since this is a local network feature, the security risks are lower, but not zero. The communication contract should not exchange any sensitive user data. A simple handshake or PIN mechanism could be considered for a future iteration to prevent unauthorized connections.
- **Multiple Controllers**: The server architecture should be designed to handle multiple client connections simultaneously. The `PlaybackServer` should maintain a list of connected clients and broadcast state updates to all of them.
