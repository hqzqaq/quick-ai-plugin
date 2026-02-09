# Quick AI Plugin

[English](README_EN.md) | [中文](README_CN.md)

---

## English

### Features

Quick AI Plugin is an IntelliJ IDEA plugin that enables you to quickly open the current file in VS Code, Cursor, Trae, or any VS Code-based AI IDE with precise cursor positioning.

#### Key Features

- **Precise Cursor Positioning**: Automatically jump to the exact cursor position (line and column) when opening files in the target IDE
- **Multi-IDE Support**: Supports VS Code, Cursor, Trae, and other VS Code-based IDEs
- **Cross-Platform Support**: Fully supports Windows, macOS, and Linux
- **Multi-IDE Management**: Configure multiple IDEs and set a default IDE
- **Customizable Shortcuts**: Support for keyboard and mouse shortcuts with customizable key combinations
- **Internationalization**: Supports Chinese and English interfaces

### Usage

#### 1. Install the Plugin

1. Open `Settings` > `Plugins` in IntelliJ IDEA
2. Click the `Marketplace` tab
3. Search for `Quick AI Plugin`
4. Click `Install` to install
5. Restart the IDE

#### 2. Configure IDE Path

1. Open `Settings` > `Tools` > `Quick AI Plugin`
2. Click the `Add IDE` button in the `IDE Configuration` section
3. Enter the IDE name (e.g., VS Code, Cursor)
4. Click `Browse` to select the IDE executable path
5. Click `Set Default` to set as the default IDE (optional)

**Common IDE Executable Paths**:

- **Windows**:
  - VS Code: `C:\Users\<Username>\AppData\Local\Programs\Microsoft VS Code\Code.exe`
  - Cursor: `C:\Users\<Username>\AppData\Local\Programs\cursor\Cursor.exe`

- **macOS**:
  - VS Code: `/Applications/Visual Studio Code.app/Contents/Resources/app/bin/code`
  - Cursor: `/Applications/Cursor.app/Contents/Resources/app/bin/code`
  - Trae: `/Applications/Trae CN.app/Contents/Resources/app/bin/code`

- **Linux**:
  - VS Code: `/usr/share/code/code`
  - Cursor: `/usr/bin/cursor`

#### 3. Use Shortcuts

The plugin provides three ways to open files:

##### Method 1: Keyboard Shortcut

- **Windows/Linux**: `Ctrl + Shift + Alt + O`
- **macOS**: `Cmd + Shift + Alt + O`

##### Method 2: Mouse Shortcut

- **Windows/Linux**: `Ctrl + Shift + Alt + Left Click`
- **macOS**: `Cmd + Shift + Alt + Left Click`

##### Method 3: Menu Operations

1. Right-click in the editor and select `Open in AI IDE`
2. Or select `Tools` > `Open in AI IDE` from the top menu bar

### Implementation Principles

#### Technical Architecture

The plugin is built on the IntelliJ Platform SDK, written in Kotlin, and follows the Service-Oriented Architecture (SOA) pattern.

#### Core Components

##### 1. Cursor Position Service (CursorPositionService)

Responsible for getting the precise cursor position in the current editor:

```kotlin
suspend fun getCurrentCursorPosition(project: Project): CursorPosition?
```

**Implementation**:
- Uses IntelliJ Platform's `FileEditorManager` to get the current editor
- Gets cursor offset through `CaretModel`
- Converts offset to line and column numbers using `Document` API
- Uses `readAction` to ensure thread safety

**Thread Safety**: This method can be called from any thread, internally uses ReadAction to ensure thread safety

##### 2. IDE Command Service (IdeCommandService)

Responsible for executing IDE open commands on different operating systems:

```kotlin
fun openFileInIde(project: Project, idePath: String, projectPath: String, cursorPosition: CursorPosition, onSuccess: () -> Unit, onError: (String) -> Unit)
```

**Implementation**:
- Detects operating system type (Windows/macOS/Linux)
- Builds cross-platform commands:
  - Windows: `cmd /c "idePath" projectPath -g filePath:line:column`
  - macOS/Linux: `idePath projectPath -g filePath:line:column`
- Uses `ProcessBuilder` to execute commands
- Uses `Task.Backgroundable` to execute in background thread with progress indication

**Thread Safety**: This method can be called from any thread, internally uses Task.Backgroundable to execute in background thread

##### 3. Configuration Management (PluginSettings)

Responsible for persisting user configurations:

