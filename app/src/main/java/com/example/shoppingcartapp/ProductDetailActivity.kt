package com.example.shoppingcartapp

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProductDetailActivity : AppCompatActivity() {

    private class Option(val label: String, val extra: Int)

    private var basePrice = 0
    private var quantity = 1
    private var sizeIdx = 0
    private var milkIdx = 0
    private var shotIdx = 0

    private val sizes = listOf(Option("Small", 0), Option("Medium", 2), Option("Large", 4))
    private val milks = listOf(
        Option("Regular", 0), Option("Lactose-free", 0),
        Option("Oat", 2), Option("Almond", 2)
    )
    private val shots = listOf(Option("1 shot", 0), Option("2 shots", 3), Option("3 shots", 6))

    private lateinit var addButton: MaterialButton
    private lateinit var qtyText: TextView
    private var isDrink = false
    private var hasSize = false
    private var hasMilk = false
    private var hasShots = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product_detail)

        window.statusBarColor = Color.TRANSPARENT
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false

        val name = intent.getStringExtra("name") ?: ""
        basePrice = intent.getIntExtra("price", 0)
        val description = intent.getStringExtra("description") ?: ""
        val category = intent.getStringExtra("category") ?: ""
        val imageRes = intent.getIntExtra("image", R.drawable.ic_launcher_background)

        isDrink = category == "Hot Drinks" || category == "Cold Drinks"

        val coffeeBased = listOf(
            "Espresso", "Americano", "Cappuccino", "Latte", "Mocha",
            "Iced Americano", "Iced Latte", "Frappuccino"
        )
        val noMilk = listOf("Espresso", "Americano", "Iced Americano")
        hasShots = name in coffeeBased
        hasMilk = (name in coffeeBased && name !in noMilk) || name == "Hot Chocolate"
        hasSize = isDrink && name != "Espresso"

        findViewById<ImageView>(R.id.detailImage).setImageResource(imageRes)
        findViewById<TextView>(R.id.detailName).text = name
        findViewById<TextView>(R.id.detailDescription).text = description
        findViewById<ImageView>(R.id.detailBack).setOnClickListener { finish() }

        addButton = findViewById(R.id.addToCartDetail)
        qtyText = findViewById(R.id.qtyText)

        val container = findViewById<LinearLayout>(R.id.optionsContainer)
        if (isDrink) {
            if (hasSize) addOptionGroup(container, "Size", sizes, 0) { sizeIdx = it }
            if (hasMilk) addOptionGroup(container, "Milk", milks, 0) { milkIdx = it }
            if (hasShots) addOptionGroup(container, "Espresso shots", shots, 0) { shotIdx = it }
        }

        findViewById<MaterialButton>(R.id.qtyMinus).setOnClickListener {
            if (quantity > 1) { quantity--; refresh() }
        }
        findViewById<MaterialButton>(R.id.qtyPlus).setOnClickListener {
            if (quantity < 10) { quantity++; refresh() }
        }

        addButton.setOnClickListener { addToCart(name, category, imageRes) }
        refresh()
    }

    private fun unitPrice(): Int {
        var p = basePrice
        if (hasSize) p += sizes[sizeIdx].extra
        if (hasMilk) p += milks[milkIdx].extra
        if (hasShots) p += shots[shotIdx].extra
        return p
    }

    private fun refresh() {
        qtyText.text = quantity.toString()
        addButton.text = "Add to Cart  |  ₪${unitPrice() * quantity}"
    }

    private fun optionsSummary(): String {
        val parts = mutableListOf<String>()
        if (hasSize) parts.add(sizes[sizeIdx].label)
        if (hasMilk) parts.add(milks[milkIdx].label)
        if (hasShots) parts.add(shots[shotIdx].label)
        return parts.joinToString(" • ")
    }

    private fun addOptionGroup(
        container: LinearLayout,
        title: String,
        options: List<Option>,
        selected: Int,
        onSelect: (Int) -> Unit
    ) {
        val d = resources.displayMetrics.density
        val titleView = TextView(this).apply {
            text = title
            textSize = 16f
            setTextColor(Color.parseColor("#3E2A1C"))
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, (18 * d).toInt(), 0, (8 * d).toInt())
        }
        container.addView(titleView)

        val scroll = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false }
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        scroll.addView(row)
        container.addView(scroll)

        val chips = mutableListOf<TextView>()
        fun style(sel: Int) {
            chips.forEachIndexed { i, c ->
                if (i == sel) {
                    c.setBackgroundResource(R.drawable.chip_selected)
                    c.setTextColor(Color.WHITE)
                } else {
                    c.setBackgroundResource(R.drawable.chip_unselected)
                    c.setTextColor(Color.parseColor("#8A7968"))
                }
            }
        }

        options.forEachIndexed { i, opt ->
            val chip = TextView(this).apply {
                text = if (opt.extra > 0) "${opt.label}  +₪${opt.extra}" else opt.label
                textSize = 13f
                gravity = Gravity.CENTER
                setPadding((16 * d).toInt(), (10 * d).toInt(), (16 * d).toInt(), (10 * d).toInt())
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { marginEnd = (8 * d).toInt() }
                setOnClickListener {
                    onSelect(i)
                    style(i)
                    refresh()
                }
            }
            chips.add(chip)
            row.addView(chip)
        }
        style(selected)
    }

    private fun addToCart(name: String, category: String, imageRes: Int) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val options = optionsSummary()
        val cartRef = FirebaseFirestore.getInstance()
            .collection("users").document(uid).collection("cart")

        addButton.isEnabled = false

        // نبحث عن نفس المنتج بنفس الخيارات بالضبط
        cartRef.whereEqualTo("name", name)
            .whereEqualTo("options", options)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    // منتج جديد أو بخيارات مختلفة: سطر جديد
                    val data = hashMapOf(
                        "name" to name,
                        "options" to options,
                        "price" to unitPrice(),
                        "quantity" to quantity,
                        "category" to category,
                        "image" to resources.getResourceEntryName(imageRes)
                    )
                    cartRef.add(data)
                        .addOnSuccessListener { onAdded(name) }
                        .addOnFailureListener { onAddFailed(it.message) }
                } else {
                    // نفس المنتج بنفس الخيارات: نزيد الكمية
                    val doc = snapshot.documents.first()
                    val current = doc.getLong("quantity")?.toInt() ?: 0
                    val newQuantity = minOf(current + quantity, 10)
                    cartRef.document(doc.id).update("quantity", newQuantity)
                        .addOnSuccessListener { onAdded(name) }
                        .addOnFailureListener { onAddFailed(it.message) }
                }
            }
            .addOnFailureListener { onAddFailed(it.message) }
    }

    private fun onAdded(name: String) {
        Toast.makeText(this, "$name added to cart", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun onAddFailed(message: String?) {
        addButton.isEnabled = true
        Toast.makeText(this, "Failed: $message", Toast.LENGTH_LONG).show()
    }
}