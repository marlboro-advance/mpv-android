# Upstream Merge Changelog: mpv-android (v2.0.1)

This document provides a detailed breakdown of all changes, updates, integrations, and architectural decisions introduced by merging `upstream/master` into this library repository.

---

## 1. Summary of Changes

* **Upstream commits merged**: 60+ upstream commits (from `8454d8a` through `fdf74f6`).
* **Conflict resolution principle**: Upstream's standalone player app UI components (`MPVActivity`, `FilePickerActivity`, UI fragments, layouts, and translations) were omitted to maintain this repository as a pure, lightweight Android library (`mpvlib`).
* **Custom fork features retained**: Direct FFmpeg fast thumbnails, Vulkan renderer integration, Kotlin Coroutine StateFlows, `MPVNode` data model, and custom build scripts.
* **New upstream features integrated**: Fontconfig subtitle rendering, libcurl integration, updated CA bundle, JNI memory/lifecycle fixes, updated dependencies (FFmpeg n9.0, HarfBuzz, mbedtls, etc.), Gradle 9.4.1, AGP 9.2.1, Kotlin 2.2.21, and Android SDK 36.

---

## 2. File-by-File Detailed Changes

### A. Android & Kotlin Layer

#### 1. `app/build.gradle`
* **Library Plugin**: Preserved `apply plugin: 'com.android.library'`.
* **AGP 9 Compatibility**: Removed `apply plugin: 'kotlin-android'` (AGP 9.0+ integrates Kotlin directly).
* **SDK Upgrades**:
  * `compileSdk` updated to `36`.
  * `targetSdk` updated to `36`.
  * `minSdkVersion` maintained at `24` (required for Vulkan support).
* **Dependencies**:
  * Bumped `androidx.appcompat:appcompat` to `1.7.1`.
* **Packaging & Publishing**:
  * Configured `sourceSets.main.jniLibs.srcDirs = ['src/main/libs']` to ensure `.so` libraries from `ndk-build` are packaged into the output AAR.
  * Added `maven-publish` and `signing` plugins with release publication metadata and GPG signing for Maven Central.
  * Loaded `ndk.properties` for NDK path detection.

#### 2. `app/src/main/java/is/xyz/mpv/BaseMPVView.kt`
* **Context Leak Fix**: Changed `MPVLib.create(context)` to `MPVLib.create(context.applicationContext)`. This prevents permanent references to `Activity` instances, preventing memory leaks when views or activities are recreated.

#### 3. `app/src/main/java/is/xyz/mpv/Utils.kt`
* **Fontconfig Subtitle Support**: Added `writeFontsConf(context, configFile)` to generate XML font configuration for fontconfig, allowing libass to find system fonts (`Roboto`, `Noto Sans`, `Noto Serif`, `Droid Sans Mono`).
* **Subfont Removal**: Removed bundled `subfont.ttf` copy logic and added automatic deletion of old `subfont.ttf` files in app storage.
* **Expanded Media Extensions**: Added upstream supported extensions:
  * Playlists: `strm`
  * Audio: `alac`, `lc3`, `qoa`
  * Video: `266`, `h266`, `vvc`
  * Image: `avif`, `heic`, `heif`, `qoi`
* **App UI Stripping**: Omitted upstream app-specific UI helpers (`OpenUrlDialog`, `PlaybackStateCache`, `handleInsetsAsPadding`) that required media session and resource files not present in the library.

#### 4. `app/src/main/java/is/xyz/mpv/KeyMapping.kt`
* Cleaned up Kotlin translation to expose `val keyMapping = mapOf(...)` without JVM signature clashes.

#### 5. `app/src/main/assets/cacert.pem`
* Updated root CA certificate bundle from upstream.

#### 6. `app/src/main/assets/subfont.ttf`
* Deleted bundled 14MB TrueType font asset. Font rendering is now delegated to fontconfig, reducing library binary size.

#### 7. `app/proguard-rules.pro`
* Added upstream ProGuard rules to preserve `is.xyz.mpv.MPVLib` JNI entry points from R8/ProGuard obfuscation.

