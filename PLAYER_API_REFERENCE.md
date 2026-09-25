# mpvlib Android: API Reference & Integration Guide (v2.0.2)

> **Context for AI Models & Developers**: This document details the complete Kotlin/Android API for `com.github.marlboro-advance:mpv-android`. It covers core playback management, reactive Kotlin Coroutine StateFlow property observation, complex command execution via `MPVNode`, and direct hardware-accelerated thumbnail generation via `FastThumbnails`.

---

## 1. Dependency Setup

### JitPack (`settings.gradle` or root `build.gradle`)
```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

### Module `build.gradle`
```kotlin
dependencies {
    implementation("com.github.marlboro-advance:mpv-android:2.0.2")
}
```

---

## 2. Architecture Overview

Package: `is.xyz.mpv`

| Class / Object | Role |
| :--- | :--- |
| `MPVLib` | Main low-level singleton binding to `libmpv` and `libplayer.so`. Exposes commands, property getters/setters, reactive Kotlin `StateFlow`s, and event flows. |
| `BaseMPVView` | Abstract `SurfaceView` managing the rendering surface, context lifecycle, and automatic property observation. |
| `FastThumbnails` | Standalone, high-speed FFmpeg-based thumbnail extractor supporting hardware decoding without creating an mpv playback instance. |
| `MPVNode` | Type-safe Kotlin sealed class representing mpv's variant node structures (maps, arrays, primitives) with JSON serialization. |
| `Utils` | System helpers for asset copying, fontconfig setup (`fonts.conf`), file path resolution, and media extensions. |
| `KeyMapping` | Maps Android `KeyEvent` keycodes to mpv input keys. |

---

## 3. `MPVLib` API Reference

### 3.1 Lifecycle & Setup
```kotlin
// Initialize libmpv context with Application Context (prevents Activity memory leaks)
MPVLib.create(context.applicationContext)

// Set startup options (must be called before init())
MPVLib.setOptionString("vo", "gpu")
MPVLib.setOptionString("hwdec", "mediacodec-copy")
MPVLib.setOptionString("config", "yes")
MPVLib.setOptionString("config-dir", context.filesDir.path)

// Finalize initialization
MPVLib.init()

// Attach/Detach Android Surface
MPVLib.attachSurface(surface: Surface)
MPVLib.detachSurface()

// Destroy player instance
MPVLib.destroy()
```

### 3.2 Executing Commands
Commands accept varargs strings matching mpv's IPC commands:
```kotlin
// Load a file
MPVLib.command("loadfile", "https://example.com/video.mp4")

// Playback control
MPVLib.command("seek", "10", "relative")
MPVLib.command("cycle", "pause")
MPVLib.command("playlist-next")
MPVLib.command("playlist-prev")

// Command returning structured MPVNode (e.g., track list)
val trackList: MPVNode? = MPVLib.commandNode("get_property_native", "track-list")
```

### 3.3 Reactive Kotlin Property StateFlows (Recommended)
Instead of polling or setting up verbose observer callbacks, use reactive StateFlows:

```kotlin
// Read or observe properties as StateFlows:
val isPausedFlow: StateFlow<Boolean?> = MPVLib.propBoolean["pause"]
val positionFlow: StateFlow<Int?>      = MPVLib.propInt["time-pos"]
val durationFlow: StateFlow<Double?>   = MPVLib.propDouble["duration"]
val titleFlow: StateFlow<String?>      = MPVLib.propString["media-title"]
val tracksFlow: StateFlow<MPVNode?>    = MPVLib.propNode["track-list"]

// Set properties directly using operator syntax:
MPVLib.propBoolean["pause"] = true
MPVLib.propInt["time-pos"] = 60
MPVLib.propDouble["speed"] = 1.25
MPVLib.propString["sub-delay"] = "+0.1"

// Collecting in a Coroutine / LifecycleScope:
lifecycleScope.launch {
    MPVLib.propInt["time-pos"].collect { currentSec ->
        currentSec?.let { updateSeekBar(it) }
    }
}
```

Available Property Types:
* `MPVLib.propBoolean` -> `StateFlow<Boolean?>`
* `MPVLib.propInt` -> `StateFlow<Int?>`
* `MPVLib.propLong` -> `StateFlow<Long?>`
* `MPVLib.propFloat` -> `StateFlow<Float?>`
* `MPVLib.propDouble` -> `StateFlow<Double?>`
* `MPVLib.propString` -> `StateFlow<String?>`
* `MPVLib.propNode` -> `StateFlow<MPVNode?>`

### 3.4 Direct Getters & Setters
```kotlin
// Getters (return nullable primitives)
fun getPropertyBoolean(property: String): Boolean?
fun getPropertyInt(property: String): Int?
fun getPropertyLong(property: String): Long?
fun getPropertyFloat(property: String): Float?
fun getPropertyDouble(property: String): Double?
fun getPropertyString(property: String): String?
fun getPropertyNode(property: String): MPVNode?

