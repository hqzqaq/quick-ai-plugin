# Quick AI Plugin

[English](README_EN.md) | [中文](README_CN.md)

### 功能介绍

Quick AI Plugin 是一款 IntelliJ IDEA 插件，让您能够快速在 VS Code、Cursor、Trae 或任何基于 VS Code 的 AI IDE 中打开当前文件，并精确跳转到光标位置。

#### 核心功能

- **精确光标定位**：在目标 IDE 中打开文件时，自动跳转到当前光标所在的位置（行号和列号）
- **多 IDE 支持**：支持 VS Code、Cursor、Trae 以及其他基于 VS Code 的 IDE
- **跨平台支持**：完美支持 Windows、macOS 和 Linux 操作系统
- **多 IDE 管理**：可以配置多个 IDE，并设置默认 IDE
- **自定义快捷键**：支持键盘快捷键和鼠标快捷键，可自定义快捷键组合
- **国际化支持**：支持中文和英文界面

### 使用方式

#### 1. 安装插件

1. 在 IntelliJ IDEA 中打开 `Settings` > `Plugins`
2. 点击 `Marketplace` 标签
3. 搜索 `Quick AI Plugin`
4. 点击 `Install` 安装
5. 重启 IDE

#### 2. 配置 IDE 路径

1. 打开 `Settings` > `Tools` > `Quick AI Plugin`
2. 在 `IDE Configuration` 区域点击 `Add IDE` 按钮
3. 输入 IDE 名称（如：VS Code、Cursor）
4. 点击 `Browse` 选择 IDE 可执行文件路径
5. 点击 `Set Default` 设置为默认 IDE（可选）

**常见 IDE 可执行文件路径**：

- **Windows**：
  - VS Code: `C:\Users\<用户名>\AppData\Local\Programs\Microsoft VS Code\Code.exe`
  - Cursor: `C:\Users\<用户名>\AppData\Local\Programs\cursor\Cursor.exe`

- **macOS**：
  - VS Code: `/Applications/Visual Studio Code.app/Contents/Resources/app/bin/code`
  - Cursor: `/Applications/Cursor.app/Contents/Resources/app/bin/code`
  - Trae: `/Applications/Trae CN.app/Contents/Resources/app/bin/code`

- **Linux**：
  - VS Code: `/usr/share/code/code`
  - Cursor: `/usr/bin/cursor`

#### 3. 使用快捷键

插件提供三种方式打开文件：

##### 方式一：键盘快捷键

- **Windows/Linux**: `Ctrl + Shift + Alt + O`
- **macOS**: `Cmd + Shift + Alt + O`

##### 方式二：鼠标快捷键

- **Windows/Linux**: `Ctrl + Shift + Alt + 左键点击`
- **macOS**: `Cmd + Shift + Alt + 左键点击`

##### 方式三：菜单操作

1. 在编辑器中右键，选择 `Open in AI IDE`
2. 或在顶部菜单栏选择 `Tools` > `Open in AI IDE`

### 实现原理

#### 技术架构

插件基于 IntelliJ Platform SDK 开发，采用 Kotlin 语言编写，使用 Service-Oriented Architecture（SOA）架构模式。

#### 核心组件

##### 1. 光标位置服务（CursorPositionService）

负责获取当前编辑器中光标的精确位置信息：

```kotlin
suspend fun getCurrentCursorPosition(project: Project): CursorPosition?
```

**实现原理**：
- 使用 IntelliJ Platform 的 `FileEditorManager` 获取当前编辑器
- 通过 `CaretModel` 获取光标偏移量
- 使用 `Document` API 将偏移量转换为行号和列号
- 使用 `readAction` 确保线程安全

**线程安全**：此方法可在任意线程调用，内部使用 ReadAction 确保线程安全

##### 2. IDE 命令服务（IdeCommandService）

负责在不同操作系统上执行 IDE 打开命令：

```kotlin
fun openFileInIde(project: Project, idePath: String, projectPath: String, cursorPosition: CursorPosition, onSuccess: () -> Unit, onError: (String) -> Unit)
```

**实现原理**：
- 检测操作系统类型（Windows/macOS/Linux）
- 构建跨平台命令：
  - Windows: `cmd /c "idePath" projectPath -g filePath:line:column`
  - macOS/Linux: `idePath projectPath -g filePath:line:column`
- 使用 `ProcessBuilder` 执行命令
- 使用 `Task.Backgroundable` 在后台线程执行，提供进度指示

**线程安全**：此方法可在任意线程调用，内部使用 Task.Backgroundable 在后台线程执行

##### 3. 配置管理（PluginSettings）

负责持久化用户配置：

```kotlin
@State(name = "QuickAiPluginSettings", storages = [Storage("QuickAiPluginSettings.xml")])
@Service(Service.Level.PROJECT)
class PluginSettings : PersistentStateComponent<PluginSettings.State>
```

