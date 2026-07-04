# 项目结构文档

## 目录树

```
stripe-android/
├── app/                              # Android应用模块
│   ├── build/                        # 构建输出
│   │   └── outputs/
│   │       └── apk/                  # APK文件
│   │
│   ├── src/
│   │   ├── androidTest/              # Android UI测试
│   │   │   └── java/
│   │   │       └── com/example/stripeseburoterminal/
│   │   │           └── ExampleInstrumentedTest.kt
│   │   │
│   │   ├── main/
│   │   │   ├── java/                 # 主要源代码
│   │   │   │   └── com/example/stripeseburoterminal/
│   │   │   │       ├── MainActivity.kt              # 主活动
│   │   │   │       ├── R.kt                         # 生成的资源类
│   │   │   │       │
│   │   │   │       ├── terminal/                    # 终端相关代码 ⭐
│   │   │   │       │   ├── TerminalViewModel.kt     # 业务逻辑 (核心)
│   │   │   │       │   ├── TerminalFragment.kt      # UI逻辑 (核心)
│   │   │   │       │   ├── TerminalConfig.kt        # 配置数据模型
│   │   │   │       │   ├── TerminalConfigStore.kt   # 配置存储
│   │   │   │       │   ├── TerminalBackendClient.kt # API客户端
│   │   │   │       │   ├── PaymentResult.kt         # 支付结果 (新增)
│   │   │   │       │   ├── ReceiptManager.kt        # 收据管理 (新增)
│   │   │   │       │   └── UsbDeviceMonitor.kt      # USB监控 (新增)
│   │   │   │       │
│   │   │   │       ├── ui/                          # UI组件
│   │   │   │       │   ├── transform/
│   │   │   │       │   ├── slideshow/
│   │   │   │       │   ├── reflow/
│   │   │   │       │   └── settings/
│   │   │   │       │       └── SettingsFragment.kt
│   │   │   │
│   │   │   ├── res/                  # 资源文件
│   │   │   │   ├── drawable/         # 图片资源
│   │   │   │   ├── layout/           # 布局文件
│   │   │   │   │   ├── activity_main.xml
│   │   │   │   │   ├── fragment_terminal.xml        # (修改)
│   │   │   │   │   ├── fragment_settings.xml
│   │   │   │   │   └── ...
│   │   │   │   │
│   │   │   │   ├── menu/             # 菜单资源
│   │   │   │   ├── mipmap/           # 应用图标
│   │   │   │   ├── values/           # 字符串和样式
│   │   │   │   │   └── strings.xml   # (修改)
│   │   │   │   │
│   │   │   │   ├── navigation/       # 导航图
│   │   │   │   └── ...
│   │   │   │
│   │   │   └── AndroidManifest.xml   # 应用清单 (修改)
│   │   │
│   │   └── test/
│   │       └── java/                 # 单元测试
│   │
│   └── build.gradle.kts              # app模块构建脚本
│
├── gradle/                           # Gradle包装器
│   ├── wrapper/
│   │   ├── gradle-wrapper.jar
│   │   └── gradle-wrapper.properties # Gradle版本配置
│   │
│   └── libs.versions.toml            # 依赖版本管理 (修改)
│
├── .gradle/                          # Gradle缓存（本地，不提交）
├── .idea/                            # Android Studio配置（本地）
├── .git/                             # Git仓库
│
├── build.gradle.kts                  # 根项目构建脚本
├── settings.gradle.kts               # 项目配置 (修改)
├── gradle.properties                 # Gradle属性 (修改)
│
├── local.properties                  # 本地开发配置（不提交）
├── gradlew                           # Gradle包装脚本（Linux/Mac）
├── gradlew.bat                       # Gradle包装脚本（Windows）
├── .gitignore                        # Git忽略配置
│
└── 📚 文档文件 (新增)
    ├── README.md                     # 英文文档
    ├── README_CN.md                  # 中文文档
    ├── QUICK_REFERENCE.md            # 快速参考
    ├── DEPLOYMENT_GUIDE.md           # 部署指南
    ├── BUILD_TROUBLESHOOTING.md      # 构建故障排除
    ├── IMPLEMENTATION_SUMMARY.md     # 实现总结
    └── PROJECT_STRUCTURE.md          # 项目结构文档
```

## 核心模块说明

