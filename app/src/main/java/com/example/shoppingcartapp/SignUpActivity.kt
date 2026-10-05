package com.example.shoppingcartapp

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore

class SignUpActivity : AppCompatActivity() {

    private lateinit var nameLayout: TextInputLayout
    private lateinit var phoneLayout: TextInputLayout
    private lateinit var emailLayout: TextInputLayout
    private lateinit var passwordLayout: TextInputLayout
    private lateinit var retypePasswordLayout: TextInputLayout

    private lateinit var nameEditText: TextInputEditText
    private lateinit var phoneEditText: TextInputEditText
    private lateinit var emailEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var retypePasswordEditText: TextInputEditText

    private lateinit var signUpButton: MaterialButton
    private lateinit var backArrow: ImageView
    private lateinit var loginText: TextView
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.sign_up_activity)

        val hero = findViewById<android.widget.ImageView>(R.id.heroImage)
        hero.post {
            val d = hero.drawable ?: return@post
            val scale = hero.width.toFloat() / d.intrinsicWidth
            val matrix = android.graphics.Matrix()
            matrix.setScale(scale, scale)
            val focus = 0.8f
            val overflow = d.intrinsicHeight * scale - hero.height
            matrix.postTranslate(0f, -overflow * focus)
            hero.imageMatrix = matrix
        }

        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        androidx.core.view.WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false

        auth = FirebaseAuth.getInstance()

        nameLayout = findViewById(R.id.nameLayout)
        phoneLayout = findViewById(R.id.phoneLayout)
        emailLayout = findViewById(R.id.emailLayout)
        passwordLayout = findViewById(R.id.passwordLayout)
        retypePasswordLayout = findViewById(R.id.retypePasswordLayout)

        nameEditText = findViewById(R.id.nameEditText)
        phoneEditText = findViewById(R.id.phoneEditText)
        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        retypePasswordEditText = findViewById(R.id.retypePasswordEditText)

        signUpButton = findViewById(R.id.signUpButton)
        backArrow = findViewById(R.id.backArrow)
        loginText = findViewById(R.id.loginText)

        backArrow.setOnClickListener { finish() }
        loginText.setOnClickListener { finish() }
        signUpButton.setOnClickListener { attemptSignUp() }
    }

    private fun attemptSignUp() {
        val name = nameEditText.text.toString().trim()
        val phone = phoneEditText.text.toString().filter { it.isDigit() }
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()
        val retypePassword = retypePasswordEditText.text.toString().trim()

        nameLayout.error = null
        phoneLayout.error = null
        emailLayout.error = null
        passwordLayout.error = null
        retypePasswordLayout.error = null

        var valid = true

        if (name.isEmpty()) {
            nameLayout.error = "Enter your name"
            valid = false
        }
        if (phone.length < 9) {
            phoneLayout.error = "Enter a valid phone number"
            valid = false
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.error = "Enter a valid email"
            valid = false
        }
        if (password.length < 6) {
            passwordLayout.error = "Password must be at least 6 characters"
            valid = false
        }
        if (retypePassword != password) {
            retypePasswordLayout.error = "Passwords do not match"
            valid = false
        }
        if (!valid) return

        signUpButton.isEnabled = false

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser!!
                    val db = FirebaseFirestore.getInstance()
                    val phoneRef = db.collection("phones").document(phone)

                    // نحجز الرقم: إذا محجوز من حساب ثاني بنرفض
                    db.runTransaction { tx ->
                        if (tx.get(phoneRef).exists()) {
                            throw Exception("PHONE_TAKEN")
                        }
                        tx.set(phoneRef, mapOf("uid" to user.uid))
                    }.addOnSuccessListener {
                        user.updateProfile(
                            UserProfileChangeRequest.Builder().setDisplayName(name).build()
                        )
                        db.collection("users").document(user.uid)
                            .set(mapOf("name" to name, "phone" to phone, "email" to email))
                        FirebaseDatabase.getInstance().getReference("Users").child(user.uid)
                            .setValue(mapOf("name" to name, "email" to email))

                        getSharedPreferences("app_prefs", MODE_PRIVATE)
                            .edit().putBoolean("remember_me", true).apply()

                        Toast.makeText(this, "Welcome, $name!", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, HomeActivity::class.java)
                        intent.putExtra("name", name)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                    }.addOnFailureListener { e ->
                        // الرقم محجوز أو فشل: نحذف الحساب اللي انعمل
                        user.delete()
                        signUpButton.isEnabled = true
                        if (e.message?.contains("PHONE_TAKEN") == true) {
                            phoneLayout.error = "Phone number already in use"
                        } else {
                            Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    signUpButton.isEnabled = true
                    Toast.makeText(this, "Sign-Up Failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }
}