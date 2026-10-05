package com.example.shoppingcartapp

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore

class CartActivity : AppCompatActivity() {

    private val deliveryFee = 5
    private val cartItems = mutableListOf<CartItem>()
    private lateinit var cartAdapter: CartAdapter
    private lateinit var cartRef: CollectionReference

    private lateinit var subtotalText: TextView
    private lateinit var deliveryText: TextView
    private lateinit var totalText: TextView
    private lateinit var emptyText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cart)

        window.statusBarColor = Color.parseColor("#FBF3EA")
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            finish()
            return
        }
        cartRef = FirebaseFirestore.getInstance().collection("users").document(uid).collection("cart")

        subtotalText = findViewById(R.id.subtotalText)
        deliveryText = findViewById(R.id.deliveryText)
        totalText = findViewById(R.id.totalPrice)
        emptyText = findViewById(R.id.emptyText)

        findViewById<ImageView>(R.id.backArrow).setOnClickListener { finish() }

        val recyclerView = findViewById<RecyclerView>(R.id.cartRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        cartAdapter = CartAdapter(cartItems, { documentId, _, newQuantity ->
            cartRef.document(documentId).update("quantity", newQuantity)
                .addOnSuccessListener {
                    val index = cartItems.indexOfFirst { it.id == documentId }
                    if (index != -1) {
                        cartItems[index].quantity = newQuantity
                        cartAdapter.notifyItemChanged(index)
                        updateTotals()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }, { _, _ -> })
        recyclerView.adapter = cartAdapter

        // سحب لليسار للحذف
        val swipe = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(
                rv: RecyclerView, vh: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                if (position == RecyclerView.NO_POSITION) return
                val item = cartItems[position]
                cartRef.document(item.id).delete()
                    .addOnSuccessListener {
                        cartItems.removeAt(position)
                        cartAdapter.notifyItemRemoved(position)
                        updateTotals()
                    }
                    .addOnFailureListener { e ->
                        cartAdapter.notifyItemChanged(position)
                        Toast.makeText(this@CartActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }
        ItemTouchHelper(swipe).attachToRecyclerView(recyclerView)

        findViewById<MaterialButton>(R.id.checkoutButton).setOnClickListener {
            if (cartItems.isEmpty()) {
                Toast.makeText(this, "Your cart is empty", Toast.LENGTH_SHORT).show()
            } else {
                startActivity(Intent(this, AddressActivity::class.java))
            }
        }

        loadCart()
    }

    private fun loadCart() {
        cartRef.get()
            .addOnSuccessListener { result ->
                cartItems.clear()
                for (doc in result) {
                    cartItems.add(
                        CartItem(
                            id = doc.id,
                            name = doc.getString("name") ?: "",
                            price = doc.getLong("price")?.toInt() ?: 0,
                            quantity = doc.getLong("quantity")?.toInt() ?: 1,
                            options = doc.getString("options") ?: "",
                            image = doc.getString("image") ?: ""
                        )
                    )
                }
                cartAdapter.notifyDataSetChanged()
                updateTotals()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to load cart: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateTotals() {
        val subtotal = cartItems.sumOf { it.price * it.quantity }
        val delivery = if (cartItems.isEmpty()) 0 else deliveryFee
        subtotalText.text = "₪$subtotal"
        deliveryText.text = "₪$delivery"
        totalText.text = "₪${subtotal + delivery}"
        emptyText.visibility = if (cartItems.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
    }
}