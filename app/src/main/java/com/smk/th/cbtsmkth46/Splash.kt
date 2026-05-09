package com.smk.th.cbtsmkth46

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class Splash : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Buat Splash Fullscreen Total
        WindowCompat.setDecorFitsSystemWindows(window, false)
        hideSystemUI()
        
        setContentView(R.layout.activity_splash)

        // Tunggu 5 detik lalu pindah ke WebView
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, TokenMasuk::class.java)
            startActivity(intent)
            finish()
        }, 5000)
    }

    private fun hideSystemUI() {
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    override fun onResume() {
        super.onResume()
        hideSystemUI()
    }
}
