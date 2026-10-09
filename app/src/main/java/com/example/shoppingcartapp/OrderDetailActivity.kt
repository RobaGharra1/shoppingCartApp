package com.example.shoppingcartapp

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsControllerCompat
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class OrderDetailActivity : AppCompatActivity() {

    private var listener: ListenerRegistration? = null
    private var orderId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order_detail)

        window.statusBarColor = Color.parseColor("#FBF3EA")
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true

        orderId = intent.getStringExtra("orderId") ?: run { finish(); return }
        findViewById<TextView>(R.id.detailTitle).text = "Order #" + orderId.take(6).uppercase()
        findViewById<ImageView>(R.id.backArrow).setOnClickListener { finish() }
    }

    override fun onStart() {
        super.onStart()
        listener = FirebaseFirestore.getInstance().collection("orders").document(orderId)
            .addSnapshotListener { doc, _ ->
                if (doc != null && doc.exists()) render(doc)
            }
    }

    override fun onStop() {
        super.onStop()
        listener?.remove()
    }

    private fun render(doc: DocumentSnapshot) {
        val status = doc.getString("status") ?: "placed"
        val deliveryType = doc.getString("deliveryType") ?: "Standard"

        findViewById<TextView>(R.id.statusLabel).text = OrderStatus.label(status)
        findViewById<TextView>(R.id.statusSub).text = when (status) {
            "delivered" -> "Delivered. Enjoy your coffee!"
            "on_the_way" -> "Your driver is on the way"
            else -> "Estimated delivery: " +
                    if (deliveryType == "Express") "15-25 min" else "35-45 min"
        }

        renderStepper(OrderStatus.index(status))
        renderItems(doc)

        val addr = doc.get("address") as? Map<*, *>
        if (addr != null) {
            val notes = (addr["notes"] as? String).orEmpty()
            findViewById<TextView>(R.id.addressText).text =
                "${addr["label"] ?: ""}\n${addr["city"] ?: ""}, ${addr["street"] ?: ""}" +
                        if (notes.isNotEmpty()) "\nNote: $notes" else ""
        }

        val pay = doc.get("payment") as? Map<*, *>
        findViewById<TextView>(R.id.paymentText).text =
            if (pay?.get("method") == "card") "Paid by ${pay["brand"]} •••• ${pay["last4"]}"
            else "Cash on delivery"

        findViewById<TextView>(R.id.dSubtotal).text = "₪${doc.getLong("subtotal") ?: 0}"
        findViewById<TextView>(R.id.dDelivery).text = "₪${doc.getLong("deliveryFee") ?: 0}"
        findViewById<TextView>(R.id.dTotal).text = "₪${doc.getLong("total") ?: 0}"
    }

    private fun renderStepper(current: Int) {
        val stepper = findViewById<LinearLayout>(R.id.stepper)
        stepper.removeAllViews()
        val d = resources.displayMetrics.density
        val labels = listOf("Placed", "Preparing", "On the way", "Delivered")

        labels.forEachIndexed { i, label ->
            val done = i <= current
            val col = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            col.addView(TextView(this).apply {
                text = if (done) "✓" else (i + 1).toString()
                gravity = Gravity.CENTER
                textSize = 13f
                setTypeface(typeface, Typeface.BOLD)
                setBackgroundResource(if (done) R.drawable.step_inactive else R.drawable.step_active)
                // الخطوات المنتهية كريمية على الخلفية الغامقة، والباقية بنية
                background.mutate().setTint(
                    Color.parseColor(if (done) "#EADBC8" else "#6F4E37")
                )
                setTextColor(Color.parseColor(if (done) "#3E2A1C" else "#EADBC8"))
                layoutParams = LinearLayout.LayoutParams((30 * d).toInt(), (30 * d).toInt())
            })
            col.addView(TextView(this).apply {
                text = label
                textSize = 11f
                gravity = Gravity.CENTER
                setTextColor(Color.parseColor(if (done) "#FFFFFF" else "#B9A590"))
                setPadding(0, (6 * d).toInt(), 0, 0)
            })
            stepper.addView(col)
        }
    }

    private fun renderItems(doc: DocumentSnapshot) {
        val container = findViewById<LinearLayout>(R.id.itemsContainer)
        container.removeAllViews()
        val d = resources.displayMetrics.density
        val items = (doc.get("items") as? List<*>)?.mapNotNull { it as? Map<*, *> } ?: emptyList()

        items.forEachIndexed { index, item ->
            val qty = (item["quantity"] as? Number)?.toInt() ?: 1
            val price = (item["price"] as? Number)?.toInt() ?: 0
            val options = (item["options"] as? String).orEmpty()

            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                if (index > 0) setPadding(0, (12 * d).toInt(), 0, 0)
            }
            val left = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            left.addView(TextView(this).apply {
                text = "${qty}x ${item["name"] ?: ""}"
                textSize = 15f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(Color.parseColor("#2B1D14"))
            })
            if (options.isNotEmpty()) {
                left.addView(TextView(this).apply {
                    text = options
                    textSize = 12f
                    setTextColor(Color.parseColor("#8A7968"))
                })
            }
            row.addView(left)
            row.addView(TextView(this).apply {
                text = "₪${price * qty}"
                textSize = 15f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(Color.parseColor("#6F4E37"))
            })
            container.addView(row)
        }
    }
}