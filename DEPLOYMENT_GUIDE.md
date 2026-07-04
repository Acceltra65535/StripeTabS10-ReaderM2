# Seburo Pte Ltd - Stripe Reader M2 终端应用部署指南

## 目录
1. [部署前准备](#部署前准备)
2. [开发环境设置](#开发环境设置)
3. [构建应用](#构建应用)
4. [部署到设备](#部署到设备)
5. [生产环境配置](#生产环境配置)
6. [验收测试](#验收测试)
7. [常见问题](#常见问题)

## 部署前准备

### 硬件要求
- **平板电脑**: Samsung Galaxy Tab S10+ (或兼容USB Host的Android设备)
- **支付终端**: Stripe Reader M2
- **连接线**: 数据USB线 (USB 2.0+)
- **网络**: WiFi或蜂窝网络连接

### 软件要求
- **Android**: 15+ (API 35+)
- **Stripe账户**: 已激活并配置
- **后端服务器**: 已部署并运行

### Stripe配置清单
- [ ] 获取Stripe Account ID
- [ ] 获取受限API密钥
- [ ] 创建Reader M2设备配置
- [ ] 生成Location ID (tml_*格式)
- [ ] 配置webhook (可选，用于异步处理)

### 后端服务器准备

确保后端服务器已实现以下API端点：

#### 1. 连接令牌端点
```
POST /terminal/connection-token
Authorization: Bearer <API_KEY>

Request:
{
  "merchant_name": "Seburo Pte Ltd"
}

Response:
{
  "secret": "connection_token_rsa_..."
}
```

#### 2. 支付意图端点
```
POST /terminal/payment-intents
Authorization: Bearer <API_KEY>

Request:
{
  "amount": 10000,
  "currency": "sgd",
  "merchant_name": "Seburo Pte Ltd",
  "location_id": "tml_...",
  "payment_method_types": ["card_present"],
  "capture_method": "automatic",
  "description": "Seburo Pte Ltd Terminal sale"
}

Response:
{
  "client_secret": "pi_..._secret_..."
}
```

## 开发环境设置

### 1. 安装Android Studio
```bash
# 下载并安装 Android Studio
# https://developer.android.com/studio

# 安装必要的SDK
Android SDK 36
Android Build Tools 36
```

### 2. 克隆项目
```bash
git clone <repository-url>
cd stripe-android
```

### 3. 配置本地开发环境
```bash
# 创建 local.properties
echo "sdk.dir=/path/to/android/sdk" > local.properties
```

### 4. 安装依赖
```bash
./gradlew dependencies
```

## 构建应用

### Debug构建（开发用）
```bash
# 构建Debug APK
./gradlew assembleDebug

# 输出位置: app/build/outputs/apk/debug/app-debug.apk
```

### Release构建（生产用）

#### 创建签名密钥
```bash
# 生成密钥库
keytool -genkey -v -keystore seburo-terminal.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias seburo-terminal

# 提示输入:
# - 密钥库密码
# - 密钥密码
# - 公司名: Seburo Pte Ltd
# - 部门: Development
# - 地点: Singapore
```

#### 配置签名信息
编辑 `local.properties`:
```properties
# Signing configuration
RELEASE_STORE_FILE=/path/to/seburo-terminal.jks
RELEASE_STORE_PASSWORD=<your-keystore-password>
RELEASE_KEY_ALIAS=seburo-terminal
RELEASE_KEY_PASSWORD=<your-key-password>
```

编辑 `app/build.gradle.kts`:
```kotlin
signingConfigs {
    release {
        storeFile = file(System.getenv("RELEASE_STORE_FILE") ?: "seburo-terminal.jks")
        storePassword = System.getenv("RELEASE_STORE_PASSWORD")
        keyAlias = System.getenv("RELEASE_KEY_ALIAS")
        keyPassword = System.getenv("RELEASE_KEY_PASSWORD")
    }
}

buildTypes {
    release {
        signingConfig = signingConfigs.release
        isMinifyEnabled = true
        isShrinkResources = true
        proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
}
```

#### 构建Release APK
```bash
./gradlew assembleRelease

# 输出位置: app/build/outputs/apk/release/app-release.apk
```

#### 或构建App Bundle (推荐用于Google Play)
```bash
./gradlew bundleRelease

# 输出位置: app/build/outputs/bundle/release/app-release.aab
```

## 部署到设备

### 1. 连接Samsung Tab S10+
```bash
# 使用USB线连接设备
# 在设备上启用USB调试
# 设置 > 关于平板电脑 > 开发者选项 > USB调试

# 验证连接
./gradlew devices
```

### 2. 安装应用

#### Debug应用（开发用）
```bash
./gradlew installDebug

# 或手动安装
adb install app/build/outputs/apk/debug/app-debug.apk
```

#### Release应用（生产用）
```bash
# 方式1: 直接安装
adb install app/build/outputs/apk/release/app-release.apk

# 方式2: 使用Google Play (需要上传到Google Play Console)
# 上传 app-release.aab 到 Google Play Console
```

### 3. 验证安装
```bash
# 检查应用是否已安装
adb shell pm list packages | grep stripeseburoterminal

# 启动应用
adb shell am start -n com.example.stripeseburoterminal/.MainActivity
```

## 生产环境配置

### 在设备上配置应用

1. **打开应用**
   - 从启动器启动"Seburo Terminal"

2. **进入Settings**
   - 点击"Settings"菜单项

3. **填写配置信息**
   ```
   Merchant Name: Seburo Pte Ltd
   Stripe Location ID: tml_... (从Stripe账户获取)
   Backend Base URL: https://your-backend.com
   Connection Token Path: /terminal/connection-token
   Payment Intent Path: /terminal/payment-intents
   Default Currency: SGD
   Default Amount: 1000
   ```

4. **保存设置**
   - 点击"Save settings"按钮

### USB Reader M2连接

1. **准备设备**
   - 将Stripe Reader M2打开
   - 确保电池有充电

2. **连接Tab S10+**
   - 使用USB线将Tab S10+与Reader M2连接
   - 等待识别

3. **在应用中连接**
   - 点击"Discover USB reader"
   - 应用会自动扫描并列出可用的阅读器
   - 选择发现的Reader M2
   - 点击"Connect selected reader"

## 验收测试

### 测试检查清单

#### 系统测试
- [ ] 应用在Tab S10+上成功安装
- [ ] 应用启动无崩溃
- [ ] 设置界面正常工作
- [ ] USB Reader M2可被发现
- [ ] Reader M2可成功连接

#### 支付测试

使用Stripe测试卡进行测试：

| 卡号 | 过期日期 | CVC | 结果 |
|------|---------|-----|------|
| 4242 4242 4242 4242 | 12/25 | 123 | 成功 |
| 4000 0000 0000 9995 | 12/25 | 123 | 拒绝 |

- [ ] 成功支付流程（金额1000 SGD）
- [ ] 支付确认显示正确
- [ ] 收据正确生成和存储
- [ ] 支付失败处理正确

#### 错误处理测试
- [ ] USB断开连接时处理正确
- [ ] 网络断开连接时处理正确
- [ ] 后端不可用时处理正确
- [ ] 无效金额输入处理正确

#### 用户体验测试
- [ ] 所有按钮在繁忙时禁用
- [ ] 状态更新实时显示
- [ ] 错误消息清晰可解
- [ ] UI在各种屏幕尺寸上显示正常

### 性能测试
- [ ] 支付流程响应时间 < 10秒
- [ ] 应用内存使用 < 200MB
- [ ] 长时间运行无内存泄漏

## 常见问题

### Q: USB Reader M2无法被发现
**A**: 
- 检查USB线是否支持数据传输
- 确保Reader M2已打开
- 在Android设置中检查USB权限
- 尝试重新连接设备

### Q: 连接失败
**A**:
- 验证Stripe Location ID正确
- 检查后端服务器是否运行
- 确认网络连接正常
- 查看应用日志获取详细错误

### Q: 支付流程挂起
**A**:
- 检查网络连接
- 确认卡片有效
- 查看Reader M2上的错误信息
- 尝试重新发起支付

### Q: 应用崩溃
**A**:
- 重启应用
- 清除应用缓存 (Settings > Apps > Clear Cache)
- 重新启动设备
- 检查日志 (adb logcat)

### Q: 如何更新应用？
**A**:
- 构建新版本
- 通过 `adb install -r` 覆盖安装
- 或通过Google Play自动更新

## 维护和监控

### 日志收集
```bash
# 实时查看日志
adb logcat

# 保存日志到文件
adb logcat > logfile.txt

# 过滤应用日志
adb logcat | grep stripeseburoterminal
```

### 应用更新检查清单
- [ ] 新版本在本地测试
- [ ] 所有支付测试通过
- [ ] 没有发现新的错误
- [ ] 增量版本号更新
- [ ] 发行说明已准备

## 紧急回滚

如果在生产环境中发现严重问题：

```bash
# 卸载应用
adb uninstall com.example.stripeseburoterminal

# 安装上一个稳定版本
adb install app-backup/app-stable-release.apk

# 验证
adb shell am start -n com.example.stripeseburoterminal/.MainActivity
```

---

**版本**: 1.0
**最后更新**: 2026年7月3日
**兼容性**: Android 15+ (API 35+)
**支持设备**: Samsung Tab S10+ 及其他支持USB Host的Android设备
