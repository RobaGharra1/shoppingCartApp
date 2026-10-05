package com.example.shoppingcartapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class AddressActivity : AppCompatActivity() {


    private lateinit var nameEditText: EditText
    private lateinit var surnameEditText: EditText
    private lateinit var phoneEditText: EditText
    private lateinit var addressEditText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.address_activity)


        val backArrow: ImageView = findViewById(R.id.backArrow)
        backArrow.setOnClickListener {
            onBackPressed()
        }


        nameEditText = findViewById(R.id.editTextName)
        surnameEditText = findViewById(R.id.editTextSurname)
        phoneEditText = findViewById(R.id.editTextPhone)
        addressEditText = findViewById(R.id.editTextAddress)


        val checkoutButton: Button = findViewById(R.id.buttonCheckout)
        checkoutButton.setOnClickListener {

            val name = nameEditText.text.toString()
            val surname = surnameEditText.text.toString()
            val phone = phoneEditText.text.toString()
            val address = addressEditText.text.toString()


            if (name.isEmpty() || surname.isEmpty() || phone.isEmpty() || address.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }


            val db = FirebaseFirestore.getInstance()
            db.collection("cart").get()
                .addOnSuccessListener { result ->
                    val cartItems = mutableListOf<CartItem>()
                    var totalPrice = 0


                    for (doc in result) {
                        val item = CartItem(
                            id = doc.id,
                            name = doc.getString("name") ?: "",
                            price = doc.getDouble("price")?.toInt() ?: 0,
                            quantity = doc.getLong("quantity")?.toInt() ?: 1
                        )
                        cartItems.add(item)
                        totalPrice += item.price * item.quantity
                    }


                    val orderData = hashMapOf(
                        "name" to name,
                        "surname" to surname,
                        "phone" to phone,
                        "address" to address,
                        "cart" to cartItems.map { item ->
                            hashMapOf(
                                "name" to item.name,
                                "quantity" to item.quantity,
                                "price" to item.price
                            )
                        },
                        "totalPrice" to totalPrice,
                        "timestamp" to com.google.firebase.Timestamp.now()
                    )


                    db.collection("orders").add(orderData)
                        .addOnSuccessListener {

                            Toast.makeText(this, "Order placed successfully", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this, CheckOut::class.java)
                            startActivity(intent)
                            finish()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Error placing order: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to load cart: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
