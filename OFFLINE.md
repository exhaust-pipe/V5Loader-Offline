# V5 Offline — Minecraft Java 26.1.2

此构建使用本地手动安装的脚本，不需要 V5 或 Discord 账号，不下载、更新或替换 Mod、脚本和辅助程序。

## 安装

本次交付的 `V5-Offline-26.1.2.jar` 面向 **Windows x86_64 / Java 25**，包含从本仓库 C++ 源码重新编译的寻路 DLL。Linux/macOS 预编译寻路库不包含在此构建中。

1. 准备 Minecraft **26.1.2** 的 Fabric 实例，Fabric Loader **0.19.3**。
2. 在该实例的 `mods` 目录放入 `V5-Offline-26.1.2.jar`，以及 Fabric API **0.153.0+26.1.2**、Fabric Language Kotlin **1.13.9+kotlin.2.3.10**。Hypixel Mod API 已包含在 Mod 中。
3. 将修改后的 **V5script 仓库内容**复制到该实例的 `config/ChatTriggers/modules/V5/`，目录必须包含 `V5/metadata.json`、`V5/loader.js`、`V5/utils/` 和 `V5/assets/`。也可解压交付的脚本 ZIP，其中已包含顶层 `V5/` 文件夹。
4. 使用干净的模块目录，避免把旧版 V5、requestV2、WebSocket 或其他未经检查的模块混入。请勿同时加载旧 V5 Loader 或 ChatTriggers。
5. 启动游戏，进入世界后执行 `/v5` 打开界面。脚本更新需手动替换本地文件，再执行 `/ct load`；含动态 mixin 的脚本需重启游戏。

实例的工作目录应为其游戏目录。日志在 `logs/latest.log`，`/ct console` 会提示此位置。可手动将扩展脚本放入 `config/ChatTriggers/modules/V5Config/UserScripts/`。

## 数据与联网范围

保留的 Mod HTTP 流量只有以下两个固定地址的 GET：

- `https://api.hypixel.net/v2/resources/skyblock/items`
- `https://api.hypixel.net/v2/skyblock/bazaar`

请求不包含请求体、查询参数、用户名、UUID、账号令牌、API Key、Cookie 或自定义调用方请求头。HTTP 客户端不继承全局 Cookie、认证器或代理，禁止重定向，设有超时和 16 MiB 响应上限。允许的固定请求头为 `Accept: application/json` 和 `User-Agent: V5-Offline`，协议还会生成 Host 等必要头。

同时保留 Hypixel 官方 Mod API 的位置事件订阅，使用正常游戏连接发送事件订阅标识、接收服务器位置数据。公开行情在后台获取，查询间隔至少五分钟，缓存写入 `V5Config/public-data/`。断网时使用已有缓存；第一次离线启动若没有缓存，行情与物品目录不可用，其余本地脚本继续加载。缓存价格可能过期。

移除的内容：V5 后端认证、JWT/刷新令牌存储、自动更新/迁移、脚本下载、WebSocket/IRC/远程命令、封禁/错误/配置/Mod 列表上报、Discord RPC/Webhook/截图上传、音乐助手下载及执行、代理转发、远程图片、额外脚本 JAR 自动加载、Mojang 账号会话读取与档案请求。玩家名称和 UUID API 仅取当前世界中的玩家实体，不读取启动器账号会话。原控制台的本地 socket 与外部进程已删除，日志留在本地。

这里的离线指 **V5 自身的加载、认证和运行资源离线化**。Minecraft 本体的账号登录、Realms、皮肤、多人游戏连接仍由原版管理；正常游戏通信没有被禁用。Hypixel 请求仍必然暴露网络源 IP，不能把网络连接描述为完全匿名。

## 本地代码边界

交付脚本已移除上述代码及依赖。加载器限制脚本导入为本地模块目录，但 ChatTriggers 仍是具有 Java 互操作能力的脚本引擎，并不是任意恶意脚本的安全沙箱。后来手工加入或修改的脚本、其他 Mod、被替换的依赖不在本次检查范围内。

## 构建

使用已有 JDK 25、CMake 和 C++ 编译器。项目 Gradle Wrapper 为 9.6.0；首次构建需要联网下载公开构建依赖，成品不会自行下载这些依赖。

Windows / MinGW 示例，命令从本仓库目录执行：

```powershell
$env:GRADLE_USER_HOME = Join-Path (Split-Path $PWD -Parent) '.AI_work/gradle-home'
cmake -S NativeSrc -B ../.AI_work/native-build -G 'MinGW Makefiles' -DCMAKE_BUILD_TYPE=Release '-DCMAKE_SHARED_LINKER_FLAGS=-static-libgcc -static-libstdc++ -static'
cmake --build ../.AI_work/native-build --parallel
Copy-Item ../.AI_work/native-build/V5PathJNI.dll src/main/resources/assets/v5/natives/windows/x86_64/V5PathJNI.dll -Force
./gradlew.bat build --no-daemon
```

输出在 `build/libs/V5-Offline-26.1.2.jar`。源代码和原有版权归属、GPL-3.0 许可保持有效；分发修改后的二进制时一并提供对应完整源码和许可。
