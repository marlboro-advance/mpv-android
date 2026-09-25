# mpvlib Android

Android library based on libmpv for video playback and thumbnail generation.

## Overview

This library provides Android bindings for the mpv media player, allowing developers to integrate mpv's powerful video playback capabilities and thumbnail generation into their Android applications.

## Features

- Full mpv media player integration
- Video thumbnail generation
- Support for multiple ABIs: armeabi-v7a, arm64-v8a, x86, x86_64
- Kotlin-friendly API

## Requirements

- Android 5.0 (API level 21) or higher
- Android Studio with Gradle support

### Option 1: Via JitPack (Recommended)

Add the JitPack repository to your root `settings.gradle` or root `build.gradle`:

```gradle
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

Add the dependency to your module `build.gradle`:

```gradle
dependencies {
    implementation("com.github.marlboro-advance:mpv-android:2.0.1")
}
```

### Option 2: Pre-built AAR

1. Download the pre-built AAR from the [releases page](https://github.com/marlboro-advance/mpv-android/releases)
2. Create a `libs` directory in your app module if it doesn't exist
3. Copy the AAR file (e.g., `mpv-android-lib-v2.0.1.aar`) to the `libs` directory
4. Add to your module `build.gradle`:

```gradle
dependencies {
    implementation(files("libs/mpv-android-lib-v2.0.1.aar"))
}
```

## Usage

Basic implementation example:

```kotlin
// Import the mpv library
import is.xyz.mpv.MPVLib

// Initialize the player
MPVLib.initialize(context)

// Load and play a video file
MPVLib.loadFile("path/to/video.mp4")

// Handle playback controls
MPVLib.play()
MPVLib.pause()
```

## Building

The library can be built using the provided build scripts:

```bash
./buildscripts/buildall.sh
```

Docker build is also supported:

```bash
./buildscripts/docker-build.sh
```

## License

This project is licensed under the MIT License - see the [LICENSE](https://github.com/marlboro-advance/mpv-lib/blob/main/LICENSE) file for details.

## Acknowledgments

- [mpv](https://mpv.io/) - The underlying media player
- Original authors: Ilya Zhuravlev and sfan5