// Setters
fun setPropertyBoolean(property: String, value: Boolean)
fun setPropertyInt(property: String, value: Int)
fun setPropertyLong(property: String, value: Long)
fun setPropertyFloat(property: String, value: Float)
fun setPropertyDouble(property: String, value: Double)
fun setPropertyString(property: String, value: String)
fun setPropertyNode(property: String, node: MPVNode)
```

### 3.5 Reactive Event & Log Flows
```kotlin
// Observe specific mpv property triggers
lifecycleScope.launch {
    MPVLib.eventFlow("eof-reached").collect {
        // Handle playback finished
    }
}

// Observe specific MPV_EVENT ID
lifecycleScope.launch {
    MPVLib.eventFlow(MPVLib.MpvEvent.MPV_EVENT_FILE_LOADED).collect {
        // File loaded successfully
    }
}

// Observe internal mpv logs (prefix, level, message)
lifecycleScope.launch {
    MPVLib.logFlow.collect { (prefix, level, message) ->
        Log.d("mpv", "[$prefix] ($level) $message")
    }
}
```

---

## 4. `MPVNode` Reference

`MPVNode` is a sealed class representing all mpv data structures.

### 4.1 Subtypes
```kotlin
MPVNode.None
MPVNode.StringNode(val value: String)
MPVNode.BooleanNode(val value: Boolean)
MPVNode.IntNode(val value: Long)
MPVNode.DoubleNode(val value: Double)
MPVNode.ByteArrayNode(val value: ByteArray)
MPVNode.ArrayNode(val value: Array<MPVNode>)
MPVNode.MapNode(val value: Map<String, MPVNode>)
```

### 4.2 Helper Methods & Accessors
```kotlin
val node: MPVNode = MPVLib.getPropertyNode("track-list") ?: MPVNode.None

// Typed conversions (returns null if type mismatch)
val str: String? = node.asString()
val bool: Boolean? = node.asBoolean()
val num: Long? = node.asInt()
val dbl: Double? = node.asDouble()
val bytes: ByteArray? = node.asByteArray()
val list: Array<MPVNode>? = node.asArray()
val map: Map<String, MPVNode>? = node.asMap()

// Indexing
val firstTrack = node[0]                     // Array indexing
val trackTitle = node[0]?["title"]?.asString() // Nested Map indexing

// JSON serialization
val jsonString: String = node.toJson()
```

---

## 5. `FastThumbnails` API Reference

`FastThumbnails` provides direct, high-performance hardware-accelerated video thumbnail generation using direct FFmpeg decoders without spinning up an mpv player instance.

### 5.1 Initialization
Call once in your `Application.onCreate()` or before generation:
```kotlin
FastThumbnails.initialize(context.applicationContext)
```

### 5.2 Extracting Thumbnails
```kotlin
// Synchronous (blocking)
val bitmap: Bitmap? = FastThumbnails.generate(
    path = "/sdcard/Movies/sample.mp4",
    position = 30.0,    // Time position in seconds (Double)
    dimension = 512,    // Max width or height in pixels (1..4096)
    useHwDec = true     // Hardware decoding enabled
)

// Asynchronous (Kotlin Coroutine, runs on Dispatchers.IO)
val bitmap: Bitmap? = FastThumbnails.generateAsync(
    path = videoUrl,
    position = 45.5,
    dimension = 256,
    useHwDec = true
)

// Extract multiple thumbnails at once
val timestamps = listOf(10.0, 20.0, 30.0, 40.0)
val thumbnails: List<Bitmap?> = FastThumbnails.generateMultipleAsync(
    path = videoUrl,
    positions = timestamps,
    dimension = 320
)

// Free codec cache on memory warning
FastThumbnails.clearCache()
```

---

## 6. `BaseMPVView` Implementation Pattern

To build a custom video player UI, subclass `BaseMPVView`:

```kotlin
class MyPlayerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : BaseMPVView(context, attrs ?: ...) {

    override fun initOptions() {
        // Called before MPVLib.init()
        MPVLib.setOptionString("hwdec", "mediacodec-copy")
        MPVLib.setOptionString("vo", "gpu")
    }

    override fun postInitOptions() {
        // Called after MPVLib.init()
        MPVLib.setOptionString("sub-font-size", "45")
    }

    override fun observeProperties() {
        // Any custom observers if needed
    }
}
```

### In Activity / Fragment:
```kotlin
class PlayerActivity : AppCompatActivity() {
    private lateinit var playerView: MyPlayerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Copy CA certificates and generate fonts.conf
        Utils.copyAssets(applicationContext)

        playerView = MyPlayerView(this)
        setContentView(playerView)

        // Initialize with app filesDir and cacheDir
        playerView.initialize(
            configDir = filesDir.path,
            cacheDir = cacheDir.path
        )

        // Queue the file to play when surface is ready
        playerView.playFile("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4")

        // Observe progress via StateFlow
        lifecycleScope.launch {
            MPVLib.propInt["time-pos"].collect { pos ->
                findViewById<TextView>(R.id.currentTime).text = Utils.prettyTime(pos ?: 0)
            }
        }
    }

    override fun onDestroy() {
        playerView.destroy()
        super.onDestroy()
    }
}
```