---

### B. Native & JNI Layer

#### 1. `app/src/main/jni/main.cpp`
* **Local Reference Leak Fix**: Added `env->DeleteLocalRef(strings[i])` in `command()` to prevent JNI local reference table exhaustion during rapid command invocations.
* **Global Reference Management**: Properly managed `global_appctx` by deleting existing global references before creating new ones.
* **Initialization Order**: Fixed `prepare_environment()` execution ordering and verified JVM retrieval.
* **Custom JNI Preserved**: Retained `commandNode` JNI implementation for structured `MPVNode` queries.

#### 2. `app/src/main/jni/Android.mk`
* Preserved `node.cpp` source compilation.
* Kept linking against `avformat`, `avutil`, `swscale`, `GLESv3`, `EGL`, and `mpv` required for fast thumbnail generation and Vulkan/GLES rendering.

#### 3. `app/src/main/jni/Application.mk`
* Retained `APP_PLATFORM := android-24` (needed for Vulkan and modern NDK compilation).
* Maintained `APP_SUPPORT_FLEXIBLE_PAGE_SIZES := true` (support for 16KB page size devices).

---

### C. Build Scripts & Native Toolchain

#### 1. `buildscripts/include/depinfo.sh`
* Updated dependency versions:
  * NDK: `r30` (`30.0.16248370`)
  * SDK platform / build tools: `36`
  * HarfBuzz: `14.4.0`
  * Fribidi: `1.0.16`
  * FreeType: `2.14.3`
  * mbedtls: `3.6.7`
  * libxml2: `2.15.4`
  * fontconfig: `2.18.3`
  * curl: `8.21.0`
  * FFmpeg revision: `n9.0`
* Retained `shaderc` dependency for `libplacebo` to support Vulkan shaders.
* Integrated `curl` and `fontconfig` into dependency chains.

#### 2. `buildscripts/scripts/mpv.sh`
* Meson setup flags updated to enable `lua`, `libcurl`, and `vulkan`:
  `-Diconv=disabled -D{lua,libcurl,vulkan}=enabled`

#### 3. New Build Scripts Added
* `buildscripts/scripts/curl.sh`: Builds `libcurl` with `mbedtls` for network streaming.
* `buildscripts/scripts/fontconfig.sh`: Builds `fontconfig` with `libxml2` and `freetype2`.
* `buildscripts/scripts/libxml2.sh`: Builds `libxml2` required by `fontconfig`.

#### 4. `buildscripts/buildall.sh`
* Toolchain detection updated for LLVM binutils (`llvm-ar`, `llvm-ranlib`).
* Target triple handling updated for clang compilers.
* Output reporting updated to check for `.aar` artifacts alongside `.apk`.

#### 5. `buildscripts/scripts/mpv-android.sh`
* Added automatic `ndk.properties` generation for Android Studio / Gradle.
* Preserved execution of `write_versions.sh` to embed version constants into `Utils.kt`.

#### 6. `buildscripts/include/download-sdk.sh` & `ci.sh`
* Added `gperf` and `nasm` to host package installation.
* Updated CI build script for multi-architecture prefix compilation (`armv7l`, `arm64`).

---

### D. Gradle & Project Infrastructure

#### 1. Gradle Wrapper
* Upgraded Gradle from `8.14.3` to `9.4.1`.
* Updated `gradle/wrapper/gradle-wrapper.jar` and `gradle/wrapper/gradle-wrapper.properties`.
* Updated POSIX shell wrapper script `gradlew`.

#### 2. Root `build.gradle`
* Upgraded Android Gradle Plugin to `9.2.1`.
* Upgraded Kotlin Gradle Plugin to `2.2.21`.

#### 3. Maven Central & Sonatype Portal Publishing
* Automated Maven Central publishing with Sonatype Central Portal API integration, GPG signing, and complete POM metadata (`io.github.marlboro-advance:mpv-android`).
