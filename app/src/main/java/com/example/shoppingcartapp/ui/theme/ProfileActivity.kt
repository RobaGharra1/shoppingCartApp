package com.example.shoppingcartapp

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var uid: String
    private lateinit var addressList: LinearLayout
    private lateinit var emptyAddresses: TextView

    private fun String.cap() = replaceFirstChar { it.uppercase() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        window.statusBarColor = Color.parseColor("#FBF3EA")
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true

        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) { finish(); return }
        uid = user.uid

        addressList = findViewById(R.id.addressList)
        emptyAddresses = findViewById(R.id.emptyAddresses)

        findViewById<ImageView>(R.id.backArrow).setOnClickListener { finish() }
        findViewById<View>(R.id.ordersRow).setOnClickListener {
            startActivity(Intent(this, OrdersActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.logoutButton).setOnClickListener { confirmLogout() }

        // معلومات فورية من الحساب، وبعدها نكمّل من Firestore
        showIdentity(user.displayName ?: "", user.email ?: "", "")
        db.collection("users").document(uid).get().addOnSuccessListener { doc ->
            showIdentity(
                doc.getString("name") ?: (user.displayName ?: ""),
                user.email ?: doc.getString("email") ?: "",
                doc.getString("phone") ?: ""
            )
        }
    }

    override fun onResume() {
        super.onResume()
        loadAddresses()
        loadOrdersCount()
    }

    private fun showIdentity(name: String, email: String, phone: String) {
        findViewById<TextView>(R.id.avatarText).text =
            name.trim().firstOrNull()?.uppercase() ?: "?"
        findViewById<TextView>(R.id.profileName).text = if (name.isEmpty()) "Guest" else name
        findViewById<TextView>(R.id.profileEmail).text = email
        val phoneView = findViewById<TextView>(R.id.profilePhone)
        phoneView.text = phone
        phoneView.visibility = if (phone.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun loadOrdersCount() {
        db.collection("orders").whereEqualTo("uid", uid).get()
            .addOnSuccessListener { findViewById<TextView>(R.id.ordersCount).text = "${it.size()}" }
    }

    private fun loadAddresses() {
        db.collection("users").document(uid).collection("addresses").get()
            .addOnSuccessListener { result ->
                addressList.removeAllViews()
                emptyAddresses.visibility = if (result.isEmpty) View.VISIBLE else View.GONE
                val d = resources.displayMetrics.density

                result.documents.sortedBy { (it.getString("label") ?: "").lowercase() }
                    .forEach { doc ->
                        val label = (doc.getString("label") ?: "").cap()
                        val line = "${(doc.getString("city") ?: "").cap()}, ${doc.getString("street") ?: ""}"

                        val row = LinearLayout(this).apply {
                            orientation = LinearLayout.HORIZONTAL
                            gravity = android.view.Gravity.CENTER_VERTICAL
                            setBackgroundResource(R.drawable.option_unselected)
                            setPadding((16 * d).toInt(), (14 * d).toInt(), (8 * d).toInt(), (14 * d).toInt())
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply { bottomMargin = (10 * d).toInt() }
                        }
                        val texts = LinearLayout(this).apply {
                            orientation = LinearLayout.VERTICAL
                            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                        }
                        texts.addView(TextView(this).apply {
                            text = label
                            textSize = 15f
                            setTypeface(typeface, Typeface.BOLD)
                            setTextColor(Color.parseColor("#2B1D14"))
                        })
                        texts.addView(TextView(this).apply {
                            text = line
                            textSize = 13f
                            setTextColor(Color.parseColor("#8A7968"))
                        })
                        val delete = TextView(this).apply {
                            text = "Delete"
                            textSize = 13f
                            setTypeface(typeface, Typeface.BOLD)
                            setTextColor(Color.parseColor("#6F4E37"))
                            setPadding((12 * d).toInt(), (8 * d).toInt(), (12 * d).toInt(), (8 * d).toInt())
                            setOnClickListener { confirmDelete(doc.id, label) }
                        }
                        row.addView(texts)
                        row.addView(delete)
                        addressList.addView(row)
                    }
            }
    }

    private fun confirmDelete(id: String, label: String) {
        AlertDialog.Builder(this)
            .setTitle("Delete \"$label\"?")
            .setPositiveButton("Delete") { _, _ ->
                db.collection("users").document(uid).collection("addresses").document(id)
                    .delete().addOnSuccessListener { loadAddresses() }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmLogout() {
        AlertDialog.Builder(this)
            .setTitle("Log out?")
            .setMessage("You will need to sign in again.")
            .setPositiveButton("Log out") { _, _ ->
                getSharedPreferences("app_prefs", MODE_PRIVATE)
                    .edit().putBoolean("remember_me", false).apply()
                FirebaseAuth.getInstance().signOut()
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}