### 📌 Terminal Module (终端模块)

#### TerminalViewModel.kt
**职责**: 业务逻辑和状态管理

**关键功能**:
- Terminal SDK初始化
- USB Reader发现和连接
- 支付流程编排
- 状态管理
- 日志记录

**依赖**:
- TerminalConfigStore
- TerminalBackendClient
- ReceiptManager
- UsbDeviceMonitor

#### TerminalFragment.kt
**职责**: UI展示和用户交互

**关键功能**:
- 状态观察和显示
- 按钮点击处理
- 对话框显示
- 支付结果处理

#### PaymentResult.kt
**职责**: 支付结果建模

**类型**:
- Success: 支付成功
- Failure: 支付失败
- Cancelled: 支付被取消

#### ReceiptManager.kt
**职责**: 收据生成和管理

**功能**:
- 格式化收据生成
- 本地文件存储
- 历史查询

#### UsbDeviceMonitor.kt
**职责**: USB设备监控

**功能**:
- USB连接监听
- USB断开监听
- 状态更新回调

## 编译结果

### 输出文件

```
app/build/outputs/
├── apk/
│   ├── debug/
│   │   └── app-debug.apk                    # Debug版本
│   └── release/
│       └── app-release.apk                  # Release版本
│
├── bundle/
│   └── release/
│       └── app-release.aab                  # App Bundle
│
└── lint-results.sarif                       # Lint报告
```

## 依赖关系图

```
TerminalViewModel
├── TerminalConfigStore ────→ SharedPreferences
├── TerminalBackendClient ──→ HTTP Client
├── ReceiptManager ─────────→ File Storage
├── UsbDeviceMonitor ───────→ USB Events
└── Stripe Terminal SDK

TerminalFragment
├── TerminalViewModel
├── Fragment Resources
└── User Input
```

## 资源文件结构

```
res/
├── values/
│   ├── strings.xml             # 字符串常量
│   ├── colors.xml              # 颜色定义
│   ├── styles.xml              # 样式定义
│   └── themes/                 # 主题
│
├── layout/
│   ├── activity_main.xml       # 主活动布局
│   ├── fragment_terminal.xml   # 终端界面布局 ⭐
│   ├── fragment_settings.xml   # 设置界面布局
│   ├── app_bar_main.xml        # 应用栏布局
│   ├── nav_header_main.xml     # 导航头部布局
│   └── content_main.xml        # 内容布局
│
├── drawable/
│   ├── ic_launcher.xml         # 应用图标
│   └── ...
│
├── mipmap/
│   ├── ic_launcher.xml
│   ├── ic_launcher_round.xml
│   └── ...
│
├── navigation/
│   └── mobile_navigation.xml   # 导航图
│
└── menu/
    ├── activity_main_drawer.xml # 抽屉菜单
    └── overflow.xml             # 溢出菜单
```

## 构建配置

### build.gradle.kts
**配置项**:
- Plugin声明
- Android配置
  - compileSdk: 36
  - minSdk: 35
  - targetSdk: 36
- 依赖声明
- 构建特性启用 (viewBinding)

### settings.gradle.kts
**配置项**:
- Plugin管理
- 仓库配置
- 项目包含

### libs.versions.toml
**版本定义**:
- Android Gradle Plugin: 9.0.1
- Kotlin: 1.9.24
- Stripe Terminal: 5.6.0
- androidx库版本

## 开发工作流

```
1. Clone项目
   ↓
2. 配置local.properties
   ↓
3. ./gradlew build
   ↓
4. 修改源代码
   ↓
5. ./gradlew installDebug
   ↓
6. 测试应用
   ↓
7. ./gradlew assembleRelease
   ↓
8. 发布到设备/Play Store
```

## 性能优化

### 构建优化
```gradle
# gradle.properties
org.gradle.jvmargs=-Xmx4096m
org.gradle.parallel=true
org.gradle.caching=true
```

### ProGuard配置
```
proguard-rules.pro
- 混淆规则
- 保留的类
- 优化设置
```

## 国际化支持

目前支持的语言:
- 英文 (默认)
- 中文 (zh-CN)

字符串资源位置:
```
res/values/strings.xml        # 英文
res/values-zh/strings.xml     # 中文（可选）
```

---

**最后更新**: 2026年7月3日
**项目版本**: 1.0
