# RideWave 🏍️🎵

RideWave is an Android application designed to provide simple and accessible media controls for motorcycle riders.

The goal of RideWave is to minimize the interaction required to control music while riding. The current version establishes the foundation of the application by integrating Spotify authentication and playback controls.

## Current Status

🚧 **RideWave is currently under development.**

The current implementation supports:

- Spotify account authorization
- Spotify App Remote connection
- Play/resume playback
- Pause playback
- Skip to the next track
- Skip to the previous track
- Spotify installation detection
- Connection and authorization error handling
- Jetpack Compose-based UI

Gesture-based controls, playlist browsing, track information, and the full rider interface will be added in future iterations.

---

## Tech Stack

- **Kotlin**
- **Jetpack Compose**
- **Material 3**
- **Spotify Android Authentication SDK**
- **Spotify App Remote SDK 0.8.0**
- **Gradle Kotlin DSL**
- **Version Catalogs**

### Android Configuration

- Minimum SDK: **24**
- Target SDK: **37**
- Application ID: `com.example.ridewave`

---

## Spotify Integration

RideWave currently uses two components of Spotify's Android integration.

### Spotify Authentication SDK

The Authentication SDK is used to explicitly request permission from the user to allow RideWave to control Spotify.

The application currently requests:

```text
app-remote-control
```

### Spotify App Remote SDK

After authorization succeeds, RideWave establishes an App Remote connection with the Spotify Android application.

App Remote is then used for playback operations such as:

```text
Play / Resume
Pause
Previous Track
Next Track
```

The Spotify application must be installed on the Android device.

---

## Authentication Flow

RideWave currently uses an explicit authorization flow before establishing the Spotify App Remote connection.

```text
RideWave
   │
   │ User selects "Connect Spotify"
   ▼
Spotify Authentication SDK
   │
   │ Request: app-remote-control
   ▼
Spotify Authorization
   │
   │ User approves RideWave
   ▼
RideWave
   │
   │ Authorization successful
   ▼
Spotify App Remote
   │
   │ Establish connection
   ▼
Spotify Connected
   │
   ├── Play
   ├── Pause
   ├── Previous
   └── Next
```

Explicit authorization is intentionally performed before connecting App Remote.

---

## Project Structure

```text
RideWave/
│
├── app/
│   ├── libs/
│   │   └── spotify-app-remote-release-0.8.0.aar
│   │
│   ├── src/main/
│   │   ├── java/com/example/ridewave/
│   │   │   ├── MainActivity.kt
│   │   │   │
│   │   │   ├── spotify/
│   │   │   │   ├── SpotifyConfig.kt
│   │   │   │   └── SpotifyManager.kt
│   │   │   │
│   │   │   └── ui/
│   │   │       └── theme/
│   │   │
│   │   └── AndroidManifest.xml
│   │
│   └── build.gradle.kts
│
├── gradle/
│   └── libs.versions.toml
│
├── settings.gradle.kts
└── README.md
```

---

## SpotifyManager

`SpotifyManager` acts as the central integration layer between RideWave and Spotify.

It is currently responsible for:

```text
Spotify installation detection
        │
        ▼
Spotify authorization request
        │
        ▼
Spotify App Remote connection
        │
        ▼
Playback commands
```

Supported operations include:

```kotlin
spotifyManager.play()

spotifyManager.pause()

spotifyManager.next()

spotifyManager.previous()

spotifyManager.disconnect()
```

Keeping Spotify-specific functionality inside `SpotifyManager` prevents the UI layer from depending directly on Spotify implementation details.

---

## Spotify Developer Setup

To run RideWave locally, create an application in the Spotify Developer Dashboard.

Configure the Android application using your own Spotify developer credentials.

### Redirect URI

RideWave currently uses:

```text
ridewave://callback
```

The same redirect URI must be configured in the Spotify Developer Dashboard.

### Android Package

```text
com.example.ridewave
```

### SHA-1 Fingerprint

Retrieve the signing fingerprint for your local build using:

```bash
./gradlew signingReport
```

Find the `debug` variant:

```text
Variant: debug
SHA1: XX:XX:XX:XX:...
```

Register the corresponding Android package and SHA-1 fingerprint in the Spotify Developer Dashboard.

---

## Development Mode

If the Spotify application is running in **Development Mode**, the Spotify account used for testing must be added under:

```text
Spotify Developer Dashboard
        ↓
RideWave
        ↓
User Management
```

Otherwise, the account may not be authorized to use the developer application.

---

## Client ID Configuration

Add your Spotify Client ID to:

```text
SpotifyConfig.kt
```

Example:

```kotlin
object SpotifyConfig {

    const val CLIENT_ID =
        "YOUR_SPOTIFY_CLIENT_ID"

    const val REDIRECT_URI =
        "ridewave://callback"

    val SCOPES = arrayOf(
        "app-remote-control"
    )
}
```

Do **not** commit sensitive credentials such as a Spotify Client Secret to the repository.

---

## Running RideWave

### 1. Clone the repository

```bash
git clone <repository-url>

cd RideWave
```

### 2. Verify Spotify configuration

Make sure your Spotify Developer application contains:

```text
Redirect URI:
ridewave://callback

Android Package:
com.example.ridewave

SHA-1:
<your debug signing fingerprint>
```

### 3. Add your Client ID

Configure `SpotifyConfig.kt` with your Spotify Client ID.

### 4. Install Spotify

Install the official Spotify Android application on the test device and sign in.

### 5. Build RideWave

```bash
./gradlew assembleDebug
```

Or run the application directly from Android Studio.

### 6. Connect Spotify

Launch RideWave and select:

```text
Connect Spotify
```

Complete Spotify authorization when prompted.

After authorization succeeds, RideWave will establish an App Remote connection.

---

## Current UI

The current interface is intentionally minimal and primarily exists to validate Spotify integration.

After connecting Spotify, the application exposes:

```text
RideWave

Spotify Connected!

[ Play ]

[ Pause ]

[ Previous ]

[ Next ]
```

The production RideWave interface will eventually replace these controls with a rider-focused interaction model.

---

## Planned Features

RideWave is intended to evolve into a gesture-driven media controller.

Planned capabilities include:

- Gesture-based playback controls
- Swipe right → Next track
- Swipe left → Previous track
- Swipe up/down → Volume control
- Single tap → Play/Pause
- Currently playing track information
- Artist information
- Album artwork
- Spotify playlist browsing
- Spotify playlist selection
- Landscape/fullscreen rider interface
- Notification display
- Digital clock
- Rider and pillion-friendly controls

---

## Intended Gesture Model

```text
                 Swipe Up
                Volume Up
                    ↑
                    │
                    │

Swipe Left  ←   RideWave   →  Swipe Right
Previous                         Next

                    │
                    │
                    ↓
                Swipe Down
               Volume Down


                  Tap
              Play / Pause
```

Gesture controls are part of the planned RideWave experience and are not yet fully implemented in the current Spotify integration milestone.

---

## Current Development Milestone

### Spotify Connectivity ✅

The current milestone establishes the core Spotify communication layer:

```text
Spotify installed
       ↓
Explicit authorization
       ↓
app-remote-control permission
       ↓
Spotify App Remote connection
       ↓
Playback commands
```

With this foundation working, development can now move toward the actual RideWave rider experience.

---

## Disclaimer

RideWave is an independent project and is not affiliated with, endorsed by, or sponsored by Spotify.

Spotify and the Spotify logo are trademarks of Spotify AB.