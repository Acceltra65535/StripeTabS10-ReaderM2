# Gradle 构建故障排除指南

## 问题描述

构建时出现以下错误：
```
Error resolving plugin [id: 'org.jetbrains.kotlin.android', version: '1.9.24']
> The request for this plugin could not be satisfied because the plugin is already on the classpath 
  with an unknown version, so compatibility cannot be checked.
```

## 原因分析

这是一个已知的Gradle问题，通常由以下原因引起：
1. Gradle缓存中的插件版本冲突
2. Kotlin插件的重复加载
3. gradle配置缓存问题

## 解决方案

### 方案1: 完全清理gradle缓存（推荐）

```bash
# 停止所有gradle daemon
./gradlew --stop

# 删除gradle缓存
rm -rf ~/.gradle
rm -rf ~/.m2

# 删除项目缓存
cd /home/stripe-android
rm -rf .gradle build

# 重新构建
./gradlew clean build
```

### 方案2: 使用不同的Gradle版本

如果方案1不起作用，尝试使用Gradle 7.6或8.0：

编辑 `gradle/wrapper/gradle-wrapper.properties`:
```properties
distributionUrl=https\://services.gradle.org/distributions/gradle-8.0-bin.zip
```

### 方案3: 禁用配置缓存

编辑 `gradle.properties`，确保包含：
```properties
org.gradle.configuration.cache=false
org.gradle.unsafe.isolated.projects=true
```

### 方案4: 在IDE中构建

如果使用Android Studio或IntelliJ IDEA：
1. 打开File > Invalidate Caches
2. 选择"Clear file system cache and local history"
3. 重启IDE
4. 重新构建项目

### 方案5: Docker环境构建

创建一个Dockerfile来在隔离的环境中构建：

```dockerfile
FROM openjdk:21

RUN apt-get update && apt-get install -y \
    android-sdk-linux \
    android-sdk-build-tools

WORKDIR /app
COPY . .

RUN ./gradlew clean build
```

然后运行：
```bash
docker build -t stripe-android .
```

## 替代构建方法

### 使用Gradle Wrapper的直接方式

```bash
# 刷新依赖
./gradlew --refresh-dependencies clean build

# 使用no-daemon模式
./gradlew --no-daemon build

# 跳过某些任务
./gradlew -x lint -x test build
```

### 编译特定模块

```bash
# 只编译应用模块
./gradlew :app:build

# 编译并输出详细日志
./gradlew -d :app:build
```

## 验证构建成功的指标

构建成功时应该看到：
```
BUILD SUCCESSFUL in XXs
```

## 预期输出文件

成功构建后，会生成：
- `app/build/outputs/apk/debug/app-debug.apk` - Debug APK
- `app/build/outputs/bundle/release/app-release.aab` - Release Bundle

## 如果仍然出现问题

### 检查环境

```bash
# 检查Java版本
java -version

# 检查Gradle版本
./gradlew --version

# 检查Kotlin版本
./gradlew kotlinVersion
```

### 查看详细日志

```bash
./gradlew build --stacktrace --debug
```

### 在干净的环境中重新克隆

```bash
git clone <repository-url> stripe-android-clean
cd stripe-android-clean
./gradlew build
```

## 性能优化

如果构建速度很慢，尝试以下优化：

```properties
# gradle.properties
org.gradle.jvmargs=-Xmx4096m
org.gradle.parallel=true
org.gradle.workers.max=8
org.gradle.caching=true
```

## 常见命令参考

```bash
# 完全清理并构建
./gradlew clean build

# 只运行单元测试
./gradlew test

# 运行UI测试
./gradlew connectedAndroidTest

# 创建APK
./gradlew assembleDebug
./gradlew assembleRelease

# 创建运行应用
./gradlew installDebug

# 创建束包
./gradlew bundleRelease

# 查看所有任务
./gradlew tasks

# 生成依赖树
./gradlew dependencies

# 检查插件版本
./gradlew pluginDependencies
```

## 联系支持

如果问题仍未解决，请提供以下信息：
- `./gradlew --version` 的输出
- `java -version` 的输出
- 完整的构建日志（使用 `./gradlew build --stacktrace` 生成）
- 操作系统和环境详情

---

**最后更新**: 2026年7月3日
