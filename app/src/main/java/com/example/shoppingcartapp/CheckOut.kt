package com.example.shoppingcartapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class CheckOut : AppCompatActivity() {

    private lateinit var returnHomeButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_checkout)


        returnHomeButton = findViewById(R.id.returnHome)


        returnHomeButton.setOnClickListener {

            val db = FirebaseFirestore.getInstance()
            db.collection("cart").get()
                .addOnSuccessListener { result ->
                    for (document in result) {
                        db.collection("cart").document(document.id).delete()
                    }
                    Toast.makeText(this, "Cart cleared! Returning home.", Toast.LENGTH_SHORT).show()


                    val intent = Intent(this, HomeActivity::class.java)
                    intent.putExtra("resetQuantities", true)
                    startActivity(intent)


                    finish()
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(this, "Failed to clear cart: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
