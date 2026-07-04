# Seburo Pte Ltd Stripe Terminal - 快速参考卡

## 🚀 快速启动

```bash
# 1. 克隆项目
git clone <repo-url>
cd stripe-android

# 2. 构建Debug APK
./gradlew assembleDebug

# 3. 安装到设备
adb install app/build/outputs/apk/debug/app-debug.apk

# 4. 启动应用
adb shell am start -n com.example.stripeseburoterminal/.MainActivity
```

## ⚙️ 应用配置

打开Settings，填写以下信息：

| 字段 | 值示例 | 说明 |
|------|--------|------|
| Merchant Name | Seburo Pte Ltd | 商户名称 |
| Stripe Location ID | tml_XXXXX | 从Stripe获取 |
| Backend Base URL | https://api.example.com | 后端服务器 |
| Connection Token Path | /terminal/connection-token | 默认值 |
| Payment Intent Path | /terminal/payment-intents | 默认值 |
| Default Currency | SGD | 货币代码 |
| Default Amount | 1000 | 最小单位 |

## 💳 支付流程

```
1. 输入金额 → 输入货币
2. 点击 "Discover USB reader" → 选择Reader → 连接
3. 点击 "Collect & charge"
4. 在Reader上挥动/插入卡片
5. 等待确认 → 查看结果
```

## 🧪 测试卡

| 卡号 | CVC | 结果 |
|------|-----|------|
| 4242 4242 4242 4242 | 123 | ✅ 成功 |
| 4000 0000 0000 9995 | 123 | ❌ 拒绝 |

## 📋 关键文件

| 文件 | 用途 |
|------|------|
| TerminalViewModel.kt | 业务逻辑 |
| TerminalFragment.kt | UI界面 |
| PaymentResult.kt | 支付结果 |
| ReceiptManager.kt | 收据管理 |
| UsbDeviceMonitor.kt | USB监控 |

## 🔧 故障排除

### USB Reader无法发现
- ✓ 检查USB线
- ✓ 重新连接设备
- ✓ 重启Reader M2

### 连接失败
- ✓ 验证Location ID
- ✓ 检查后端服务器
- ✓ 检查网络连接

### 支付失败
- ✓ 确认卡片有效
- ✓ 检查余额
- ✓ 查看错误代码

## 📱 支持的设备

- ✅ Samsung Galaxy Tab S10+
- ✅ Android 15+（API 35+）
- ✅ 支持USB Host功能的设备

## 🛠️ 构建命令

```bash
# Debug构建
./gradlew assembleDebug

# Release构建
./gradlew assembleRelease

# 完全清理和构建
./gradlew clean build

# 运行单元测试
./gradlew test

# 查看所有任务
./gradlew tasks
```

## 📝 核心功能

- ✅ USB阅读器发现和连接
- ✅ 完整的支付流程
- ✅ 自动收据生成
- ✅ 实时USB监控
- ✅ 离线支付支持
- ✅ 多币种支持
- ✅ 详细操作日志
- ✅ 完整错误处理

## 🌐 API端点

### 连接令牌
```
POST /terminal/connection-token
Request: { "merchant_name": "Seburo Pte Ltd" }
Response: { "secret": "connection_token_..." }
```

### 支付意图
```
POST /terminal/payment-intents
Request: {
  "amount": 10000,
  "currency": "sgd",
  "merchant_name": "Seburo Pte Ltd",
  "location_id": "tml_...",
  ...
}
Response: { "client_secret": "pi_..._secret_..." }
```

## 📞 支持联系

- 文档: README.md, README_CN.md
- 故障排除: BUILD_TROUBLESHOOTING.md
- 部署指南: DEPLOYMENT_GUIDE.md
- 实现总结: IMPLEMENTATION_SUMMARY.md

## ✅ 验收清单

- [ ] 应用安装成功
- [ ] USB Reader可发现
- [ ] 设置配置正确
- [ ] 测试支付成功
- [ ] 收据生成正确
- [ ] 错误处理正常
- [ ] UI响应正常

## 🔐 安全建议

- ✓ 从不硬编码API密钥
- ✓ 使用HTTPS通信
- ✓ 验证用户输入
- ✓ 定期更新依赖
- ✓ 启用ProGuard混淆

## 📊 性能指标

- 支付流程: < 10秒
- 内存使用: < 200MB
- USB发现: < 30秒
- APK大小: ~50MB

---

**版本**: 1.0
**最后更新**: 2026年7月3日
**应用**: Seburo Pte Ltd Stripe Reader M2 Terminal
