package com.example.shoppingcartapp

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth

class WelcomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        if (FirebaseAuth.getInstance().currentUser != null &&
            prefs.getBoolean("remember_me", false)) {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_welcome)

        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false

        // تعديل مكان صورة الـ Hero
        val hero = findViewById<android.widget.ImageView>(R.id.heroImage)
        hero.post {
            val d = hero.drawable ?: return@post

            val scale = maxOf(
                hero.width.toFloat() / d.intrinsicWidth,
                hero.height.toFloat() / d.intrinsicHeight
            )

            val matrix = android.graphics.Matrix()
            matrix.setScale(scale, scale)

            val dx = (hero.width - d.intrinsicWidth * scale) / 2f

            val focus = 0.8f

            val dy = -(d.intrinsicHeight * scale - hero.height) * focus

            matrix.postTranslate(dx, dy)
            hero.imageMatrix = matrix
        }

        findViewById<MaterialButton>(R.id.getStartedButton).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}