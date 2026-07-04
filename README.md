# Seburo Pte Ltd - Stripe Reader M2 Terminal Application

A complete production-grade Android application for Stripe Reader M2 payment terminal on Samsung Tab S10+ tablet.

## Features

✅ **USB Connection Support** - Connect Tab S10+ to Stripe Reader M2 via data USB cable
✅ **Complete Payment Flow** - From discovery, connection, payment intent creation to collection and confirmation
✅ **Real-time USB Monitoring** - Auto-detect USB device connection/disconnection events
✅ **Receipt Generation & Storage** - Auto-generate and save payment receipts locally
✅ **Payment Result Management** - Complete error handling for success, failure, and cancellation
✅ **Multi-Currency Support** - Support for SGD and other currencies (via Stripe)
✅ **Real-time Logging** - Complete operation log tracking
✅ **Offline Support** - Stripe Terminal SDK's offline payment capability integration

## Project Architecture

### Core Components

1. **TerminalViewModel.kt** - Main business logic container
   - Terminal initialization and configuration management
   - USB reader discovery and connection
   - Payment flow orchestration
   - Real-time state management

2. **TerminalFragment.kt** - UI layer
   - User interaction handling
   - Payment status display
   - Payment result dialogs
   - Button state management

3. **UsbDeviceMonitor.kt** - USB device monitoring
   - Listen for USB connection/disconnection events
   - Dynamic connection status updates

4. **ReceiptManager.kt** - Receipt handling
   - Generate formatted payment receipts
   - Store receipts locally

5. **PaymentResult.kt** - Payment result modeling
   - Success, Failure, Cancelled states

6. **TerminalBackendClient.kt** - Backend communication
   - Fetch connection tokens
   - Create payment intents

## Quick Start

### Prerequisites

- Android 35 (API 35) or higher
- Samsung Tab S10+ or compatible USB Host device
- Stripe Reader M2
- Data USB cable (USB-A or USB-C, depending on device)
- Stripe account and API keys
- Configured backend server

### Setup Steps

1. **Get Stripe API Keys**
   - Visit https://dashboard.stripe.com
   - Obtain publishable key and restricted API key
   - Create Stripe Location ID

2. **Setup Backend Server**
   Backend must implement these two endpoints:

   ```
   POST /terminal/connection-token
   Request body: { "merchant_name": "Seburo Pte Ltd" }
   Response: { "secret": "connection_token_...", ... }

   POST /terminal/payment-intents
   Request body: {
       "amount": 10000,
       "currency": "sgd",
       "merchant_name": "Seburo Pte Ltd",
       "location_id": "tml_...",
       "payment_method_types": ["card_present"],
       "capture_method": "automatic",
       "description": "Seburo Pte Ltd Terminal sale"
   }
   Response: { "client_secret": "pi_..._secret_..." }
   ```

3. **Configure Application**
   - Open the app
   - Navigate to "Settings"
   - Fill in the following information:
     - **Merchant Name**: Seburo Pte Ltd
     - **Stripe Location ID**: tml_* ID from Stripe
     - **Backend Base URL**: Your backend server URL (e.g., https://your-backend.com)
     - **Connection Token Path**: /terminal/connection-token
     - **Payment Intent Path**: /terminal/payment-intents
     - **Default Currency**: SGD
     - **Default Amount**: 1000 (in minor units, SGD cents)
   - Click "Save settings"

4. **Connect Reader Device**
   - Connect Tab S10+ to Stripe Reader M2 with USB cable
   - Click "Discover USB reader" button
   - Select discovered reader
   - Click "Connect selected reader" to connect

## Usage Flow

### Basic Payment Flow

1. **Enter Amount**
   - Enter amount in "Amount in minor units" field (e.g., 1000 = 10.00 SGD)
   - Optionally change "Currency code" (default SGD)

2. **Collect Payment**
   - Click "Collect & charge" button
   - Customer taps or inserts card on Stripe Reader M2
   - Wait for payment processing

3. **Confirm Result**
   - Success: Shows confirmation dialog with amount, receipt number, and status
   - Failure: Shows error message and error code
   - Cancelled: Shows cancellation notification

4. **Receipt**
   - Auto-generated and saved to device local storage
   - Formatted receipt includes: merchant name, date/time, amount, last 4 card digits, receipt number

## Dependencies

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

## Permissions

App requires the following permissions:

- `android.permission.INTERNET` - Communicate with Stripe backend
- `android.permission.USB_PERMISSION` - Access USB devices
- `android.permission.ACCESS_NETWORK_STATE` - Check network state
- `android.permission.CHANGE_NETWORK_STATE` - Manage network connections
- USB Host feature - Required (hardware feature)

## File Structure

```
app/src/main/
├── java/com/example/stripeseburoterminal/
│   ├── MainActivity.kt
│   ├── R.kt (generated)
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

## Development & Testing

### Local Build

```bash
# Build project
./gradlew build

# Run unit tests
./gradlew test

# Install on connected device
./gradlew installDebug

# Generate release build (requires signing configuration)
./gradlew assembleRelease
```

### Logging & Debugging

App provides detailed operation logs:
- Terminal initialization
- USB reader discovery
- Connection status changes
- Payment flow events
- Errors and exceptions

All logs are displayed in real-time in the UI "Logs" section.

## Troubleshooting

### Issue: USB Device Not Discovered
**Solution**:
- Ensure USB cable supports data transfer (some cables are charge-only)
- Check if USB debugging is enabled on Tab S10+
- Try disconnecting and reconnecting the device
- Check Device Manager to verify Reader M2 is recognized

### Issue: Connection Failed
**Solution**:
- Verify Stripe Location ID is correct
- Check backend server is running and accessible
- Ensure network connection is working
- Review app logs for detailed error information

### Issue: Payment Flow Failed
**Solution**:
- Confirm sufficient API quota in Stripe account
- Verify card is valid (use Stripe test cards for testing)
- Check amount format is correct (in minor units)
- Review payment status and error code

## Test Payments

Use Stripe-provided test cards:

| Card Number | Expiry | CVC | Result |
|-------------|--------|-----|--------|
| 4242 4242 4242 4242 | 12/25 | 123 | Success |
| 4000 0000 0000 9995 | 12/25 | 123 | Declined |
| 4000 0000 0000 0002 | 12/25 | 123 | Declined |

**Important**: Use test cards only in test environment. Never use in production.

## Security Considerations

- Never hardcode API keys in code
- Use environment variables or secure config management for sensitive information
- All backend communication must use HTTPS
- Validate all user input
- Keep dependencies updated for security patches
- Enable ProGuard/R8 obfuscation in release builds

## License

This application is proprietary software of Seburo Pte Ltd. All rights reserved.

## Technical Support

For questions or support, contact Seburo Pte Ltd technical support team.

---

**Developer**: GitHub Copilot
**Last Updated**: July 3, 2026
**App Version**: 1.0
**API Level**: Android 36 (SDK)
