# Seburo Pte Ltd - Stripe Reader M2 Terminal Application

一个完整的生产级Android应用程序，用于Samsung Tab S10+平板电脑的Stripe Reader M2收款终端。

## 功能特性

✅ **USB连接支持** - 通过数据USB线连接Tab S10+和Stripe Reader M2
✅ **完整收款流程** - 从发现、连接、创建支付意图到收集和确认付款
✅ **实时USB监控** - 自动检测USB设备连接/断开事件
✅ **收据生成和存储** - 自动生成并本地保存支付收据
✅ **支付结果管理** - 成功、失败和取消的完整错误处理
✅ **多币种支持** - 支持SGD及其他货币（通过Stripe）
✅ **实时日志** - 完整的操作日志跟踪
✅ **离线支持** - Stripe Terminal SDK的离线支付功能集成

## 项目架构

### 核心组件

1. **TerminalViewModel.kt** - 主要业务逻辑容器
   - 终端初始化和配置管理
   - USB阅读器发现和连接
   - 支付流程编排
   - 实时状态管理

2. **TerminalFragment.kt** - UI层
   - 用户交互处理
   - 支付状态显示
   - 支付结果对话框
   - 按钮状态管理

3. **UsbDeviceMonitor.kt** - USB设备监控
   - 监听USB连接/断开事件
   - 动态更新连接状态

4. **ReceiptManager.kt** - 收据处理
   - 生成格式化的支付收据
   - 本地存储收据数据

5. **PaymentResult.kt** - 支付结果建模
   - Success, Failure, Cancelled状态

6. **TerminalBackendClient.kt** - 后端通信
   - 获取连接令牌
   - 创建支付意图

## 快速开始

### 前置条件

- Android 35 (API 35) 或更高版本
- Samsung Tab S10+ 或兼容的USB Host设备
- Stripe Reader M2
- 数据USB线（USB-A 或 USB-C，取决于设备）
- Stripe账户和API密钥
- 配置的后端服务器

### 配置步骤

1. **获取Stripe API密钥**
   - 访问 https://dashboard.stripe.com
   - 获取可发布密钥和受限API密钥
   - 创建Stripe Location ID

2. **设置后端服务器**
   后端需要实现以下两个端点：

   ```
   POST /terminal/connection-token
   请求体: { "merchant_name": "Seburo Pte Ltd" }
   响应: { "secret": "connection_token_...", ... }

   POST /terminal/payment-intents
   请求体: {
       "amount": 10000,
       "currency": "sgd",
       "merchant_name": "Seburo Pte Ltd",
       "location_id": "tml_...",
       "payment_method_types": ["card_present"],
       "capture_method": "automatic",
       "description": "Seburo Pte Ltd Terminal sale"
   }
   响应: { "client_secret": "pi_..._secret_..." }
   ```

3. **配置应用**
   - 打开应用
   - 进入"Settings"（设置）
   - 填写以下信息：
     - **Merchant Name**: Seburo Pte Ltd
     - **Stripe Location ID**: 从Stripe获取的tml_开头的ID
     - **Backend Base URL**: 你的后端服务器URL（例如 https://your-backend.com）
     - **Connection Token Path**: /terminal/connection-token
     - **Payment Intent Path**: /terminal/payment-intents
     - **Default Currency**: SGD
     - **Default Amount**: 1000（以最小单位表示，如SGD分为100）
   - 点击"Save settings"保存

4. **连接Reader设备**
   - 使用USB线连接Tab S10+和Stripe Reader M2
   - 点击"Discover USB reader"按钮
   - 选择发现的阅读器
   - 点击"Connect selected reader"连接

## 使用流程

### 基本收款流程

1. **输入金额**
   - 在"Amount in minor units"字段输入金额（例如1000表示10.00 SGD）
   - 可选地更改"Currency code"（默认SGD）

2. **收集支付**
   - 点击"Collect & charge"按钮
   - 提示客户在Stripe Reader M2上挥动或插入卡片
   - 等待支付处理

