# mpv-android: JitPack + Maven AAR Setup

## Objective

Modify this repository so that JitPack can build `mpv-android` from source, including the native `.so` libraries, and publish a usable Maven AAR.

Desired consumer dependency:

```gradle
implementation("com.github.marlboro-advance:mpv-android:<tag>")
```

First test release:

```text
v0.0.2
```

The most important requirement:

> The generated AAR MUST contain the native `.so` libraries, especially `libmpv.so` and `libplayer.so`.

Do not replace or redesign the existing native build system. It already contains the required build pipeline.

---

# 1. Repository Architecture

Important directories:

```text
app/
buildscripts/
build.gradle
settings.gradle
```

The Android library module is:

```text
app/
```

The native build system is:

```text
buildscripts/
```

Native Android build files include:

```text
app/src/main/jni/Android.mk
```

Supported ABIs:

```text
armeabi-v7a
arm64-v8a
x86
x86_64
```

---

# 2. Existing Native Build Pipeline

The existing pipeline is:

```text
buildscripts/ci.sh
        |
        +-- download SDK + NDK
        |
        +-- download dependencies
        |
        +-- obtain/build native prefix
        |
        +-- build mpv
        |
        +-- build mpv-android
        |
        +-- ndk-build
        |
        +-- Gradle assembleRelease
        |
        v
      AAR
```

The intended CI commands are:

```bash
cd buildscripts
IN_CI=1 ./ci.sh install
IN_CI=1 ./ci.sh build
```

`ci.sh build` eventually executes:

```bash
./buildall.sh -n mpv
./buildall.sh -n
```

The second command builds `mpv-android` and eventually runs:

```text
buildscripts/scripts/mpv-android.sh
```

---

# 3. Existing Native Dependency Tree

`buildscripts/include/depinfo.sh` defines:

```text
mpv-android
└── mpv
    ├── ffmpeg
    │   ├── mbedtls
    │   └── dav1d
    ├── libass
    │   ├── freetype2
    │   ├── fribidi
    │   ├── harfbuzz
    │   └── unibreak
    ├── lua
    └── libplacebo
        └── shaderc
```

Do not manually duplicate these dependency commands in the JitPack configuration. Use the existing scripts.

---

# 4. Android SDK / NDK Requirements

`buildscripts/include/depinfo.sh` defines:

```bash
v_sdk=11076708_latest
v_ndk=r29
v_ndk_n=29.0.14206865
v_sdk_platform=35
v_sdk_build_tools=35.0.0
```

Required environment:

```text
Android SDK Platform 35
Android Build Tools 35.0.0
Android NDK 29.0.14206865
Java 17
```

`buildscripts/include/download-sdk.sh` already installs/configures these.

Do not duplicate SDK/NDK installation logic unless absolutely necessary.

---

# 5. Existing Native Library Layout

`buildscripts/scripts/mpv-android.sh` expects native prefixes such as:

```text
buildscripts/prefix/arm64/lib/libmpv.so
buildscripts/prefix/armv7l/lib/libmpv.so
buildscripts/prefix/x86/lib/libmpv.so
buildscripts/prefix/x86_64/lib/libmpv.so
```

It maps them to:

```text
PREFIX32
PREFIX64
PREFIX_X86
PREFIX_X64
```

and invokes:

```bash
ndk-build -C app/src/main
```

The existing `Android.mk` then consumes these prebuilt native libraries.

---

# 6. Android.mk

The existing `app/src/main/jni/Android.mk` selects the prefix by ABI:

```make
ifeq ($(TARGET_ARCH_ABI),armeabi-v7a)
PREFIX = $(PREFIX32)
endif

ifeq ($(TARGET_ARCH_ABI),arm64-v8a)
PREFIX = $(PREFIX64)
endif

ifeq ($(TARGET_ARCH_ABI),x86_64)
PREFIX = $(PREFIX_X64)
endif

ifeq ($(TARGET_ARCH_ABI),x86)
PREFIX = $(PREFIX_X86)
endif
```

It imports libraries including:

```text
libswresample
libpostproc
libavutil
libavcodec
libavformat
libswscale
libavfilter
libavdevice
libmpv
```

and builds:

