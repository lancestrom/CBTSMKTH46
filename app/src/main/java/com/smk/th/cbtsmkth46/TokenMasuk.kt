package com.smk.th.cbtsmkth46

import android.app.ActivityManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.Button
import android.widget.Toast
import java.net.HttpURLConnection
import java.net.URL
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.textfield.TextInputEditText

class TokenMasuk : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private var isRequestingPermission = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Security & Layout (Sama seperti WebView)
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

        enableEdgeToEdge()
        setContentView(R.layout.activity_token_masuk)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val etToken = findViewById<TextInputEditText>(R.id.etToken)
        val btnMasuk = findViewById<Button>(R.id.btnMasuk)

        btnMasuk.setOnClickListener {
            val token = etToken.text.toString().trim()
            if (token.isNotEmpty()) {
                checkToken(token)
            } else {
                Toast.makeText(this, getString(R.string.empty_token_error), Toast.LENGTH_SHORT).show()
            }
        }

        // 2. Disable Back (Sama seperti WebView)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Toast.makeText(this@TokenMasuk, "Aplikasi Terkunci!", Toast.LENGTH_SHORT).show()
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

    override fun onResume() {
        super.onResume()
        isRequestingPermission = false
        hideSystemUI()

        // Cek hanya izin Overlay (Appear on Top)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                forceOpenOverlaySettings()
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

    private fun checkToken(inputToken: String) {
        val btnMasuk = findViewById<Button>(R.id.btnMasuk)
        btnMasuk.isEnabled = false
        btnMasuk.text = "Mengecek..."

        Thread {
            try {
                val url = URL("http://103.103.20.187/token/api/check_token")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val response = connection.inputStream.bufferedReader().use { it.readText() }.trim()

                runOnUiThread {
                    btnMasuk.isEnabled = true
                    btnMasuk.text = getString(R.string.btn_masuk)

                    // Memeriksa apakah input sama dengan response dari API
                    if (response.equals(inputToken, ignoreCase = true) || response.contains(inputToken, ignoreCase = true)) {
                        val intent = Intent(this, WebView::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this, "Token Salah! Silakan coba lagi.", Toast.LENGTH_LONG).show()
                        findViewById<TextInputEditText>(R.id.etToken).text?.clear()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    btnMasuk.isEnabled = true
                    btnMasuk.text = getString(R.string.btn_masuk)
                    Toast.makeText(this, "Koneksi Gagal: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun bringToFront() {
        if (isRequestingPermission) return
        try {
            val activityManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
            activityManager.moveTaskToFront(taskId, ActivityManager.MOVE_TASK_NO_USER_ACTION)
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