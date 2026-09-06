# CompassApp — Kotlin Multiplatform location + heading demo

A corrected, working KMP + Compose Multiplatform project: shows the device's
current GPS coordinates (via the real `jordond/compass` library) and its
compass heading (via native sensors), on both Android and iOS.

## What was wrong in the original draft, and what changed

1. **Wrong Maven group id.** The library's real group is `dev.jordond.compass`,
   not `com.jordond.compass`. Wrong coordinates would fail to resolve at all.
2. **Stale version.** Pinned to the current release, `3.0.2` (verified against
   Maven Central / the project's GitHub releases), not `3.0.0`.
3. **`String.format()` and `System.currentTimeMillis()` in `commonMain`.**
   Both are JVM-only APIs and don't compile in shared Kotlin Multiplatform
   code. Replaced with `kotlin.math` (`round`, `pow`) for formatting.
4. **`getApplicationContext()` didn't exist.** The Android sensor code called
   an undefined function to get a `Context`. Replaced with a small
   `androidx.startup.Initializer` (`CompassContextInitializer.kt`) that
   captures the application `Context` automatically at process start — no
   manual `Application` subclass required.
5. **iOS `CLLocationManagerDelegateProtocol` override was malformed**
   (`didFailWithError: Any?` isn't a real Core Location callback signature).
   Fixed to the real `locationManager(_:didFailWithError:)` signature.
6. **The Android `:app` module wrongly applied the Kotlin Multiplatform
   plugin.** A plain Android application module doesn't need it — it just
   needs `com.android.application` + `org.jetbrains.kotlin.android` +
   the Compose compiler, and to depend on `:shared`. Fixed.
7. **The Compass library doesn't provide a heading/magnetometer API** — it's
   a *geolocation + geocoding* library only. So the compass/heading part of
   this project is intentionally hand-written platform sensor code
   (`SensorManager` on Android, `CLLocationManager.startUpdatingHeading()`
   on iOS), wired through a clean `expect`/`actual` (`PlatformHeadingProvider`).
8. **No location permission needs to be declared manually on Android** — the
   `geolocation-mobile` artifact merges `ACCESS_FINE_LOCATION` /
   `ACCESS_COARSE_LOCATION` into your manifest automatically. Don't
   duplicate it.
9. **iOS requires `NSLocationWhenInUseUsageDescription`** in `Info.plist` —
   included here.
10. **`KotlinSourceSet with name 'iosMain' not found`.** Kotlin's "default
    hierarchy template" is supposed to auto-create `iosMain` once you declare
    `iosX64()`/`iosArm64()`/`iosSimulatorArm64()` targets, but it doesn't
    reliably kick in with every Kotlin/Compose Multiplatform plugin version
    combination. Fixed by creating `iosMain` explicitly with `by creating`
    and wiring `iosX64Main`/`iosArm64Main`/`iosSimulatorArm64Main` to
    `dependsOn(iosMain)` by hand, instead of relying on the implicit template.
11. **`AAR metadata` check failure: "requires compileSdk 36" / "requires AGP
    8.9.1 or higher".** Several transitive dependencies pulled in by Compass
    and Compose Multiplatform (`androidx.activity:activity-compose:1.12.4`,
    `androidx.core:core-ktx:1.16.0`, `androidx.compose.ui:ui:1.9.2`, and the
    Compass Android artifacts themselves) require `compileSdk 36` and a newer
    AGP than 8.5.0. Fixed by bumping **AGP to 8.13.0** (supports up to API
    36.1), **`compileSdk`/`targetSdk` to 36** in both `:shared` and `:app`,
    and the **Gradle wrapper to 8.13** (AGP 8.13's minimum Gradle
    requirement) — all three have to move together or you get a fresh
    version-mismatch error instead.
12. **Kotlin compiler crash: `Module was compiled with an incompatible
    version of Kotlin. The binary version of its metadata is 2.3.0, expected
    version is 2.0.0.`** Compass 3.0.2's published artifacts were compiled
    with a newer Kotlin than this project's pinned `2.0.20`, which crashes
    the K2 frontend outright (not just a warning). Fixed by bumping
    **Kotlin to 2.4.10** (current stable) and **Compose Multiplatform to
    1.11.0** together — Compose Multiplatform's latest release always tracks
    the latest Kotlin, so they move as a pair. Also bumped `kotlinx-coroutines`
    to 1.11.0 and `kotlinx-datetime` to 0.7.1 to match.

## Project layout

```
CompassApp/
├── gradle/libs.versions.toml     # Version catalog (corrected coordinates)
├── settings.gradle.kts
├── build.gradle.kts
├── shared/                       # KMP module: all logic + Compose UI
│   ├── build.gradle.kts
│   └── src/
│       ├── commonMain/           # CompassRepository, HeadingData, Compose UI
│       ├── androidMain/          # SensorManager heading + Context holder
│       └── iosMain/              # CoreLocation heading + UIViewController factory
├── app/                          # Thin Android launcher (depends on :shared)
└── iosApp/                       # Thin iOS SwiftUI wrapper (depends on Shared.framework)
```

## Building

### Android
```bash
./gradlew :app:installDebug
```
The Gradle wrapper (`gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`)
is included and pinned to Gradle 8.7, the version AGP 8.5.0 expects. If you
previously hit `Unable to load class 'org.gradle.api.internal.plugins.
DefaultArtifactPublicationSet'`, that was caused by an IDE/local Gradle
version mismatch from the missing wrapper — re-download this project and
open it fresh (File > Sync Project with Gradle Files); don't reuse an old
`.idea`/`.gradle` cache from the previous version of this project.

### iOS
This project doesn't include a generated `.xcodeproj` (it's a binary-ish
Xcode project format, not something to hand-write as text). To build for iOS:

1. `cd shared && ../gradlew :shared:embedAndSignAppleFrameworkForXcode` (or
   let Xcode's build phase do this) to produce `Shared.framework`.
2. Easiest path: open Android Studio with the KMP plugin, or create a new
   Xcode project ("App", SwiftUI, in `iosApp/`) and drag in the provided
   `iOSApp.swift` / `ContentView.swift` / `Info.plist`, then add a **Run
   Script** build phase calling the Gradle `embedAndSignAppleFrameworkForXcode`
   task (this is the standard KMP + Xcode wiring — see JetBrains' KMP wizard
   output for the exact script if you want a byte-for-byte reference).

## Notes on the `Location` type

`location.coordinates.latitude` / `.longitude` are used here, matching the
usage shown in Compass's own docs (`println(status.location.coordinates)`).
If you need altitude/accuracy/speed, check the current KDoc at
https://docs.compass.jordond.dev for the exact field names on `Location` —
I intentionally kept this demo to the fields directly confirmed in the
library's own documentation rather than guess at unverified ones.