```kotlin
@State(name = "QuickAiPluginSettings", storages = [Storage("QuickAiPluginSettings.xml")])
@Service(Service.Level.PROJECT)
class PluginSettings : PersistentStateComponent<PluginSettings.State>
```

**Configuration Items**:
- IDE configuration list (name, path, is default)
- Shortcut enable status
- Custom shortcut
- Language setting

**Persistence Mechanism**: Uses IntelliJ Platform's `PersistentStateComponent` interface, configurations are saved in `.idea/QuickAiPluginSettings.xml` in the project directory

##### 4. Mouse Listener (IdeEditorMouseListener)

Monitors mouse events in the editor and detects shortcut combinations:

```kotlin
override fun mouseReleased(event: EditorMouseEvent)
```

**Implementation**:
- Listens to mouse release events
- Detects modifier key combinations (Ctrl/Shift/Alt/Meta)
- Detects click type (left click)
- Validates if current window is active
- Triggers open IDE operation

**Thread Safety**: This method is called on EDT, internally uses coroutines to execute in background

##### 5. UI Configuration Interface (PluginConfigurable)

Settings page built with Kotlin UI DSL v2:

**Features**:
- IDE configuration management (add, delete, set default)
- Shortcut configuration
- Language selection
- Shortcut recording

**Thread Safety**: UI operations execute on EDT, background operations use coroutines

#### Asynchronous Processing Mechanism

The plugin uses Kotlin Coroutines for asynchronous operations to ensure the UI thread is not blocked:

```kotlin
private val coroutineScope = CoroutineScope(Dispatchers.IO)

coroutineScope.launch {
    val cursorPosition = cursorPositionService.getCurrentCursorPosition(project)
    // Background processing
}
```

**Advantages**:
- Avoids blocking EDT (Event Dispatch Thread)
- Provides better user experience
- Supports cancellation

#### Cross-Platform Support

The plugin builds different commands by detecting the operating system type:

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

#### Extension Points

The plugin uses IntelliJ Platform's extension point mechanism:

- `projectConfigurable`: Register settings page
- `postStartupActivity`: Register startup activity to initialize mouse listener
- `action`: Register menu actions and shortcuts
- `notificationGroup`: Register notification group

### Tech Stack

- **Language**: Kotlin 2.1.20
- **Platform**: IntelliJ Platform 2025.2.4
- **Build Tool**: Gradle 8.11
- **Async Framework**: Kotlin Coroutines
- **UI Framework**: Kotlin UI DSL v2
- **JDK Version**: 21

### Project Structure

```
quick-ai-plugin/
├── src/main/kotlin/com/hqz/quick/ai/quickaiplugin/
│   ├── action/                    # Actions
│   │   └── OpenInIdeAction.kt     # Action to open IDE
│   ├── config/                    # Configuration
│   │   └── PluginSettings.kt      # Plugin configuration management
│   ├── listener/                  # Listeners
│   │   ├── IdeEditorMouseListener.kt         # Editor mouse listener
│   │   ├── IdeEditorMouseListenerManager.kt  # Listener manager
│   │   └── MyPluginPostStartupActivity.kt    # Startup activity
│   ├── service/                   # Services
│   │   ├── CursorPositionService.kt           # Cursor position service
│   │   ├── FileChooserService.kt              # File chooser service
│   │   └── IdeCommandService.kt              # IDE command service
│   ├── ui/                        # UI Components
│   │   ├── AddIdeDialog.kt                    # Add IDE dialog
│   │   ├── PluginConfigurable.kt             # Plugin configuration UI
│   │   └── ShortcutRecorderDialog.kt          # Shortcut recorder dialog
│   └── util/                      # Utilities
│       ├── I18nService.kt                     # Internationalization service
│       └── LogService.kt                      # Logging service
└── src/main/resources/
    ├── META-INF/
    │   ├── plugin.xml            # Plugin configuration file
    │   └── pluginIcon.svg        # Plugin icon
    └── messages/                  # Internationalization resources
        ├── QuickAiPluginBundle.properties           # English resources
        └── QuickAiPluginBundle_zh_CN.properties     # Chinese resources
```

### Build and Development

#### Requirements

- JDK 21 or higher
- IntelliJ IDEA 2025.2.4 or higher
- Gradle 8.11

#### Build Plugin

```bash
./gradlew buildPlugin
```

#### Run Plugin

```bash
./gradlew runIde
```

#### Verify Plugin

```bash
./gradlew verifyPlugin
```

### Contributing

Issues and Pull Requests are welcome!

### License

MIT License

### Author

[hqzqaq](https://github.com/hqzqaq)
