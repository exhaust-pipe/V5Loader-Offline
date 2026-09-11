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
2. Copy the entire contents of the companion V5-Offline repository to `config/ChatTriggers/modules/V5/`.
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

Prepare JDK 25, CMake, and MinGW. Make sure `JAVA_HOME` points to JDK 25 and the required tools are available in the current terminal's `PATH`. Run the following PowerShell commands from this repository's directory:

```powershell
$env:GRADLE_USER_HOME = Join-Path (Split-Path $PWD -Parent) '.AI_work/gradle-home'
cmake -S NativeSrc -B ../.AI_work/native-build -G 'MinGW Makefiles' -DCMAKE_BUILD_TYPE=Release '-DCMAKE_SHARED_LINKER_FLAGS=-static-libgcc -static-libstdc++ -static'
cmake --build ../.AI_work/native-build --parallel
Copy-Item ../.AI_work/native-build/V5PathJNI.dll src/main/resources/assets/v5/natives/windows/x86_64/V5PathJNI.dll -Force
./gradlew.bat build --no-daemon
```

The build output is `build/libs/V5-Offline-26.1.2.jar`. The first build requires downloading dependencies. Once the dependency cache is complete, you can use `./gradlew.bat build --offline --no-daemon`.
