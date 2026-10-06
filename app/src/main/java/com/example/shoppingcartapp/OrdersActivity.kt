package com.example.shoppingcartapp

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class OrdersActivity : AppCompatActivity() {

    private val rows = mutableListOf<OrderRow>()
    private lateinit var adapter: OrdersAdapter
    private lateinit var emptyText: TextView
    private var listener: ListenerRegistration? = null
    private lateinit var uid: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_orders)

        window.statusBarColor = Color.parseColor("#FBF3EA")
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true

        uid = FirebaseAuth.getInstance().currentUser?.uid ?: run { finish(); return }

        emptyText = findViewById(R.id.emptyOrders)
        findViewById<ImageView>(R.id.backArrow).setOnClickListener { finish() }

        val recycler = findViewById<RecyclerView>(R.id.ordersRecycler)
        recycler.layoutManager = LinearLayoutManager(this)
        adapter = OrdersAdapter(rows) { row ->
            startActivity(Intent(this, OrderDetailActivity::class.java).putExtra("orderId", row.id))
        }
        recycler.adapter = adapter
    }

    override fun onStart() {
        super.onStart()
        // تحديث لحظي: لما تتغيّر الحالة بالكونسول بتتغيّر هون لحالها
        listener = FirebaseFirestore.getInstance().collection("orders")
            .whereEqualTo("uid", uid)
            .addSnapshotListener { snap, _ ->
                if (snap == null) return@addSnapshotListener
                rows.clear()
                for (doc in snap.documents) {
                    val items = (doc.get("items") as? List<*>)
                        ?.mapNotNull { it as? Map<*, *> } ?: emptyList()
                    val summary = items.joinToString(", ") {
                        "${it["quantity"] ?: 1}x ${it["name"] ?: ""}"
                    }
                    rows.add(
                        OrderRow(
                            id = doc.id,
                            items = summary,
                            total = doc.getLong("total")?.toInt() ?: 0,
                            status = doc.getString("status") ?: "placed",
                            time = doc.getTimestamp("createdAt")?.toDate()?.time
                                ?: System.currentTimeMillis()
                        )
                    )
                }
                rows.sortByDescending { it.time }
                adapter.notifyDataSetChanged()
                emptyText.visibility = if (rows.isEmpty()) View.VISIBLE else View.GONE
            }
    }

    override fun onStop() {
        super.onStop()
        listener?.remove()
    }
}