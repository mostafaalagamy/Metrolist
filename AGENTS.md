# Metrolist Agent Guidelines

This document provides context, commands, and standards for AI agents operating within the Metrolist repository.

## 1. Project Overview
Metrolist is an open-source YouTube Music client for Android.
- **Platform**: Android (Min SDK 26, Target SDK 36).
- **Language**: Kotlin.
- **Java Version**: Java 21 (Strictly enforced). Do not change toolchain versions.
- **Architecture**: MVVM with Clean Architecture principles.
- **Multi-module**: Split into `:app` (UI/Logic) and API clients (`:innertube`, `:kugou`, etc.).

## 2. Build & Test Commands

Use the Gradle Wrapper (`./gradlew`) for all tasks.

### Build
- **Build Debug APK**:
  ```bash
  ./gradlew assembleDebug
  ```
- **Build Release APK**:
  ```bash
  ./gradlew assembleRelease
  ```
- **Clean Build Directory**:
  ```bash
  ./gradlew clean
  ```

### Linting & Code Analysis
- **Run Android Lint**:
  ```bash
  ./gradlew lint
  ```
  - Configuration is in `lint.xml` and `app/build.gradle.kts`.
  - *Note*: `abortOnError` is set to `false`, but critical issues should be fixed.

### Testing
*Note: Test coverage is currently minimal. New logic (especially in API modules) should include unit tests.*

- **Run All Unit Tests**:
  ```bash
  ./gradlew test
  ```
- **Run Specific Test Class**:
  ```bash
  ./gradlew test --tests "com.metrolist.music.MyTestClass"
  ```
- **Run Specific Test Method**:
  ```bash
  ./gradlew test --tests "com.metrolist.music.MyTestClass.myTestMethod"
  ```

## 3. Tech Stack

- **UI**: Jetpack Compose (Material3).
- **DI**: Hilt (Dagger).
- **Concurrency**: Kotlin Coroutines & Flow.
- **Networking**:
  - **Ktor Client**: Used in `:innertube`, `:kugou`, etc.
  - **OkHttp/Retrofit**: Used internally by libraries like Coil or Media3.
- **Database**: Room.
- **Image Loading**: Coil.
- **Media Playback**: AndroidX Media3 (ExoPlayer).

## 4. Code Style & Conventions

### General Kotlin
- **Formatting**: Follow standard [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html).
  - Indentation: 4 spaces.
  - No wildcard imports (`import foo.*`).
- **Naming**:
  - Classes/Interfaces: `PascalCase`.
  - Functions/Properties: `camelCase`.
  - Constants/Const Vals: `SCREAMING_SNAKE_CASE`.
  - Backing Properties: `_propertyName` (private mutable) + `propertyName` (public immutable).

### Jetpack Compose
- **Naming**: Composable functions that return `Unit` should use `PascalCase` (noun-like, e.g., `SearchBar`).
- **Parameters**: `modifier: Modifier = Modifier` should be the first optional parameter.
- **State**: Use `remember`, `derivedStateOf`, and `LaunchedEffect` appropriately.
- **Theming**: Use `MaterialTheme.colorScheme` and `MaterialTheme.typography`.

### Error Handling
- **Result Pattern**: The API modules (specifically `:innertube`) rely heavily on `kotlin.Result`.
  ```kotlin
  // Preferred pattern in API modules
  suspend fun fetchData(): Result<Data> = runCatching {
      // make network request
  }
  ```
- **UI Layer**: Observe `Result` outcomes and map to UI states (Loading, Content, Error).

### Module-Specific Guidelines

#### `:app` Module
- Contains UI code, ViewModels, and integration logic.
- **Package Structure**:
  - `ui/component`: Reusable Compose components.
  - `ui/screens`: Full screen Composables.
  - `viewmodel`: Hilt-injected ViewModels.
  - `constants`: App-wide constants and `DataStore` keys.

#### `:innertube` Module
- Handles communication with YouTube Music's internal API.
- **Core Object**: `YouTube.kt` is the main entry point for high-level operations.
- **Data Models**: Classes in `models/` often mirror YouTube's JSON response structure.
- **Network**: Uses Ktor for requests.

## 5. Development Workflow for Agents

1. **Discovery**:
   - Use `ls` and `glob` to locate relevant files.
   - Read `build.gradle.kts` in the target module to understand available dependencies.

2. **Implementation**:
   - Follow existing patterns. If you see `runCatching`, use it.
   - When creating new UI components, place them in `ui/component` if reusable.
   - **Strings**: Use `res/values/metrolist_strings.xml` for all string resources. Do *not* use or create `strings.xml`.
   - Ensure new resources (strings, colors) are added to `res/values` if not using Material3 defaults.

3. **Verification**:
   - Always run `./gradlew assembleDebug` after significant changes to verify compilation.
   - If tests were added, run them to confirm behavior.

4. **Safety**:
   - Do not commit secrets or API keys.
   - Note that `local.properties` is used for secrets (e.g., LastFM keys).

## 6. Common Patterns

**Coroutines in ViewModel:**
```kotlin
@HiltViewModel
class MyViewModel @Inject constructor(
    private val repository: MyRepository
) : ViewModel() {
    fun loadData() {
        viewModelScope.launch {
            repository.getData()
                .onSuccess { /* handle success */ }
                .onFailure { /* handle error */ }
        }
    }
}
```

**Ktor Request (Innertube):**
```kotlin
client.post(url) {
    setBody(MyBody())
}.body<MyResponse>()
```
