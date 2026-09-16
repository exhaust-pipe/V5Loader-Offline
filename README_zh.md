# V5 本地加载器

[English](README.md) | 简体中文

这是一个以离线和隐私为目标的 V5Loader 分支。它移除了 V5 账号／认证、官方后端通信、遥测、自动模块与更新下载、Discord RPC、代理支持等官方联网功能。运行时模块仅从本地加载。配套 V5-Offline 脚本为公开物品／Bazaar 数据保留了 Hypixel 公共 API 路径。

本 Loader 应与 [V5-Offline](https://github.com/exhaust-pipe/V5-Offline) 配套使用。脚本需要手动安装，Loader 不会替你下载或更新。

## 运行时隐私模型

- 不需要 V5 账号，也不进行官方认证。
- 不发送遥测或向 V5 后端上报数据。
- 不自动下载模块、脚本、辅助程序或更新。
- Offline 兼容层会阻止远程图片加载。
- 脚本输出仅保存在本地 `logs/latest.log`，不使用上游可执行 JS 的 socket Console。
- Skija 渲染器所需的平台运行库在构建时打入 Mod，首次启动不会再从 Maven 下载 Skija。
- 配套 Offline 脚本使用的 Hypixel 公共数据访问会继续保留。

## 支持的 Minecraft 版本

| 组件 | 版本 |
| --- | --- |
| Minecraft | 26.1.2、26.2 |
| Fabric Loader | >= 0.19.3 |
| Fabric Language Kotlin | >= 1.13.9+kotlin.2.3.10 |

Fabric API 版本由 Stonecutter 根据 Minecraft 版本自动选择。

## 安装

1. 安装对应 Minecraft 版本的 `V5-Offline-<version>-<minecraft>.jar`、Fabric API 和 Fabric Language Kotlin。
2. 手动下载匹配的 [V5-Offline](https://github.com/exhaust-pipe/V5-Offline/releases) 脚本包。
3. 解压为 `config/ChatTriggers/modules/V5/`。
4. 不要同时安装其他版本的 V5 Loader 或 ChatTriggers。

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
