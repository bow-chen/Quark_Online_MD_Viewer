# 夸克网盘 Markdown 在线阅读器 (Quark MD Reader)

一款专为**手机和平板**设计的 Android 原生应用，完美解决**夸克网盘中 Markdown 文件图片相对路径（如 `images/xxx.png`）无法渲染显示**的问题，且**完全不下载落盘、不占手机本地存储空间**。

---

## 🌟 核心特性

1. **相对路径图片精准智能加载**：
   - 解决原生夸克与其它 App 渲染 MD 时无法找到 `![img](images/foo.png)` 相对路径图片导致破损的问题。
   - 自动获取当前 MD 文件在夸克网盘中的目录，通过内置的 `QuarkImageFetcher` 拦截器将相对路径转换为网盘节点进行异步流式解码。
2. **零存储占用（纯内存流式读取）**：
   - 阅读 Markdown 与查看图片均采用内存流（InputStream）直接解码上屏。
   - 不像常规同步网盘那样把整个目录下载到手机，退出阅读即清理，极大减轻手机与平板的存储压力。
3. **高保真 Markdown 渲染**：
   - 采用原生 **Markwon** + **Jetpack Compose**，支持代码高亮、Markdown 表格、任务复选框、删除线等。
   - 支持双击/轻触图片，原生性能流畅不掉帧。
4. **内置夸克登录与网盘浏览**：
   - 内置安全 Web 登录，一键获取 Cookie/Token。
   - 树状目录导航，无缝浏览网盘各层级文件夹与 MD 文档。

---

## 🏗️ 项目架构与关键代码目录

```
PythonProject/
├── app/
│   ├── build.gradle.kts                          # App 模块依赖
│   └── src/main/
│       ├── AndroidManifest.xml                  # 权限与应用配置
│       └── java/com/example/quarkmdreader/
│           ├── MainActivity.kt                  # 主入口与导航状态机
│           ├── QuarkApplication.kt             # Application 配置
│           ├── data/
│           │   ├── api/
│           │   │   └── QuarkApiService.kt       # 夸克 API: 目录树/MD流读取/直链
│           │   ├── image/
│           │   │   └── QuarkImageFetcher.kt     # ★核心: Coil 相对路径图片拦截与流式解码
│           │   └── model/
│           │       └── QuarkFile.kt             # 文件与目录数据结构
│           └── ui/
│               ├── component/
│               │   └── MarkdownViewer.kt        # Markwon 原生渲染组件
│               └── screen/
│                   ├── QuarkLoginScreen.kt      # 夸克网盘 Web 登录页
│                   ├── FileExplorerScreen.kt    # 网盘目录树浏览页
│                   └── MarkdownReaderScreen.kt  # MD 在线流式阅读页
├── gradle/
│   ├── libs.versions.toml                       # 统一版本控制
│   └── wrapper/gradle-wrapper.properties        # Gradle 8.10.2
├── build.gradle.kts                             # 根构建脚本
└── settings.gradle.kts                          # 模块管理
```

---

## 🚀 编译与打包 APK 指南

### 方式 1：使用 Android Studio（推荐）
1. 打开 **Android Studio**，点击 `File` -> `Open`，选择当前目录 `PythonProject`。
2. Android Studio 会自动下载 Gradle 与相关依赖（Markwon, Coil, Compose 等）。
3. 点击顶部菜单 `Build` -> `Build Bundle(s) / APK(s)` -> `Build APK(s)`。
4. 编译完成后，在右下角点击 `locate` 或前往 `app/build/outputs/apk/debug/` 即可获取 `app-debug.apk`。

### 方式 2：使用命令行（Gradle）
在项目根目录下执行：
```bash
./gradlew assembleDebug
```
生成的 APK 路径为：`app/build/outputs/apk/debug/app-debug.apk`。
将 APK 安装至手机/平板（或通过 `adb install app/build/outputs/apk/debug/app-debug.apk`）即可立即使用！
