package com.example.stripeusbapp

import android.app.Application
import com.stripe.stripeterminal.TerminalApplicationDelegate

class StripeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        TerminalApplicationDelegate.onCreate(this)
    }
}
