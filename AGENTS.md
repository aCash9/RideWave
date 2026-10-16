# Repository guidelines

This document describes how to work on RideWave. It is intentionally tool-agnostic and applies to people and automated contributors alike.

## Project scope

RideWave is a single-module Kotlin Android application. It uses Jetpack Compose for the UI and Spotify Authentication/App Remote for playback control. The app is currently a small prototype, so keep changes focused and avoid introducing architecture or dependencies without a concrete need.

## Before making changes

- Read the relevant source files and the root `README.md`.
- Check the working tree before editing so existing user changes are preserved.
- Confirm whether a change affects Spotify setup, the redirect URI, Android manifest behavior, or device-only functionality.
- Do not commit credentials, signing keys, keystores, generated build output, IDE state, or local machine configuration.

## Code organization

- Put activity and Compose screen behavior in `app/src/main/java/com/example/ridewave/MainActivity.kt` or an appropriate UI package.
- Keep Spotify SDK calls and connection lifecycle logic in the `spotify` package.
- Keep gesture recognition and gesture-to-action mapping in the `gestures` package.
- Keep theme and reusable visual styling in `ui/theme`.
- Prefer existing AndroidX and Kotlin facilities over adding a dependency for a small helper.
- Match the existing Kotlin formatting and naming conventions.
- Keep callbacks and lifecycle cleanup explicit; Spotify connections must be disconnected when the activity stops.

## Configuration and security

- Treat `SpotifyConfig.kt`, the manifest callback URI, and the Spotify Developer Dashboard as one configuration surface.
- Never add access tokens, client secrets, keystores, passwords, or personal credentials to source control.
- Use ignored local configuration or a secure build-time mechanism for values that vary by environment.
- If changing the package name, redirect URI, or signing configuration, document every corresponding setup change.

## Validation

Run the smallest useful set of checks for the change, then expand it when practical:

```bash
./gradlew assembleDebug
./gradlew test
./gradlew lint
```

Run `./gradlew connectedAndroidTest` when changing Android lifecycle, manifest, Compose interaction, or device behavior and a connected device/emulator is available. Spotify behavior must also be tested on a device with Spotify installed and an authorized test account.

When a check cannot run, report that clearly and include the reason. Do not hide build or test failures by changing unrelated configuration.

## UI and interaction expectations

- Preserve the current landscape, fullscreen, screen-awake behavior unless the task explicitly changes it.
- Ensure gesture changes do not interfere with visible controls or accessibility semantics.
- Provide useful user-facing feedback for authorization, connection, timeout, and missing-app failures.
- Keep playback actions safe when Spotify is disconnected; do not assume the App Remote object is always available.

## Documentation

Update `README.md` when setup steps, supported behavior, project structure, build commands, or known limitations change. Keep examples generic and avoid exposing local paths or credentials. Prefer documenting the reason for a non-obvious integration decision, especially around Spotify authorization and lifecycle handling.

## Change quality

Keep commits and patches narrow and reviewable. Avoid drive-by formatting changes. New behavior should include a test when it can be tested without Spotify, and manual verification notes when it depends on Spotify or Android device behavior.
