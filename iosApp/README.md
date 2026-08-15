# iOS app shell

This directory holds the Swift entry point for the iOS target (`iOSApp.swift`,
`ContentView.swift`, `Info.plist`), which host the shared Compose Multiplatform UI via
`MainViewController()` (see `app/src/iosMain/kotlin/.../MainViewController.kt`).

**Not yet included:** an `iosApp.xcodeproj`. Kotlin/Native cannot compile the `iosArm64` /
`iosX64` / `iosSimulatorArm64` targets declared in `app/build.gradle.kts` on a non-macOS host,
so this project was scaffolded and validated on Windows without attempting to build or open
Xcode. To finish the iOS side on a Mac:

1. Open this repo in Android Studio or run `./gradlew :app:embedAndSignAppleFrameworkForXcode`
   (or use the JetBrains "KMP" Xcode integration) to confirm the `RickAndMortyApp` framework
   builds for the simulator/device.
2. Create an Xcode project (`File > New > Project > iOS App`) named `iosApp` in this directory,
   pointing its sources at the existing `iosApp/iosApp/*.swift` and `Info.plist` files instead of
   generating new ones.
3. Add a "Run Script" build phase (or use the Kotlin CocoaPods/Swift Package Manager export)
   that links the `RickAndMortyApp.framework` produced by `:app`'s iOS targets, matching the
   `baseName` configured in `app/build.gradle.kts`.
4. Build/run on a simulator to verify the Home screen renders.

This is tracked as a follow-up for whoever picks up iOS work on macOS tooling.