```text
libplayer.so
```

The final AAR therefore needs to contain the generated native libraries.

---

# 7. Current app/build.gradle

The Android module currently uses:

```gradle
apply plugin: 'com.android.library'
apply plugin: 'kotlin-android'

ext.abiCodes = ["armeabi-v7a": 1, "arm64-v8a": 2, "x86": 3, "x86_64": 4]
ext.universalBase = 8000

version = "0.1.10"

android {
    namespace 'is.xyz.mpv'
    compileSdk 35

    defaultConfig {
        minSdkVersion 24
        targetSdkVersion 34

        versionCode 38
        versionName version
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility JavaVersion.VERSION_17
        targetCompatibility JavaVersion.VERSION_17
    }

    tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile) {
        kotlinOptions.jvmTarget = JavaVersion.VERSION_17.toString()
    }
}

dependencies {
    implementation "org.jetbrains.kotlin:kotlin-stdlib:$kotlin_version"
    implementation 'androidx.appcompat:appcompat:1.7.0'
}
```

There is currently no Maven publishing configuration.

---

# 8. Required Gradle Changes

Modify only what is necessary.

Add:

```gradle
apply plugin: 'maven-publish'
```

to `app/build.gradle`.

Inside the existing `android {}` block, configure release publishing:

```gradle
publishing {
    singleVariant("release") {
        withSourcesJar()
    }
}
```

Then configure Maven publication after the Android configuration:

```gradle
afterEvaluate {
    publishing {
        publications {
            release(MavenPublication) {
                from components.release

                groupId = project.findProperty("GROUP") ?: "com.github.marlboro-advance"
                artifactId = "mpv-android"
                version = project.findProperty("VERSION") ?: project.version
            }
        }
    }
}
```

Do not hardcode the JitPack release version into the project.

The project may retain:

```gradle
version = "0.1.10"
```

for normal local development.

JitPack should be able to override the Maven publication version using:

```text
-PGROUP=com.github.marlboro-advance
-PVERSION=<tag>
```

---

# 9. Create jitpack.yml

Create this file at repository root:

```text
jitpack.yml
```

Initial configuration:

```yaml
jdk:
  - openjdk17

before_install:
  - chmod +x buildscripts/*.sh
  - chmod +x buildscripts/include/*.sh
  - chmod +x buildscripts/scripts/*.sh

install:
  - cd buildscripts
  - IN_CI=1 ./ci.sh install
  - cd ..

build:
  - cd buildscripts
  - IN_CI=1 ./ci.sh build
  - cd ..
  - ./gradlew publishToMavenLocal -PGROUP=com.github.marlboro-advance -PVERSION=$VERSION -x test

artifacts:
  - app/build/outputs/aar/*.aar
```

IMPORTANT:

Before committing this configuration, inspect the actual JitPack lifecycle and existing Gradle tasks.

Avoid building the Android AAR twice unnecessarily.

`ci.sh build` already eventually invokes `assembleRelease` through:

```text
buildscripts/scripts/mpv-android.sh
```

Determine whether `publishToMavenLocal` can consume the already-created release component without triggering another native build.

Prefer the smallest and cleanest solution.

---

# 10. Critical Requirement: Native Libraries Must Be In AAR

After building the release AAR, verify:

```bash
unzip -l app/build/outputs/aar/*.aar | grep '\.so'
```

The result MUST contain native libraries.

For an arm64 build, examples include:

```text
jni/arm64-v8a/libplayer.so
jni/arm64-v8a/libmpv.so
jni/arm64-v8a/libavcodec.so
jni/arm64-v8a/libavformat.so
jni/arm64-v8a/libavutil.so
jni/arm64-v8a/libswscale.so
jni/arm64-v8a/libswresample.so
```

If the AAR contains no `.so` files, do NOT consider the task complete. Investigate the native build and Android packaging chain.

---

# 11. Previous Failure

A previous JitPack build successfully produced:

```text
com.github.marlboro-advance:mpv-android:v0.0.1
```

but the AAR contained no native `.so` files.

The JitPack log contained:

```text
:app:mergeReleaseNativeLibs NO-SOURCE
```

The cause was that JitPack effectively ran the Gradle build without running the project's native dependency build first.

