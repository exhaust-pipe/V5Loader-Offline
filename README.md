# V5 Local Loader

English | [简体中文](README_zh.md)

An offline-focused fork of V5Loader. It removes V5 account/authentication, vendor backend communication, telemetry, automatic module/update downloads, Discord RPC, proxy support, and other vendor-network features. Runtime module loading is local-only. The retained public Hypixel API path is used only by the matching V5-Offline scripts for public market/item data.

This loader is intended to be used with [V5-Offline](https://github.com/exhaust-pipe/V5-Offline). Install the scripts manually; the loader does not download or update them for you.

## Runtime privacy model

- No V5 account or vendor authentication.
- No telemetry or vendor data reporting.
- No automatic module, script, helper, or update downloads.
- Remote image loading is blocked by the Offline compatibility layer.
- Script output stays local in `logs/latest.log`; the upstream socket/eval console is not used.
- The Skija renderer is packaged with its supported platform runtimes at build time, so first launch does not need to download Skija from Maven.
- Public Hypixel data access used by the Offline scripts is intentionally retained.

## Supported Minecraft versions

| Component | Version |
| --- | --- |
| Minecraft | 26.1.2 and 26.2 |
| Fabric Loader | >= 0.19.3 |
| Fabric Language Kotlin | >= 1.13.9+kotlin.2.3.10 |

Fabric API is selected per Minecraft version through Stonecutter.

## Installation

1. Install the matching `V5-Offline-<version>-<minecraft>.jar`, Fabric API, and Fabric Language Kotlin.
2. Download the matching [V5-Offline](https://github.com/exhaust-pipe/V5-Offline/releases) script package manually.
3. Extract it as `config/ChatTriggers/modules/V5/`.
4. Do not install another V5 Loader or ChatTriggers build at the same time.

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

The build resolves dependencies from Maven/Gradle repositories, including the supported Skija platform runtimes. Those runtimes are then included in the produced mod so they are not downloaded at game startup.

### Native Pathfinder JNI

The repository tracks four native Pathfinder binaries:

- Windows x86_64
- Linux x86_64
- macOS arm64
- macOS x86_64

Windows uses the static MSVC runtime so the JNI DLL does not require a separately installed Visual C++ Redistributable.

Changing `NativeSrc/**` triggers the **Rebuild JNI** workflow. It builds all platforms, collects the four binaries, and commits updated files under `src/main/resources/assets/v5/natives/` back to the source branch when they changed.

### GitHub Actions

- **Build**: builds both Minecraft 26.1.2 and 26.2 and uploads artifacts.
- **Rebuild JNI**: runs automatically for `NativeSrc/**` changes and can also be started manually.
- **Release**: runs only for tag pushes, builds both Minecraft versions, and publishes both JARs to the GitHub Release named by the tag.

## Original project

- [V5Loader](https://github.com/V5-Client/V5Loader)
- [V5 scripts](https://github.com/V5-Client/V5)
- [Original documentation](https://rdbt.top/docs/getting-started)

The original project's copyright and GPL-3.0 license are retained. Third-party licenses are listed in [THIRD_PARTY_LICENSES.md](THIRD_PARTY_LICENSES.md).
