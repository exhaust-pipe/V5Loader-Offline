# V5 Local Loader

English | [简体中文](README_zh.md)

An offline version of V5Loader with all third-party networking removed. It requires no account, does not download, update, or replace mods, scripts, or helper programs, and does not upload any data.
This mod requires the similarly modified [V5-Offline](https://github.com/exhaust-pipe/V5-Offline) scripts to work. Since automatic downloads have been removed, you need to download that repository's contents manually. See [Installation](#installation).

## Original Project Links

- [Original project documentation](https://rdbt.top/docs/getting-started)
- [Original V5Loader repository](https://github.com/V5-Client/V5Loader)
- [Original V5 script repository](https://github.com/V5-Client/V5)

The original project's copyright and [GPL-3.0 license](LICENSE) are retained. See [THIRD_PARTY_LICENSES.md](THIRD_PARTY_LICENSES.md) for third-party licenses.

## Requirements

| Component              | Version                 |
| ---------------------- | ----------------------- |
| Minecraft              | 26.1.2                  |
| Fabric Loader          | >= 0.19.3               |
| Fabric API             | >= 0.153.0+26.1.2       |
| Fabric Language Kotlin | >= 1.13.9+kotlin.2.3.10 |

## Installation

1. Install the mod and the dependencies listed above.
2. Extract the zip file released with [V5-Offline](https://github.com/exhaust-pipe/V5-Offline/releases) as a **folder** directly into `config/ChatTriggers/modules/`. Ensure the folder is named `V5`, resulting in the following structure: `config/ChatTriggers/modules/V5`.
3. Confirm that the directory structure matches the following and that no other version of V5 Loader or ChatTriggers is installed:

```text
Game directory/
├─ mods/
│  ├─ V5-Offline
│  ├─ fabric-api
│  └─ fabric-language-kotlin
└─ config/ChatTriggers/modules/V5/
   ├─ metadata.json
   ├─ loader.js
   ├─ utils/
   └─ assets/
```

## Usage and Updates

- Use `/v5` to open the interface, or assign a key binding manually.
- To update scripts, manually replace the files in `config/ChatTriggers/modules/V5/`, then run `/ct load`. Changes involving dynamic mixins require a game restart.
- Place custom scripts in `config/ChatTriggers/modules/V5Config/UserScripts/`.
- Logs are stored in `logs/latest.log`. `/ct console` displays the log location.

Item and Bazaar market data come from the official API. The cache is stored in `config/ChatTriggers/modules/V5Config/public-data/`.

## Building from Source

The current target is Minecraft **26.1.2**. The JAR bundles native libraries for Windows x86_64, Linux x86_64, and macOS arm64 and x86_64. Minecraft 26.2 source compatibility will be handled separately.

Prepare JDK 25, CMake 3.15 or newer, and a C++ compiler for your platform. Set `JAVA_HOME` to JDK 25 and make the tools available in the current terminal's `PATH`. Run all commands from this repository's root. Intermediate files go under `build/`, and the Gradle cache goes under `.gradle/user-home/`; no external helper scripts or pre-existing caches are required.

### 1. Build native libraries

Run the commands for your operating system. On Windows, install Visual Studio C++ Build Tools and the Windows SDK, then use an x64 developer PowerShell. These commands use MSVC with the static C/C++ runtime:

```powershell
cmake -S NativeSrc -B build/native-windows -A x64 -DCMAKE_BUILD_TYPE=Release -DCMAKE_MSVC_RUNTIME_LIBRARY=MultiThreaded -DCMAKE_POLICY_DEFAULT_CMP0091=NEW
cmake --build build/native-windows --config Release --parallel
New-Item -ItemType Directory -Force src/main/resources/assets/v5/natives/windows/x86_64 | Out-Null
Copy-Item build/native-windows/Release/V5PathJNI.dll src/main/resources/assets/v5/natives/windows/x86_64/V5PathJNI.dll -Force
```

On Linux x86_64, use GCC/G++ and Make (or the corresponding CMake build tools):

```bash
cmake -S NativeSrc -B build/native-linux -DCMAKE_BUILD_TYPE=Release
cmake --build build/native-linux --config Release --parallel
mkdir -p src/main/resources/assets/v5/natives/linux/x86_64
cp build/native-linux/V5PathJNI.so src/main/resources/assets/v5/natives/linux/x86_64/V5PathJNI.so
```

On macOS, use Xcode Command Line Tools. Build both Apple Silicon and Intel libraries:

```bash
for arch in arm64 x86_64; do
  cmake -S NativeSrc -B "build/native-macos-$arch" \
    -DCMAKE_BUILD_TYPE=Release \
    -DCMAKE_OSX_ARCHITECTURES="$arch" \
    -DCMAKE_OSX_DEPLOYMENT_TARGET=11.0
  cmake --build "build/native-macos-$arch" --config Release --parallel
  mkdir -p "src/main/resources/assets/v5/natives/macos/$arch"
  cp "build/native-macos-$arch/V5PathJNI.dylib" "src/main/resources/assets/v5/natives/macos/$arch/V5PathJNI.dylib"
done
```

A local build replaces only the platform libraries produced by those commands; the other platforms retain the libraries already tracked in the repository. After changing `NativeSrc/`, build on each platform and collect all four files in the matching directories under `src/main/resources/assets/v5/natives/` before packaging. Alternatively, use GitHub Actions below to build and commit all platform libraries automatically.

### 2. Build the JAR

```powershell
# Windows / PowerShell
./gradlew.bat --gradle-user-home .gradle/user-home build --no-daemon -PreleaseBuild
```

```bash
# Linux / macOS
bash ./gradlew --gradle-user-home .gradle/user-home build --no-daemon -PreleaseBuild
```

The output is `build/libs/V5-Offline-26.1.2.jar`, containing all four platform/architecture libraries. The first build downloads Gradle, Minecraft, and Maven dependencies. Once the same cache is complete, append `--offline` to the command.

### 3. GitHub Actions

Enable Actions in your fork and select **Compile JNI and JVM → Run workflow** to rebuild all native platforms, commit the libraries back to the selected branch, and build the JAR. Pushing build-related files to any branch also triggers the workflow; JVM-only or dependency changes reuse committed native libraries.

Download the JAR from the run's Artifacts. Successful builds on the `26.1.2` branch also create or update a GitHub Release in the current fork, using `mod_version` from `gradle.properties` plus an incrementing `-rN` suffix. No upstream service Secrets are needed. Native commits and releases require `contents: write`, and branch rules must allow Actions to push. See [.github/README.md](.github/README.md).
