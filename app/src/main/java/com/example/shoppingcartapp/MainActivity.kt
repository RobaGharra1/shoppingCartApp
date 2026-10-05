package com.example.shoppingcartapp

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var emailEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var signUpText: TextView
    private lateinit var forgotPasswordText: TextView
    private lateinit var rememberMeCheckBox: MaterialCheckBox
    private lateinit var loginButton: MaterialButton
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)

        // دخول تلقائي فقط لو كان Remember me مفعّل
        if (auth.currentUser != null) {
            if (prefs.getBoolean("remember_me", false)) {
                navigateToHome()
                return
            } else {
                auth.signOut()
            }
        }

        setContentView(R.layout.activity_main)

        val hero = findViewById<android.widget.ImageView>(R.id.heroImage)
        hero.post {
            val d = hero.drawable ?: return@post
            val scale = hero.width.toFloat() / d.intrinsicWidth
            val matrix = android.graphics.Matrix()
            matrix.setScale(scale, scale)
            // 0.0 = أعلى الصورة، 1.0 = أسفلها. الوضع الحالي ~0.5، فنزيده لنقص من فوق
            val focus = 0.8f
            val overflow = d.intrinsicHeight * scale - hero.height
            matrix.postTranslate(0f, -overflow * focus)
            hero.imageMatrix = matrix
        }

        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        androidx.core.view.WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false

        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        signUpText = findViewById(R.id.signUpText)
        forgotPasswordText = findViewById(R.id.forgotPasswordText)
        rememberMeCheckBox = findViewById(R.id.rememberMeCheckBox)
        loginButton = findViewById(R.id.loginButton)

        signUpText.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }

        forgotPasswordText.setOnClickListener {
            val email = emailEditText.text.toString().trim()

            if (email.isEmpty()) {
                Toast.makeText(this, "Enter your email first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Reset link sent to your email", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this, "Failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }

        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        prefs.edit().putBoolean("remember_me", rememberMeCheckBox.isChecked).apply()
                        Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show()
                        navigateToHome()
                    } else {
                        Toast.makeText(this, "Login Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    private fun navigateToHome() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }
}