**配置项**：
- IDE 配置列表（名称、路径、是否默认）
- 快捷键启用状态
- 自定义快捷键
- 语言设置

**持久化机制**：使用 IntelliJ Platform 的 `PersistentStateComponent` 接口，配置保存在项目目录的 `.idea/QuickAiPluginSettings.xml` 文件中

##### 4. 鼠标监听器（IdeEditorMouseListener）

监听编辑器中的鼠标事件，检测快捷键组合：

```kotlin
override fun mouseReleased(event: EditorMouseEvent)
```

**实现原理**：
- 监听鼠标释放事件
- 检测修饰键组合（Ctrl/Shift/Alt/Meta）
- 检测点击类型（左键点击）
- 验证当前窗口是否为活跃窗口
- 触发打开 IDE 操作

**线程安全**：此方法在 EDT 调用，内部使用协程在后台执行

##### 5. UI 配置界面（PluginConfigurable）

使用 Kotlin UI DSL v2 构建的设置页面：

**功能**：
- IDE 配置管理（添加、删除、设置默认）
- 快捷键配置
- 语言选择
- 快捷键录制

**线程安全**：UI 操作在 EDT 执行，后台操作使用协程

#### 异步处理机制

插件使用 Kotlin Coroutines 进行异步操作，确保不阻塞 UI 线程：

```kotlin
private val coroutineScope = CoroutineScope(Dispatchers.IO)

coroutineScope.launch {
    val cursorPosition = cursorPositionService.getCurrentCursorPosition(project)
    // 后台处理
}
```

**优势**：
- 避免阻塞 EDT（Event Dispatch Thread）
- 提供更好的用户体验
- 支持取消操作

#### 跨平台支持

插件通过检测操作系统类型来构建不同的命令：

```kotlin
private fun detectOSType(): OSType {
    val osName = System.getProperty("os.name").lowercase()
    return when {
        osName.contains("win") -> OSType.WINDOWS
        osName.contains("mac") -> OSType.MACOS
        osName.contains("nix") || osName.contains("nux") || osName.contains("aix") -> OSType.LINUX
        else -> OSType.UNKNOWN
    }
}
```

#### 扩展点

插件使用 IntelliJ Platform 的扩展点机制：

- `projectConfigurable`: 注册设置页面
- `postStartupActivity`: 注册启动活动，初始化鼠标监听器
- `action`: 注册菜单操作和快捷键
- `notificationGroup`: 注册通知组

### 技术栈

- **语言**: Kotlin 2.1.20
- **平台**: IntelliJ Platform 2025.2.4
- **构建工具**: Gradle 8.11
- **异步框架**: Kotlin Coroutines
- **UI 框架**: Kotlin UI DSL v2
- **JDK 版本**: 21

### 项目结构

```
quick-ai-plugin/
├── src/main/kotlin/com/hqz/quick/ai/quickaiplugin/
│   ├── action/                    # 操作类
│   │   └── OpenInIdeAction.kt     # 打开 IDE 的 Action
│   ├── config/                    # 配置类
│   │   └── PluginSettings.kt      # 插件配置管理
│   ├── listener/                  # 监听器
│   │   ├── IdeEditorMouseListener.kt         # 编辑器鼠标监听器
│   │   ├── IdeEditorMouseListenerManager.kt  # 监听器管理器
│   │   └── MyPluginPostStartupActivity.kt    # 启动活动
│   ├── service/                   # 服务类
│   │   ├── CursorPositionService.kt           # 光标位置服务
│   │   ├── FileChooserService.kt              # 文件选择服务
│   │   └── IdeCommandService.kt              # IDE 命令服务
│   ├── ui/                        # UI 组件
│   │   ├── AddIdeDialog.kt                    # 添加 IDE 对话框
│   │   ├── PluginConfigurable.kt             # 插件配置界面
│   │   └── ShortcutRecorderDialog.kt          # 快捷键录制对话框
│   └── util/                      # 工具类
│       ├── I18nService.kt                     # 国际化服务
│       └── LogService.kt                      # 日志服务
└── src/main/resources/
    ├── META-INF/
    │   ├── plugin.xml            # 插件配置文件
    │   └── pluginIcon.svg        # 插件图标
    └── messages/                  # 国际化资源
        ├── QuickAiPluginBundle.properties           # 英文资源
        └── QuickAiPluginBundle_zh_CN.properties     # 中文资源
```

### 构建和开发

#### 环境要求

- JDK 21 或更高版本
- IntelliJ IDEA 2025.2.4 或更高版本
- Gradle 8.11

#### 构建插件

```bash
./gradlew buildPlugin
```

#### 运行插件

```bash
./gradlew runIde
```

#### 验证插件

```bash
./gradlew verifyPlugin
```

### 贡献指南

欢迎提交 Issue 和 Pull Request！

### 许可证

MIT License

### 作者

[hqzqaq](https://github.com/hqzqaq)