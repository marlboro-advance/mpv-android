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

## Installation

Add this to your project-level build.gradle:

```gradle
allprojects {
    repositories {
        // Your other repositories
        maven { url 'https://jitpack.io' }
    }
}
```

Add the dependency to your module-level build.gradle:

```gradle
dependencies {
    implementation 'com.github.marlboro-advance:mpv-lib:Tag'
}
```

For Clojure/Leiningen:
```clojure
:dependencies [[com.github.marlboro-advance/mpv-lib "Tag"]]
```

Replace "Tag" with the specific version you want to use (e.g., "0.1.10").

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