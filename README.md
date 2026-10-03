# V5 Local Loader

English | [简体中文](README_zh.md)

An offline-focused fork of V5Loader. It removes all official V5 online features, allowing the mod to run exclusively from local sources. Additionally, it adds some new features and bug fixes.

This loader is intended to be used with [V5-Offline](https://github.com/exhaust-pipe/V5-Offline). Since the online features has been removed, you need to [install the scripts manually](#Installation); the loader does not download or update them for you.

## offline update

- No V5 account or vendor authentication.
- No telemetry or vendor data reporting.
- No automatic update downloads.
- Remote image loading is blocked.
- Script output stays local in `logs/latest.log`; the upstream socket/eval console is not used.
- GUI rendering has fallen back to the NanoVG, skipping the download of the newer Skija-based version.

## Supported Minecraft versions

| Component | Version |
| --- | --- |
| Minecraft | 26.1.2 and 26.2 |
| Fabric Loader | >= 0.19.3 |
| Fabric Language Kotlin | >= 1.13.9+kotlin.2.3.10 |

Fabric API is selected per Minecraft version through Stonecutter.

## Installation

1. Install the mod and the dependencies listed above.
2. Extract the zip file released with [V5-Offline](https://github.com/exhaust-pipe/V5-Offline/releases) as a **folder** directly into `config/ChatTriggers/modules/`. Ensure the folder is named `V5`, resulting in the following structure: `config/ChatTriggers/modules/V5`.
3. Confirm that the directory structure matches the following and that no other version of V5 Loader or ChatTriggers is installed:

Expected layout:

```text
Game directory/
├─ mods/
│  ├─ V5-Offline-<version>-<minecraft>.jar
│  ├─ fabric-api
│  └─ fabric-language-kotlin
└─ config/ChatTriggers/modules/V5/
   ├─ metadata.json
   ├─ loader.js
   ├─ utils/
   └─ assets/
```

## Usage

- `/v5` opens the V5 interface.
- `/ct load` reloads local scripts. Dynamic mixin changes still require a game restart.
- User scripts belong in `config/ChatTriggers/modules/V5Config/UserScripts/`.
- Script logs are written to `logs/latest.log`; `/ct console` points to that file.
- Public item/Bazaar data caches are stored under `config/ChatTriggers/modules/V5Config/public-data/` by the matching scripts.

## Building

Requirements: JDK 25, CMake 3.13+, and a C++ toolchain for the target native platform.

### JVM / mod builds

Build one Minecraft version explicitly:

```bash
./gradlew :26.1.2:build -PreleaseBuild
./gradlew :26.2:build -PreleaseBuild
```

Outputs are written below `versions/<minecraft>/build/libs/` and are named like:

```text
V5-Offline-5.2.0-offline-26.1.2.jar
V5-Offline-5.2.0-offline-26.2.jar
```

The build resolves dependencies from Maven/Gradle repositories, including NanoVG runtimes for Windows, Linux, and macOS on x86_64 and ARM64. Android NanoVG is compiled from the matching LWJGL sources. Native libraries are bundled in the mod; none are downloaded at game startup.

### Native Pathfinder JNI

Generated Pathfinder JNI binaries are not stored in Git. A complete mod contains eight Pathfinder JNI targets and two Android NanoVG libraries:

```text
src/main/resources/assets/v5/natives/
├─ android/arm64/V5PathJNI.so
├─ android/arm64/liblwjgl_nanovg.so
├─ android/x86_64/V5PathJNI.so
├─ android/x86_64/liblwjgl_nanovg.so
├─ linux/arm64/V5PathJNI.so
├─ linux/x86_64/V5PathJNI.so
├─ macos/arm64/V5PathJNI.dylib
├─ macos/x86_64/V5PathJNI.dylib
├─ windows/arm64/V5PathJNI.dll
└─ windows/x86_64/V5PathJNI.dll
```

Windows uses the static MSVC runtime so the JNI DLL does not require a separately installed Visual C++ Redistributable. Android uses static libc++, targets API 24 or newer, and supports 16 KB memory pages. An Android launcher must still provide Java 25, compatible LWJGL core libraries, and an OpenGL 3 compatible Minecraft rendering environment; this project is not a standalone Android application.

The Windows ARM64 JNI builder uses Temurin 21 headers and import libraries because Temurin 25 is unavailable for that platform. Compiling and running the Minecraft mod still requires Java 25.

For local JNI builds, use the same CMake configuration as the Actions builders. On Linux:

```bash
cmake -S NativeSrc -B NativeSrc/build -DCMAKE_BUILD_TYPE=Release
cmake --build NativeSrc/build --config Release --parallel
```

On macOS, also set the target architecture, for example `-DCMAKE_OSX_ARCHITECTURES=arm64 -DCMAKE_OSX_DEPLOYMENT_TARGET=11.0`; use `x86_64` instead when building that target. Linux ARM64 uses the same command on an ARM64 host. On Windows x64 with MSVC (use `-A ARM64` for ARM64):

```powershell
cmake -S NativeSrc -B NativeSrc/build -A x64 -DCMAKE_BUILD_TYPE=Release -DCMAKE_MSVC_RUNTIME_LIBRARY=MultiThreaded -DCMAKE_POLICY_DEFAULT_CMP0091=NEW
cmake --build NativeSrc/build --config Release --parallel
```

Android builds produce both Pathfinder JNI and NanoVG. Install NDK 27.2.12479018 and check out LWJGL 3.4.1 (commit `b800ccffab14396fc529ddb6c931b7c5c5226763`) into `../lwjgl3`; its version must match `gradle/libs.versions.toml`. Build ARM64 as follows, or change the ABI to `x86_64`:

```bash
cmake -S NativeSrc -B NativeSrc/build-android \
  -DCMAKE_TOOLCHAIN_FILE="$ANDROID_NDK_HOME/build/cmake/android.toolchain.cmake" \
  -DANDROID_ABI=arm64-v8a -DANDROID_PLATFORM=android-24 \
  -DANDROID_STL=c++_static -DANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON \
  -DV5_LWJGL_SOURCE_DIR="$PWD/../lwjgl3" -DCMAKE_BUILD_TYPE=Release
cmake --build NativeSrc/build-android --config Release --parallel
```

Copy the generated `V5PathJNI` library into its matching path above before running Gradle. Android also requires `nanovg/liblwjgl_nanovg.so`. Local validation only needs the current platform libraries; a complete cross-platform JAR requires every file above. Startup, GUI rendering, and pathfinding on the added platforms still require validation on their respective devices.

### GitHub Actions

- **Build**: restores the JNI bundle by source hash when possible; on a cache miss it builds the required Linux, macOS, Windows, and Android native targets first, then builds Minecraft 26.1.2 and 26.2 and uploads the final JAR artifacts.
- **Native builders**: reusable Linux, macOS, Windows, and Android workflows used by Build and Release. Generated JNI files are never committed back to the repository.
- **Release**: runs only for tag pushes, always rebuilds all JNI targets from source, builds both Minecraft versions using the tag as the mod version, and publishes both JARs to the matching GitHub Release.

## Original project

- [V5Loader](https://github.com/V5-Client/V5Loader)
- [V5 scripts](https://github.com/V5-Client/V5)
- [Original documentation](https://rdbt.top/docs/getting-started)

The original project's copyright and GPL-3.0 license are retained. Third-party licenses are listed in [THIRD_PARTY_LICENSES.md](THIRD_PARTY_LICENSES.md).
