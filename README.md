# mpv-android Library

Android library based on [libmpv](https://github.com/mpv-player/mpv) for video playback and thumbnail generation.

## Features

### Video Playback
- Hardware and software video decoding
- libass support for styled subtitles
- High-quality rendering with advanced settings
- Background playback, Picture-in-Picture support

### Fast Thumbnail Generation ⚡
- **50-100ms per thumbnail** using direct FFmpeg API
- Hardware acceleration via Android MediaCodec
- Multi-threaded frame decoding
- Supports 99%+ of video formats

## Quick Start: Thumbnails

```kotlin
// 1. Initialize once (in Application.onCreate)
FastThumbnails.initialize(context)

// 2. Generate thumbnails anywhere
lifecycleScope.launch {
    val bitmap = FastThumbnails.generateAsync(
        "/sdcard/video.mp4", 
        position = 10.0, 
        dimension = 256
    )
    imageView.setImageBitmap(bitmap)
}
```

**Performance**: 20-30x faster than traditional methods!

## Documentation

- **[Quick Start](QUICK_START.md)** - 30-second thumbnail setup
- **[Thumbnail Guide](THUMBNAIL_GUIDE.md)** - Complete documentation
- **[Thumbnails Overview](THUMBNAILS_README.md)** - Features & implementation

## Library Usage

This is **not** a standard AAR you can import directly. If you want to use libmpv in your app:

### Key Components
- [`MPVLib`](app/src/main/java/is/xyz/mpv/MPVLib.kt) - Main library interface
- [`BaseMPVView`](app/src/main/java/is/xyz/mpv/BaseMPVView.kt) - View component
- [`FastThumbnails`](app/src/main/java/is/xyz/mpv/FastThumbnails.kt) - Thumbnail generation
- [Native code](app/src/main/jni/) - JNI implementation

### Building

Native dependencies are built by [these scripts](buildscripts/):

```bash
cd buildscripts
./buildall.sh
```

Then build the Android library:

```bash
./gradlew assembleRelease
```

## Maven Publication

Published as: `io.github.abdallahmehiz:mpv-android-lib`

```gradle
dependencies {
    implementation 'io.github.abdallahmehiz:mpv-android-lib:0.1.10'
}
```

## Requirements

- **Min SDK**: 21 (Android 5.0)
- **Target SDK**: 34
- **Compile SDK**: 35

## Architecture

```
┌─────────────────────────────────────┐
│  Kotlin API Layer                   │
│  - MPVLib (playback)                │
│  - FastThumbnails (thumbnails)      │
└──────────────┬──────────────────────┘
               │ JNI
┌──────────────▼──────────────────────┐
│  Native C++ Layer                   │
│  - libmpv integration               │
│  - FFmpeg direct API (thumbnails)   │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│  Native Libraries                   │
│  - libmpv.so                        │
│  - libavcodec/avformat/avutil.so    │
└─────────────────────────────────────┘
```

## License

MIT License - See [LICENSE](LICENSE) file

## Credits

Based on [mpv-android](https://github.com/mpv-android/mpv-android) by the mpv-android team.

Fast thumbnail generation implementation by Abdallah Mehiz.