3. **确认结果**
   - 成功：显示确认对话框，金额、收据号和状态
   - 失败：显示错误信息和错误代码
   - 取消：显示取消通知

4. **收据**
   - 自动生成并保存到设备本地存储
   - 格式化的收据包含：商户名称、日期时间、金额、卡号后4位、收据号

## 依赖项

```gradle
- androidx.core:core-ktx:1.10.1
- androidx.appcompat:appcompat:1.6.1
- com.google.android.material:material:1.10.0
- androidx.lifecycle:lifecycle-livedata-ktx:2.6.1
- androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.1
- androidx.navigation:navigation-fragment-ktx:2.6.0
- androidx.navigation:navigation-ui-ktx:2.6.0
- com.stripe:stripeterminal:5.6.0
```

## 权限

应用需要以下权限：

- `android.permission.INTERNET` - 与Stripe后端通信
- `android.permission.USB_PERMISSION` - 访问USB设备
- `android.permission.ACCESS_NETWORK_STATE` - 检查网络状态
- `android.permission.CHANGE_NETWORK_STATE` - 管理网络连接
- USB Host功能 - 必需（hardware feature）

## 文件结构

```
app/src/main/
├── java/com/example/stripeseburoterminal/
│   ├── MainActivity.kt
│   ├── R.kt (生成)
│   └── terminal/
│       ├── TerminalViewModel.kt
│       ├── TerminalFragment.kt
│       ├── TerminalConfig.kt
│       ├── TerminalConfigStore.kt
│       ├── TerminalBackendClient.kt
│       ├── PaymentResult.kt
│       ├── ReceiptManager.kt
│       └── UsbDeviceMonitor.kt
├── res/
│   ├── layout/
│   │   └── fragment_terminal.xml
│   └── values/
│       └── strings.xml
└── AndroidManifest.xml
```

## 开发和测试

### 本地构建

```bash
# 构建项目
./gradlew build

# 运行单元测试
./gradlew test

# 在连接的设备上运行
./gradlew installDebug

# 生成发行版（需要签名配置）
./gradlew assembleRelease
```

### 日志和调试

应用提供详细的操作日志：
- 终端初始化
- USB阅读器发现
- 连接状态变化
- 支付流程事件
- 错误和异常

所有日志实时显示在UI的"Logs"部分。

## 故障排除

### 问题：USB设备未被发现
**解决方案**：
- 确保USB线支持数据传输（某些USB线仅用于充电）
- 检查Tab S10+是否已启用USB调试
- 尝试断开并重新连接设备
- 检查设备管理器中是否识别Reader M2

### 问题：连接失败
**解决方案**：
- 验证Stripe Location ID正确无误
- 检查后端服务器是否运行且可访问
- 确保网络连接正常
- 查看应用日志获取详细错误信息

### 问题：支付流程失败
**解决方案**：
- 确认Stripe账户中有足够的API额度
- 验证卡片是否有效（使用Stripe测试卡进行测试）
- 检查金额格式是否正确（以最小单位）
- 查看支付状态和错误代码

## 支付测试

使用Stripe提供的测试卡：

| 卡号 | 过期日期 | CVC | 结果 |
|------|--------|-----|------|
| 4242 4242 4242 4242 | 12/25 | 123 | 成功 |
| 4000 0000 0000 9995 | 12/25 | 123 | 拒绝 |
| 4000 0000 0000 0002 | 12/25 | 123 | 拒绝 |

**重要**：仅在测试环境中使用测试卡。切勿在生产环境中使用。

## 安全考虑

- 从不在代码中硬编码API密钥
- 使用环境变量或安全配置管理敏感信息
- 所有后端通信都应使用HTTPS
- 验证所有用户输入
- 定期更新依赖项以获取安全补丁
- 启用ProGuard/R8混淆在发行构建中

## 许可证

此应用为Seburo Pte Ltd专有软件。版权所有。

## 技术支持

如有问题或建议，请联系Seburo Pte Ltd的技术支持团队。

---

**开发者**: GitHub Copilot
**最后更新**: 2026年7月3日
**应用版本**: 1.0
**API版本**: Android 36 (SDK)
