# V5 本地加载器

[English](README.md) | 简体中文

V5Loader的离线版本，删除了所有第三方联网内容，不需要账号，不下载、更新或替换 Mod、脚本和辅助程序，不上传任何数据。
该mod需要加载同样修改过的 [V5-Offline](https://github.com/exhaust-pipe/V5-Offline) 才能使用，由于移除了自动下载功能，你需要手动下载该仓库的内容，详见 [安装](#安装)

## 原项目链接

- [原项目文档](https://rdbt.top/docs/getting-started)
- [V5Loader 原仓库](https://github.com/V5-Client/V5Loader)
- [V5 脚本原仓库](https://github.com/V5-Client/V5)

保留原项目版权与 [GPL-3.0 许可证](LICENSE)。第三方许可见 [THIRD_PARTY_LICENSES.md](THIRD_PARTY_LICENSES.md)。

## 运行环境

| 组件                   | 版本                    |
| ---------------------- | ----------------------- |
| Minecraft              | 26.1.2                  |
| Fabric Loader          | >= 0.19.3               |
| Fabric API             | >= 0.153.0+26.1.2       |
| Fabric Language Kotlin | >= 1.13.9+kotlin.2.3.10 |

## 安装

1. 安装mod和上述依赖
2. 将配套 [V5-Offline](https://github.com/exhaust-pipe/V5-Offline/releases) 发布的zip文件作为一个**文件夹**完整解压到 `config/ChatTriggers/modules/`，并确认该文件夹名称为V5，使结构变为：`config/ChatTriggers/modules/V5`
3. 确认目录结构如下，且没有同时安装其他版本的 V5 Loader 或 ChatTriggers：

```text
游戏目录/
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

## 使用与更新

- `/v5` 打开界面，或者手动设置快捷键。
- 更新脚本时手动替换 `config/ChatTriggers/modules/V5/` 中的文件，再执行 `/ct load`。涉及动态 mixin 的修改需要重启游戏。
- 自定义脚本放入 `config/ChatTriggers/modules/V5Config/UserScripts/`。
- 日志位于 `logs/latest.log`，`/ct console` 会提示日志位置。

物品和 Bazaar 行情来自 官方 api，缓存保存在 `config/ChatTriggers/modules/V5Config/public-data/`

## 从源码构建

当前构建目标为 Minecraft **26.1.2**，JAR 包含 Windows x86_64、Linux x86_64、macOS arm64 和 x86_64 的原生库。26.2 的源码适配另行处理。

准备 JDK 25、CMake 3.15 或更新版本，以及对应平台的 C++ 编译器；将 `JAVA_HOME` 指向 JDK 25，并确保工具位于当前终端的 `PATH` 中。以下命令均在本仓库根目录执行，构建中间文件位于 `build/`，Gradle 缓存位于 `.gradle/user-home/`，不需要仓库外的辅助脚本或缓存。

### 1. 构建原生库

按当前操作系统执行相应命令。Windows 需要 Visual Studio C++ Build Tools 和 Windows SDK，在 x64 开发者 PowerShell 中运行；以下使用 MSVC，静态链接 C/C++ runtime：

```powershell
cmake -S NativeSrc -B build/native-windows -A x64 -DCMAKE_BUILD_TYPE=Release -DCMAKE_MSVC_RUNTIME_LIBRARY=MultiThreaded -DCMAKE_POLICY_DEFAULT_CMP0091=NEW
cmake --build build/native-windows --config Release --parallel
New-Item -ItemType Directory -Force src/main/resources/assets/v5/natives/windows/x86_64 | Out-Null
Copy-Item build/native-windows/Release/V5PathJNI.dll src/main/resources/assets/v5/natives/windows/x86_64/V5PathJNI.dll -Force
```

Linux x86_64 需要 GCC/G++ 和 Make（或对应的 CMake 构建工具）：

```bash
cmake -S NativeSrc -B build/native-linux -DCMAKE_BUILD_TYPE=Release
cmake --build build/native-linux --config Release --parallel
mkdir -p src/main/resources/assets/v5/natives/linux/x86_64
cp build/native-linux/V5PathJNI.so src/main/resources/assets/v5/natives/linux/x86_64/V5PathJNI.so
```

macOS 需要 Xcode Command Line Tools。以下分别生成 Apple Silicon 和 Intel 两种架构的库：

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

一次本地构建只会替换上述命令生成的平台库，其他平台使用仓库已有的原生库。若修改了 `NativeSrc/`，应在各平台分别构建，将四个文件汇总到 `src/main/resources/assets/v5/natives/` 的对应目录后再打包。也可以运行下述 GitHub Actions 完成三平台构建和自动回写。

### 2. 构建 JAR

```powershell
# Windows / PowerShell
./gradlew.bat --gradle-user-home .gradle/user-home build --no-daemon -PreleaseBuild
```

```bash
# Linux / macOS
bash ./gradlew --gradle-user-home .gradle/user-home build --no-daemon -PreleaseBuild
```

结果为 `build/libs/V5-Offline-26.1.2.jar`，包含上述四种平台／架构的原生库。首次构建需要联网下载 Gradle、Minecraft 和 Maven 依赖；同一缓存完整后，可在命令末尾追加 `--offline`。

### 3. GitHub Actions

在自己的 fork 启用 Actions，选择 **Compile JNI and JVM → Run workflow**，即可重建所有平台原生库、自动提交回所选分支，再构建 JAR。向任意分支推送构建相关文件也会触发；仅 JVM 源码或依赖变更时复用已提交的原生库。

从该次运行的 Artifacts 下载 JAR。`26.1.2` 分支构建成功后还会自动在当前 fork 创建／更新 GitHub Release，使用 `gradle.properties` 的 `mod_version` 加递增的 `-rN` 标签。无需原项目的 Secrets；原生库回写和 Release 发布需要 `contents: write`，分支规则必须允许 Actions 推送。详见 [.github/README.md](.github/README.md)。
