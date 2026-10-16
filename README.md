# RideWave

RideWave is an Android app for simple Spotify playback control. All pages use a pure black background. The introduction explains gestures once, the connection page handles initial Spotify setup, and returning authorized users open ride mode directly while Spotify reconnects in the background. Ride mode has a bold clock in the header, a top-left close icon, and gesture feedback that clears after two seconds. The entire ride screen accepts gestures except the close control.

The project is actively evolving. The working feature set is intentionally small: connect to Spotify, then use a tap or horizontal swipe to control the current playback session.

## Features

- Spotify authorization using the Android Authentication SDK.
- Spotify App Remote connection and automatic reconnection after a previous authorization.
- Tap to toggle play/pause.
- Swipe left to skip to the next track.
- Swipe right to skip to the previous track.
- Portrait and landscape support without recreating the activity or reconnecting Spotify on rotation.
- Screen kept awake only during ride mode; the clock updates once per minute.
- Playback state synchronized from Spotify for the tap-to-toggle gesture.
- Connection, authorization, timeout, and missing-Spotify-app feedback.

Ride mode displays the live song title, artist, and playback status at the bottom center. A large, full-opacity white clock sits in the header; centered playback icons provide two-second gesture feedback.

The project does not yet provide playlist browsing, configurable gestures, background playback, or a dedicated settings screen.

## Technology

- Kotlin 2.2.10
- Android Gradle Plugin 9.4.1
- Jetpack Compose and Material 3
- Gradle Kotlin DSL with a version catalog
- Spotify Android Authentication SDK 2.1.0
- Spotify App Remote SDK 0.8.0 (local AAR)
- Java 11 source and target compatibility

Android configuration:

- Application ID: `com.example.ridewave`
- Minimum SDK: 24
- Compile/target SDK: 37
- Module: `app`

## Repository layout

```text
.
├── app/
│   ├── libs/                         # Local Spotify App Remote AAR
│   ├── src/main/
│   │   ├── java/com/example/ridewave/
│   │   │   ├── MainActivity.kt       # UI, lifecycle, auth flow
│   │   │   ├── gestures/              # Gesture detection and action mapping
│   │   │   ├── spotify/               # Spotify configuration and integration
│   │   │   └── ui/theme/              # Compose theme definitions
│   │   └── AndroidManifest.xml        # Permissions, launcher, callback URI
│   ├── src/test/                      # Local JVM tests
│   └── src/androidTest/               # Instrumented Android tests
├── gradle/libs.versions.toml         # Dependency and plugin versions
├── build.gradle.kts                   # Root Gradle configuration
├── settings.gradle.kts                # Module and repository configuration
├── AGENTS.md                          # Repository contribution guidance
└── README.md
```

## Requirements

- Android Studio with a compatible Android SDK. The Gradle daemon toolchain is configured for JDK 25; Java source/target compatibility is 11.
- An Android device or emulator running API 24 or later.
- The Spotify Android app installed on the test device.
- A Spotify Developer application configured for this package and signing key.

The Spotify App Remote integration is device-oriented. Testing on a physical device with Spotify installed is recommended.

## Spotify setup

1. Create an application in the [Spotify Developer Dashboard](https://developer.spotify.com/dashboard).
2. Add the Android package `com.example.ridewave`.
3. Register the debug signing certificate SHA-1. Generate it with:

   ```bash
   ./gradlew signingReport
   ```

4. Configure the redirect URI `ridewave://callback`.
5. Set the client ID in `app/src/main/java/com/example/ridewave/spotify/SpotifyConfig.kt` for local development.
6. If the Spotify application is in Development Mode, add the Spotify account used for testing to the application's allowed users.

The redirect URI must match in all three places: Spotify Developer Dashboard, `SpotifyConfig.kt`, and the manifest intent filter. The package name and SHA-1 must match the installed build's signing identity.

### Credential safety

Do not commit a real client ID or other credentials to a public repository. Review `SpotifyConfig.kt` before publishing or sharing the repository, and prefer a local, ignored configuration mechanism for future work. A Spotify client ID is not a secret by itself, but committing application-specific credentials still makes environment management and rotation harder.

## Build and test

From the repository root:

```bash
# Build the debug APK
./gradlew assembleDebug

# Run JVM unit tests
./gradlew test

# Run lint checks, when configured for the local Android SDK
./gradlew lint

# Install the debug APK on a connected device
./gradlew installDebug
```

The generated debug APK is placed under `app/build/outputs/apk/debug/`. Instrumented tests require a connected emulator or device:

```bash
./gradlew connectedAndroidTest
```

Before opening a pull request, run the relevant build and tests and explain any checks that could not run locally.

## How the app works

`MainActivity` owns the Compose screen and Android lifecycle. `SpotifyManager` isolates Spotify installation checks, authorization intent creation, App Remote connection management, playback commands, and disconnect handling. `GestureDetector` translates pointer input into `GestureType` values, while `GestureHandler` maps those values to Spotify actions.

The connection flow is:

```text
Connect Spotify
      ↓
Spotify Authentication SDK
      ↓
Authorization callback
      ↓
Spotify App Remote connection
      ↓
Tap / swipe playback controls
```

Onboarding completion and authorization state are remembered in app preferences. After setup, subsequent launches open ride mode and reconnect in the background. Connection failures open the connection page with a retry action. Successful connection opens ride mode automatically. The close icon exits the app; Android Back returns to the preceding page.

Orientation and screen-size changes are handled by the activity so rotation preserves the Spotify connection and current page. Backgrounding the app disconnects App Remote; returning to ride mode reconnects in the background.

## Known limitations

- Playback controls depend on an active Spotify App Remote connection.
- The local Spotify App Remote AAR is required to build the app.
- The current UI hides system bars and adapts its layout to the available window size.
- There is only one application module and no production release/signing configuration.
- The starter unit and instrumented tests are still minimal; new behavior should be covered as it stabilizes.

## Contribution notes

Keep Spotify-specific code in the `spotify` package, gesture interpretation in `gestures`, and UI concerns in Compose/activity code. Prefer small, focused changes, verify both orientations, and update this README when setup or user-visible behavior changes. See [AGENTS.md](AGENTS.md) for repository working agreements.

## License

No license has been declared yet. Until one is added, assume the code is not licensed for redistribution.
