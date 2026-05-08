package com.smk.th.cbtsmkth46

import android.app.ActivityManager
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.view.KeyEvent
import android.view.WindowManager
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

class WebView : AppCompatActivity() {

    private var webView: WebView? = null
    private var swipeRefresh: SwipeRefreshLayout? = null
    private var btnNext: Button? = null
    private val handler = Handler(Looper.getMainLooper())
    private var isRequestingPermission = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Security & Layout
        try {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                window.setHideOverlayWindows(true)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                setShowWhenLocked(true)
                setTurnScreenOn(true)
            } else {
                @Suppress("DEPRECATION")
                window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or 
                               WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or 
                               WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Fullscreen Setup
        WindowCompat.setDecorFitsSystemWindows(window, false)
        hideSystemUI()
        
        setContentView(R.layout.activity_web_view)

        webView = findViewById(R.id.webView)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        btnNext = findViewById(R.id.btnNext)

        setupWebView()

        // 2. Disable Back
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Toast.makeText(this@WebView, "Aplikasi Terkunci!", Toast.LENGTH_SHORT).show()
                hideSystemUI()
                bringToFront()
            }
        })

        // Watch for system UI changes to re-hide
        window.decorView.setOnApplyWindowInsetsListener { view, insets ->
            val isVisible = WindowInsetsCompat.toWindowInsetsCompat(insets)
                .isVisible(WindowInsetsCompat.Type.navigationBars())
            if (isVisible) {
                handler.postDelayed({ hideSystemUI() }, 500)
            }
            view.onApplyWindowInsets(insets)
        }
    }

    private fun setupWebView() {
        webView?.let { wv ->
            wv.settings.apply {
                @Suppress("SetJavaScriptEnabled")
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = true
                allowContentAccess = true
                useWideViewPort = true
                loadWithOverviewMode = true
                cacheMode = WebSettings.LOAD_DEFAULT
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            }

            wv.setOnLongClickListener { true }
            wv.isLongClickable = false

            wv.webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    swipeRefresh?.isRefreshing = true
                }
                override fun onPageFinished(view: WebView?, url: String?) {
                    swipeRefresh?.isRefreshing = false
                    hideSystemUI()
                }
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean = false
            }
            wv.loadUrl("http://project.cbt.smkth-jakbar.com/list_jurusan/")
        }
        swipeRefresh?.setOnRefreshListener { webView?.reload() }
    }

    override fun onResume() {
        super.onResume()
        isRequestingPermission = false
        hideSystemUI()
        
        // Check Mandatory Permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                forceOpenOverlaySettings()
                return
            }
            
            val powerManager = getSystemService(POWER_SERVICE) as PowerManager
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                try {
                    isRequestingPermission = true
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName"))
                    startActivity(intent)
                } catch (e: Exception) {
                    isRequestingPermission = false
                    e.printStackTrace()
                }
                return
            }
        }
        bringToFront()
    }

    private fun forceOpenOverlaySettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            isRequestingPermission = true
            Toast.makeText(this, "WAJIB: Aktifkan 'Appear on Top'!", Toast.LENGTH_LONG).show()
            try {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            } catch (e: Exception) {
                isRequestingPermission = false
                e.printStackTrace()
            }
        }
    }

    private fun bringToFront() {
        if (isRequestingPermission) return
        try {
            val activityManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
            activityManager.moveTaskToFront(taskId, ActivityManager.MOVE_TASK_NO_USER_ACTION)
            
            val intent = Intent(this, WebView::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun hideSystemUI() {
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus && !isRequestingPermission) {
            try {
                @Suppress("DEPRECATION")
                sendBroadcast(Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS))
            } catch (e: Exception) {
                // Android 12+ restricts this broadcast
                e.printStackTrace()
            }
            
            handler.postDelayed({
                if (!isRequestingPermission) {
                    bringToFront()
                    hideSystemUI()
                }
            }, 50)
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        bringToFront()
    }

    override fun onPause() {
        super.onPause()
        if (!isRequestingPermission) {
            bringToFront()
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        return when (event.keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_HOME, KeyEvent.KEYCODE_APP_SWITCH,
            KeyEvent.KEYCODE_MENU -> true
            else -> super.dispatchKeyEvent(event)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}
