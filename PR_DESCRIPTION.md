Closes #1115

Follows up on the original Cast PR that was merged - this adds build flavor support so Cast works on F-Droid too

### What this does
Separates Google Cast into a `gms` build flavor so the default build (`foss`) doesn't include any Google Play Services dependencies. This addresses the F-Droid compatibility concern raised by @FineFindus.

### How it works
- `foss` flavor (default): No Cast functionality, no GMS libs - this is what F-Droid will build
- `gms` flavor: Full Cast support like before

The `foss` flavor has stub implementations so the code compiles without needing the Cast libs. The Cast toggle in Settings only shows up in `gms` builds.

### Changes
Build config:
- Added `variant` flavor dimension alongside existing `abi` dimension
- Cast dependencies now use `gmsImplementation` instead of `implementation`
- `foss` is set as default so existing CI commands work unchanged

New source sets:
- `app/src/foss/` - stub files (empty composables, no-op handlers)
- `app/src/gms/` - moved the real Cast implementation here

Modified:
- PlayerSettings checks `BuildConfig.CAST_AVAILABLE` before showing Cast toggle
- MusicService works with both stubs and real implementation

### Testing
Both `assembleUniversalFossDebug` and `assembleUniversalGmsDebug` build and run correctly. FOSS build has no Cast UI, GMS build works like before.
