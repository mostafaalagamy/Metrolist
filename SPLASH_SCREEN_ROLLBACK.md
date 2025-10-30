# Splash Screen Implementation - Rollback Guide

## Changes Made

This document describes all changes made to implement the splash screen loading indicator and provides instructions to roll back if needed.

### Files Created

1. **`app/src/main/kotlin/com/metrolist/music/ui/screens/SplashScreen.kt`**
   - New file created
   - Contains the SplashScreen composable
   - Displays "Zemer" app name, loading message, progress bar, and sync status
   - **To rollback**: Simply delete this file

### Files Modified

#### 1. `app/src/main/kotlin/com/metrolist/music/MainActivity.kt`

**Changes:**

**Line 185** - Added import:
```kotlin
import com.metrolist.music.ui.screens.SplashScreen
```

**Lines 439-446** - Added splash screen conditional logic:
```kotlin
// Check whitelist sync status
val syncProgress by syncUtils.whitelistSyncProgress.collectAsState()

// Show splash screen while syncing
if (!syncProgress.isComplete) {
    SplashScreen(syncProgress = syncProgress)
    return@BoxWithConstraints
}
```

**To rollback MainActivity.kt:**

1. Remove the import at line 185:
   ```kotlin
   import com.metrolist.music.ui.screens.SplashScreen
   ```

2. Remove lines 439-446 (the splash screen check block)

The code should return to this state at line 439:
```kotlin
val bottomInsetDp = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()

val navController = rememberNavController()
```

## Behavior Changes

### Before
- App shows main UI immediately on launch
- Whitelist syncs in background (non-blocking)
- User sees empty content until sync completes
- First-time users: ~20-40 seconds of empty UI

### After
- App shows "Zemer" splash screen with progress bar
- Displays sync progress (current/total artists, artist name)
- Main UI only appears when sync is complete
- Smooth transition from loading to main UI

## Testing Notes

To test the splash screen:
1. Clear app data: `adb shell pm clear com.metrolist.music.debug`
2. Launch app - splash screen should appear during first-time sync
3. Close and reopen app - if synced < 24hrs ago, goes straight to main UI
4. If synced > 24hrs ago, splash screen appears again

## Quick Rollback Command

```bash
# Delete splash screen file
rm app/src/main/kotlin/com/metrolist/music/ui/screens/SplashScreen.kt

# Restore MainActivity.kt from git
git checkout HEAD -- app/src/main/kotlin/com/metrolist/music/MainActivity.kt

# Rebuild
./gradlew assembleArm64Debug
adb install -r app/build/outputs/apk/arm64/debug/app-arm64-debug.apk
```

## Git Commit Info

All splash screen changes are in a single commit. To revert:
```bash
git revert HEAD
```

Or to manually undo:
```bash
git checkout HEAD~1 -- app/src/main/kotlin/com/metrolist/music/MainActivity.kt
rm app/src/main/kotlin/com/metrolist/music/ui/screens/SplashScreen.kt
```
