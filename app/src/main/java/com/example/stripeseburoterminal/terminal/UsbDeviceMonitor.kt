package com.example.stripeseburoterminal.terminal

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbManager
import androidx.core.content.ContextCompat

class UsbDeviceMonitor(private val context: Context) {

    private var receiver: BroadcastReceiver? = null
    private var isMonitoring = false
    private var onDeviceConnected: (() -> Unit)? = null
    private var onDeviceDisconnected: (() -> Unit)? = null

    fun startMonitoring(
        onConnected: (() -> Unit)? = null,
        onDisconnected: (() -> Unit)? = null
    ) {
        if (isMonitoring) return

        onDeviceConnected = onConnected
        onDeviceDisconnected = onDisconnected

        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                        onDeviceConnected?.invoke()
                    }
                    UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                        onDeviceDisconnected?.invoke()
                    }
                }
            }
        }

        val intentFilter = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }

        ContextCompat.registerReceiver(
            context,
            receiver!!,
            intentFilter,
            ContextCompat.RECEIVER_EXPORTED
        )
        isMonitoring = true
    }

    fun stopMonitoring() {
        if (!isMonitoring || receiver == null) return
        try {
            context.unregisterReceiver(receiver)
        } catch (e: Exception) {
            // Already unregistered
        }
        receiver = null
        isMonitoring = false
    }

    fun isUsbDeviceConnected(): Boolean {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager ?: return false
        return usbManager.deviceList.isNotEmpty()
    }
}
