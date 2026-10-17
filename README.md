# RideWave

RideWave is an Android app for simple Spotify playback control. All pages use a pure black background. The introduction explains gestures once, the connection page handles initial Spotify setup, and returning authorized users open ride mode directly while Spotify reconnects in the background. Ride mode has a bold clock in the header, a top-left close icon, and gesture feedback that clears after two seconds. The entire ride screen accepts gestures except the close, notification, and contacts controls.

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
- Top-right notification button with a scrollable popup of current device notifications.
- Fullscreen ride contacts with up to four large animated bubbles, direct calling, and a 10-second inactivity timeout.

Ride mode displays the live song title, artist, and playback status at the bottom center. A large, full-opacity white clock sits in the header; centered playback icons provide two-second gesture feedback.

The project does not yet provide configurable gestures, background playback, or a dedicated settings screen.

## Ride playlists

Tap the bottom-left Playlists button to choose up to four playlist bubbles. The picker loads all pages of your playlist library. Selected rows have a highlighted background and border, with spacing between items. Press **Done** with zero to four selections. Tap a bubble to start playback through Spotify App Remote, then enable shuffle in the new playback context. If Spotify rejects shuffle, playback continues with a visible warning. **Edit** reopens the full list with your selections highlighted; deselect and replace playlists, then press **Done**. Close or Back discards unfinished edits. The overlay closes after five seconds without interaction; touching, scrolling, or activating a control restarts the timer, while loading and backgrounding pause it. Saved choices and setup completion persist between launches.

Playlist bubbles load cover art from the image URLs returned by Spotify, with a name fallback when artwork is missing or unavailable. Names remain below the bubbles. Artwork is loaded off the main thread with bounded downloads and timeouts. Opening the overlay refreshes playlist metadata, including temporary image URLs. Portrait uses a scrollable two-column layout; landscape places the bubbles in a row.

Web API authorization uses PKCE and the playlist scopes in the configuration template; playback uses App Remote. No redirect URI, manifest, or dashboard changes are required for this flow. Spotify must be installed, authorized, and connected for playback. Loading and playback failures appear in the playlist overlay.

Manual verification: authorize a new account, browse a library larger than 50 playlists, save zero, two, and four selections, and restart after each. Confirm playback followed by shuffle for each playlist bubble. Edit and replace a selection; close an unfinished edit and confirm saved choices remain. Check four bubbles in portrait/landscape, artwork and missing-image fallback, a disconnected device, and an account with fewer than four playlists.

## Device notifications

Tap the bell in the top-right corner of any screen. On first use, choose **Enable notification access**, then allow RideWave in Android Settings and return to the app. This uses Android's notification listener service; Spotify setup and callback configuration are unchanged.

In portrait, the popup fills half the screen height and uses the available width. In landscape, it becomes a full-height panel on the right half of the screen. It shows active notifications with app icons, newest first, and updates as they arrive or are removed. Each preview is limited to four lines (one title line and up to three body lines), with an ellipsis for longer content. App names remain available to screen readers through the icons. It closes after five seconds without interaction; touching or scrolling restarts the timer, and Android accessibility timeout preferences are respected. Close, Back, and tapping outside also dismiss it. Popup interactions do not trigger playback gestures.

Notifications are held only in memory, with no history, storage, or upload. This is a read-only preview: opening or dismissing a notification in its source app is not supported. Android may redact sensitive content or restrict work-profile/device notifications. Access can be revoked in Android Settings. If the listener is disconnected, the popup offers settings to restore access.

Manual verification: grant and revoke access; post, update, and remove notifications from another app; check empty and disconnected states; scroll a long list in both orientations; verify inactivity dismissal and return from Settings. Confirm Spotify reconnects after leaving Settings and that popup interactions do not change playback.

## Ride contacts

Tap the contacts button at the bottom-right of ride mode to open a fullscreen overlay with a subtle heading. Large photo/name bubbles use a two-column layout in portrait and a single row in landscape. A single **+** bubble starts in the center; the add bubble remains until four contacts have been saved. The overlay uses true black surfaces, restrained white and gray styling, and brief fades to match ride mode and minimize OLED power use. Tap **Edit** to remove a saved contact. Back, Close, or the surrounding background dismisses the overlay without triggering playback.

The overlay closes after **five seconds of inactivity**. Touch and accessibility button actions restart the timer. Holding a touch, loading/saving, the contact picker, permission prompts, app settings, and backgrounding pause it; returning starts a fresh interval. Android accessibility timeout preferences can extend the interval. Animation speed settings do not shorten the actual timeout.

Tapping a contact **places a call directly** using the selected number. Android asks for **Phone (`CALL_PHONE`) permission** the first time; granting it continues that selected call. If permission is denied, RideWave offers another request or app settings. Returning from settings requires a fresh contact tap. The phone app may still require a SIM choice or device-specific confirmation. Missing calling apps and denied/revoked permissions show feedback.

Choose numbers with Android's contact picker. RideWave requests read-only Contacts permission because some Android contact providers do not grant photo access with the selected phone-number row. Only the four contacts you explicitly select—their names, phone numbers, and resized photos—are copied to private app storage excluded from Android backup. RideWave does not import or retain the rest of the address book. Details persist between launches and are snapshots: remove and re-add a contact to refresh a changed number or photo. Duplicate numbers are rejected. Picker and storage failures show feedback.

This uses Android's [phone-number picker and direct-call intents](https://developer.android.com/guide/components/intents-common). The manifest declares `CALL_PHONE` and keeps telephony optional so devices without calling hardware can still install RideWave. Spotify setup and callback configuration are unchanged; leaving for the picker or calling app uses the existing disconnect/reconnect lifecycle.

Manual verification: add contacts with and without photos, select among multiple numbers, cancel the picker, try a duplicate, fill all four slots, remove/re-add a contact, and restart the app. Check large targets in portrait/landscape and large text, animation settings, 10-second dismissal/reset, and extended accessibility timeouts. With an agreed test number, verify direct calls after permission grant, denial/retry/settings/revocation, and dual-SIM behavior. Confirm Back dismisses the overlay first, contacts never trigger playback gestures, and Spotify reconnects on return. Automated tests exercise callbacks and call intent construction without placing real calls.

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
5. Copy `app/src/main/java/com/example/ridewave/spotify/SpotifyConfig.kt.example` to `app/src/main/java/com/example/ridewave/spotify/SpotifyConfig.kt` and set the client ID for local development. The copied file is ignored by Git. The template includes the initial Web API scopes used by `SpotifyWebApi`.
6. If the Spotify application is in Development Mode, add the Spotify account used for testing to the application's allowed users.

The redirect URI must match in all three places: Spotify Developer Dashboard, `SpotifyConfig.kt`, and the manifest intent filter. The package name and SHA-1 must match the installed build's signing identity.

### Credential safety

Do not commit a real client ID or other credentials to a public repository. `SpotifyConfig.kt` is ignored by Git; commit only the `SpotifyConfig.kt.example` template. A Spotify client ID is not a secret by itself, but keeping it local makes environment management and rotation easier.

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

Onboarding completion and authorization state are remembered in app preferences. After setup, subsequent launches open ride mode and reconnect in the background. Connection failures open the connection page with a retry action. Successful connection opens ride mode automatically. The close icon and Android Back exit the app; completed onboarding and connection pages are not placed in a back stack.

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
