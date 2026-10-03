# V5 本地加载器

[English](README.md) | 简体中文

这是一个以离线和隐私为目标的 V5Loader 分支。它移除了所有V5官方的联网功能，使mod仅从本地加载运行。同时增加了一些新功能与修复。

该加载器需要加载配套的脚本 [V5-Offline](https://github.com/exhaust-pipe/V5-Offline) 才能正常使用。因为移除了联网功能，脚本需要你手动[安装](#安装)，Loader 不会替你下载或更新。

## 离线更新

- 不需要 V5 账号，也不进行官方认证。
- 不发送遥测或向 V5 后端上报数据。
- 不自动下载更新。
- 阻止远程图片加载。
- 脚本输出仅保存在本地 `logs/latest.log`，不使用原来可执行 JS 的 socket Console。
- gui渲染回退到旧版本的 NanoVG 进行，不下载新版本使用的 Skija

## 支持的 Minecraft 版本

| 组件 | 版本 |
| --- | --- |
| Minecraft | 26.1.2、26.2 |
| Fabric Loader | >= 0.19.3 |
| Fabric Language Kotlin | >= 1.13.9+kotlin.2.3.10 |

Fabric API 版本由 Stonecutter 根据 Minecraft 版本自动选择。

## 安装

1. 安装对应版本的mod和上述依赖
2. 将配套 [V5-Offline](https://github.com/exhaust-pipe/V5-Offline/releases) 发布的zip文件作为一个**文件夹**完整解压到 `config/ChatTriggers/modules/`，并确认该文件夹名称为V5，使结构变为：`config/ChatTriggers/modules/V5`
3. 确认目录结构如下，且没有同时安装其他版本的 V5 Loader 或 ChatTriggers：

目录结构应类似：

```text
游戏目录/
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

## 使用

- `/v5` 打开 V5 界面。
- `/ct load` 重新加载本地脚本；动态 mixin 变更仍需要重启游戏。
- 用户脚本放在 `config/ChatTriggers/modules/V5Config/UserScripts/`。
- 脚本日志写入 `logs/latest.log`；`/ct console` 会提示该日志位置。
- 配套脚本会把公开物品／Bazaar 数据缓存到 `config/ChatTriggers/modules/V5Config/public-data/`。

## 构建

需要 JDK 25、CMake 3.13+，以及对应平台的 C++ 工具链。

### JVM / Mod 构建

显式构建目标 Minecraft 版本：

```bash
./gradlew :26.1.2:build -PreleaseBuild
./gradlew :26.2:build -PreleaseBuild
```

输出位于 `versions/<minecraft>/build/libs/`，文件名类似：

```text
V5-Offline-5.2.0-offline-26.1.2.jar
V5-Offline-5.2.0-offline-26.2.jar
```

构建阶段会从 Maven／Gradle 仓库解析依赖，并取得 Windows、Linux、macOS 的 x86_64／ARM64 NanoVG 运行库。Android NanoVG 从同版本 LWJGL 源码编译。原生库均随 Mod 打包，游戏运行时无需下载。

### Native Pathfinder JNI

仓库不保存生成后的 Pathfinder JNI 二进制文件。完整 Mod 包含八个平台的寻路 JNI，以及两份 Android NanoVG 库：

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

Windows 使用静态 MSVC runtime，使 JNI DLL 不依赖目标机器额外安装 Visual C++ Redistributable。Android 使用静态 libc++，最低 API 24，并支持 16 KB 内存页。Android 启动器仍须提供 Java 25、匹配的 LWJGL 核心库和兼容 OpenGL 3 的 Minecraft 渲染环境；本项目不提供独立 Android 应用。

Windows ARM64 的 JNI builder 使用 Temurin 21 的头文件和导入库，因为该平台暂无 Temurin 25。Minecraft Mod 的编译和运行仍需要 Java 25。

本地构建 JNI 时可使用与 Actions builder 相同的 CMake 参数。Linux：

```bash
cmake -S NativeSrc -B NativeSrc/build -DCMAKE_BUILD_TYPE=Release
cmake --build NativeSrc/build --config Release --parallel
```

macOS 还需要指定目标架构，例如添加 `-DCMAKE_OSX_ARCHITECTURES=arm64 -DCMAKE_OSX_DEPLOYMENT_TARGET=11.0`；构建 x86_64 时将架构改为 `x86_64`。Linux ARM64 在对应架构主机上使用同一命令。Windows x64 + MSVC（ARM64 使用 `-A ARM64`）：

```powershell
cmake -S NativeSrc -B NativeSrc/build -A x64 -DCMAKE_BUILD_TYPE=Release -DCMAKE_MSVC_RUNTIME_LIBRARY=MultiThreaded -DCMAKE_POLICY_DEFAULT_CMP0091=NEW
cmake --build NativeSrc/build --config Release --parallel
```

Android 构建同时生成寻路 JNI 和 NanoVG。准备 NDK 27.2.12479018，并将 LWJGL 3.4.1（提交 `b800ccffab14396fc529ddb6c931b7c5c5226763`）源码放入 `../lwjgl3`；此版本必须与 `gradle/libs.versions.toml` 一致。以下构建 ARM64，x86_64 则将 ABI 改为 `x86_64`：

```bash
cmake -S NativeSrc -B NativeSrc/build-android \
  -DCMAKE_TOOLCHAIN_FILE="$ANDROID_NDK_HOME/build/cmake/android.toolchain.cmake" \
  -DANDROID_ABI=arm64-v8a -DANDROID_PLATFORM=android-24 \
  -DANDROID_STL=c++_static -DANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON \
  -DV5_LWJGL_SOURCE_DIR="$PWD/../lwjgl3" -DCMAKE_BUILD_TYPE=Release
cmake --build NativeSrc/build-android --config Release --parallel
```

构建完成后，将 `V5PathJNI` 库复制到上方对应目录；Android 还需要复制 `nanovg/liblwjgl_nanovg.so`，再运行 Gradle。仅在本机验证时只需当前平台的库；完整跨平台 JAR 需要上面全部文件。新增平台的实际启动、GUI 和寻路仍需对应设备验证。

### GitHub Actions

- **Build**：优先按源码哈希恢复 JNI bundle；缓存未命中时才构建 Linux、macOS、Windows、Android 原生库，然后统一构建 Minecraft 26.1.2 和 26.2 并上传最终 JAR artifacts。
- **Native builders**：Linux、macOS、Windows、Android 的 reusable workflow，由 Build 和 Release 调用；生成的 JNI 文件不会提交回仓库。
- **Release**：仅在 push tag 时运行，始终从源码重编全部 JNI，再使用 tag 作为 Mod 版本构建两个 Minecraft 版本，并发布到对应 GitHub Release。

## 原项目

- [V5Loader](https://github.com/V5-Client/V5Loader)
- [V5 scripts](https://github.com/V5-Client/V5)
- [原项目文档](https://rdbt.top/docs/getting-started)

保留原项目版权与 GPL-3.0 许可证。第三方许可证见 [THIRD_PARTY_LICENSES.md](THIRD_PARTY_LICENSES.md)。