Do not repeat that configuration.

---

# 12. Previous JitPack Details

Previous JitPack environment:

```text
JDK 17.0.12
Gradle 8.14.3
```

Project versions:

```text
Android Gradle Plugin 8.13.2
Gradle 8.14.3
Kotlin 2.0.21
Java 17
```

Root `build.gradle` contains:

```gradle
buildscript {
    ext.kotlin_version = '2.0.21'

    repositories {
        mavenCentral()
        gradlePluginPortal()
        google()
    }

    dependencies {
        classpath 'com.android.tools.build:gradle:8.13.2'
        classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version"
    }
}

allprojects {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        google()
    }
}

tasks {
    wrapper {
        gradleVersion = "8.14.3"
        distributionType = Wrapper.DistributionType.BIN
    }
}

include ':app'
```

Do not unnecessarily modify the root Gradle file.

---

# 13. Do Not Do These Things

Do NOT:

- Replace the existing native build system.
- Rewrite `Android.mk`.
- Remove `ci.sh`.
- Remove `buildall.sh`.
- Replace FFmpeg/libmpv compilation with a random prebuilt binary.
- Add a random Gradle native build system.
- Modify application source code.
- Modify JNI C/C++ source code.
- Remove existing ABI support.
- Remove existing SDK/NDK version pins.
- Commit generated `.so` files.
- Commit a generated AAR.
- Commit the `build/` directory.
- Perform unrelated refactoring.

This task is build/release infrastructure only.

---

# 14. First Release Test

After making changes:

```bash
git add app/build.gradle jitpack.yml
git commit -m "Configure JitPack Maven publishing"
git push
```

Create a new test tag:

```bash
git tag v0.0.2
git push origin v0.0.2
```

JitPack should build:

```text
com.github.marlboro-advance:mpv-android:v0.0.2
```

---

# 15. JitPack Build Must Show Native Compilation

The JitPack log should show evidence of:

```text
ci.sh install
```

then:

```text
Fetching SDK + NDK
```

and:

```text
Fetching deps
```

or an existing prefix cache being restored.

Then:

```text
Building mpv
Building mpv-android
```

Then:

```text
ndk-build
```

Then:

```text
assembleRelease
```

A build that only executes Gradle publication without the native build is NOT sufficient.

---

# 16. Maven Artifact Requirements

Final Maven coordinates:

```text
groupId:
com.github.marlboro-advance

artifactId:
mpv-android

version:
the Git tag used by JitPack
```

Example:

```text
com.github.marlboro-advance:mpv-android:v0.0.2
```

The artifact should contain:

```text
mpv-android-v0.0.2.aar
mpv-android-v0.0.2-sources.jar
```

The AAR must contain the native `.so` libraries.

---

# 17. Consumer Test

Test from another Android project:

```gradle
repositories {
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation("com.github.marlboro-advance:mpv-android:v0.0.2")
}
```

The consumer must resolve the AAR and obtain the native libraries automatically.

---

# 18. Success Criteria

The task is complete ONLY when all of these are true:

1. JitPack successfully builds the repository.
2. Native dependencies are built or restored by the project's existing CI mechanism.
3. `libmpv.so` is present.
4. `libplayer.so` is present.
5. The AAR contains native `.so` files under the appropriate ABI directories.
6. JitPack publishes:
   `com.github.marlboro-advance:mpv-android:<tag>`
7. A separate Android project can consume the dependency normally.
8. No generated native binaries or AARs are committed to Git.
9. Existing source/native build scripts remain intact.

---

# 19. Working Style

Before editing:

1. Inspect the existing files.
2. Check whether a publishing configuration already exists elsewhere.
3. Check the available Gradle tasks.
4. Check the existing CI workflow if present.
5. Make the minimum required changes.

After editing:

1. Show exactly which files were changed.
2. Show the final relevant sections.
3. Explain any deviation from this document.
4. Do not make unrelated refactors.

If something fails, diagnose the actual error instead of replacing the build system.

Priority:

```text
Existing build system
        ↓
Native libraries
        ↓
AAR
        ↓
Maven/JitPack
```

Do not create a second native build system just for JitPack.
