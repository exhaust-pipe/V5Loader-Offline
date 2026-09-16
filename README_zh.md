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

构建阶段会从 Maven／Gradle 仓库解析依赖，并取得受支持平台的 Skija 运行库；这些运行库随后会随 Mod 一起打包，因此游戏运行时无需再下载。

### Native Pathfinder JNI

仓库维护四套 Pathfinder 原生库：

- Windows x86_64
- Linux x86_64
- macOS arm64
- macOS x86_64

Windows 使用静态 MSVC runtime，使 JNI DLL 不依赖目标机器额外安装 Visual C++ Redistributable。

修改 `NativeSrc/**` 会自动触发 **Rebuild JNI** workflow：三平台构建完成后收集四个原生库，并在内容发生变化时回写到当前源码分支的 `src/main/resources/assets/v5/natives/`。

### GitHub Actions

- **Build**：同时构建 Minecraft 26.1.2 和 26.2，并上传 artifacts。
- **Rebuild JNI**：`NativeSrc/**` 发生变化时自动运行，也支持手动触发。
- **Release**：仅在 push tag 时运行，构建两个 Minecraft 版本，并把两个 JAR 发布到以该 tag 命名的 GitHub Release。

## 原项目

- [V5Loader](https://github.com/V5-Client/V5Loader)
- [V5 scripts](https://github.com/V5-Client/V5)
- [原项目文档](https://rdbt.top/docs/getting-started)

保留原项目版权与 GPL-3.0 许可证。第三方许可证见 [THIRD_PARTY_LICENSES.md](THIRD_PARTY_LICENSES.md)。
