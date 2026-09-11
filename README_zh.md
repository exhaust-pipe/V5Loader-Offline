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
2. 将配套 V5-Offline 仓库的内容完整复制到 `config/ChatTriggers/modules/V5/`
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

准备 JDK 25、CMake 和 MinGW，并确保 `JAVA_HOME` 指向 JDK 25、相关工具已在当前终端的 `PATH` 中。以下 PowerShell 命令在本仓库目录执行：

```powershell
$env:GRADLE_USER_HOME = Join-Path (Split-Path $PWD -Parent) '.AI_work/gradle-home'
cmake -S NativeSrc -B ../.AI_work/native-build -G 'MinGW Makefiles' -DCMAKE_BUILD_TYPE=Release '-DCMAKE_SHARED_LINKER_FLAGS=-static-libgcc -static-libstdc++ -static'
cmake --build ../.AI_work/native-build --parallel
Copy-Item ../.AI_work/native-build/V5PathJNI.dll src/main/resources/assets/v5/natives/windows/x86_64/V5PathJNI.dll -Force
./gradlew.bat build --no-daemon
```

构建结果为 `build/libs/V5-Offline-26.1.2.jar`。首次构建需要下载依赖；依赖缓存完整后，可使用 `./gradlew.bat build --offline --no-daemon`。
