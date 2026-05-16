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
import android.widget.EditText
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class TokenMasuk : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private var isRequestingPermission = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Security & Layout (Same as WebView.kt)
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
        
        setContentView(R.layout.activity_token_masuk)

        val etToken = findViewById<EditText>(R.id.et_token)
        val btnSubmit = findViewById<Button>(R.id.btn_submit)

        btnSubmit.setOnClickListener {
            val token = etToken.text.toString().trim()
            if (token.isEmpty()) {
                Toast.makeText(this, getString(R.string.empty_token_error), Toast.LENGTH_SHORT).show()
            } else {
                checkToken(token)
            }
        }

        // 2. Disable Back
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

    private fun checkToken(token: String) {
        val url = "http://project.cbt.smkth-jakbar.com/token/api/check_token_masuk"
        val queue = Volley.newRequestQueue(this)

        val stringRequest = object : StringRequest(Request.Method.POST, url,
            { response ->
                try {
                    val jsonResponse = JSONObject(response)
                    val status = jsonResponse.optString("status")
                    if (status == "true" || status == "success") {
                        Toast.makeText(this, "Token Berhasil Diverifikasi", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, WebView::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        val message = jsonResponse.optString("message", "Token Salah!")
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    if (response.trim() == "true") {
                        Toast.makeText(this, "Token Berhasil Diverifikasi", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, WebView::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this, "Respon Server: $response", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            { error ->
                Toast.makeText(this, "Gagal terhubung ke server: ${error.message}", Toast.LENGTH_SHORT).show()
            }) {
            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["token"] = token
                return params
            }
        }

        queue.add(stringRequest)
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
        }

        // Start Lock Task (Screen Pinning) to block Home and Recents
        try {
            startLockTask()
        } catch (e: Exception) {
            e.printStackTrace()
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
        val keyCode = event.keyCode
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || 
            keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || 
            keyCode == KeyEvent.KEYCODE_HOME || 
            keyCode == KeyEvent.KEYCODE_APP_SWITCH) {
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}
