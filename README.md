# CompassApp

Kotlin Multiplatform (Android + iOS) compass/location app built with Compose
Multiplatform, `dev.jordond.compass` for geolocation, and a hand-rolled
`expect`/`actual` `PlatformHeadingProvider` for magnetometer/device heading.

## Before you build

1. Create `local.properties` in the project root pointing at your Android SDK
   (this file is machine-specific and intentionally not included):
   ```
   sdk.dir=/path/to/your/Android/sdk
   ```
2. This bundle does **not** include `gradle/wrapper/gradle-wrapper.jar`
   (binary files can't travel through the export used to package this zip).
   Regenerate it once you have Gradle installed locally:
   ```
   gradle wrapper --gradle-version 8.13
   ```
   or open the project in Android Studio, which will offer to do this for
   you automatically.
3. Android: open the project root in Android Studio and run the `app`
   configuration.
4. iOS: open `iosApp/iosApp.xcodeproj` (or the workspace, if you add
   CocoaPods) in Xcode and run it — you'll need Xcode's usual iOS toolchain
   set up. Note `iosApp.xcodeproj` itself isn't included since Xcode project
   files are binary/structured in a way this export can't carry either; the
   Swift sources are all here under `iosApp/iosApp/`, so recreate the Xcode
   project shell and add these files if you don't already have one.

## What changed in this drop

`shared/src/commonMain/kotlin/com/example/compassapp/ui/CompassScreen.kt` —
`CircularCompass()`:

- Removed the semi-transparent "white tail" line that pointed south inside
  the rotating dial group (it wasn't part of the target design).
- Replaced the plain 6dp center dot with a fixed crosshair reticle (gray
  dot + white cross) drawn *after* the `rotate { ... }` block, so it stays
  screen-fixed and doesn't spin with the dial as heading changes — matching
  the reference screenshot.

Everything else (tick marks, N/E/S/W + degree labels, the red north
indicator, the fixed top triangle, the dial's rotate-with-heading behavior)
was already correct and is unchanged.
