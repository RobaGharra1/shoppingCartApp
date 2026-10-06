package com.example.shoppingcartapp

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class HomeActivity : AppCompatActivity() {

    private lateinit var productAdapter: ProductAdapter
    private var productList: List<Product> = listOf()
    private val db = FirebaseFirestore.getInstance()

    private val categories = listOf(
        "All" to "🍽️",
        "Hot Drinks" to "☕",
        "Cold Drinks" to "🧊",
        "Pastries" to "🥐",
        "Desserts" to "🍰"
    )
    private var selectedCategory = "All"
    private var currentQuery = ""
    private val chipViews = mutableListOf<TextView>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // شريط الحالة كريمي
        window.statusBarColor = Color.parseColor("#FBF3EA")
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true

        val recyclerView = findViewById<RecyclerView>(R.id.productRecyclerView)
        recyclerView.layoutManager = GridLayoutManager(this, 2)

        val searchView = findViewById<SearchView>(R.id.searchView)

        productAdapter = ProductAdapter(productList) { product, quantity ->
            addToCart(product, quantity)
        }
        recyclerView.adapter = productAdapter

        setupCategories()
        loadProducts()

        // الشريط السفلي
        findViewById<LinearLayout>(R.id.navCart).setOnClickListener {
            startActivity(Intent(this, CartActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.navOrders).setOnClickListener {
            startActivity(Intent(this, OrdersActivity::class.java))
        }

        findViewById<ImageButton>(R.id.btnLogout).setOnClickListener {
            getSharedPreferences("app_prefs", MODE_PRIVATE).edit().putBoolean("remember_me", false).apply()
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }


        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false

            override fun onQueryTextChange(newText: String?): Boolean {
                currentQuery = newText ?: ""
                applyFilters()
                return true
            }
        })

        // التحية
        val name = FirebaseAuth.getInstance().currentUser?.displayName
            ?: intent.getStringExtra("name")
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greeting = when {
            hour < 12 -> "Good Morning"
            hour < 18 -> "Good Afternoon"
            else -> "Good Evening"
        }
        findViewById<TextView>(R.id.greetingText).text =
            if (name.isNullOrEmpty()) greeting else "$greeting,\n$name"
    }

    private fun setupCategories() {
        val container = findViewById<LinearLayout>(R.id.categoryContainer)
        val density = resources.displayMetrics.density

        categories.forEach { (category, emoji) ->
            val chip = TextView(this).apply {
                text = "$emoji\n$category"
                tag = category
                textSize = 12f
                gravity = Gravity.CENTER
                setPadding(
                    (14 * density).toInt(), (10 * density).toInt(),
                    (14 * density).toInt(), (10 * density).toInt()
                )
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { marginEnd = (10 * density).toInt() }
                setOnClickListener {
                    selectedCategory = category
                    updateChipStyles()
                    applyFilters()
                }
            }
            chipViews.add(chip)
            container.addView(chip)
        }
        updateChipStyles()
    }

    private fun updateChipStyles() {
        chipViews.forEach { chip ->
            if (chip.tag == selectedCategory) {
                chip.setBackgroundResource(R.drawable.chip_selected)
                chip.setTextColor(0xFFFFFFFF.toInt())
            } else {
                chip.setBackgroundResource(R.drawable.chip_unselected)
                chip.setTextColor(0xFF8A7968.toInt())
            }
        }
    }

    private fun loadProducts() {
        db.collection("products").get()
            .addOnSuccessListener { result ->
                productList = result.map { doc ->
                    val imageName = doc.getString("image") ?: "ic_launcher_background"
                    Product(
                        name = doc.getString("name") ?: "",
                        price = doc.getDouble("price")?.toInt() ?: 0,
                        inStock = doc.getString("inStock") ?: "Out of Stock",
                        imageResIds = listOf(getImageResourceId(imageName)),
                        category = doc.getString("category") ?: "",
                        description = doc.getString("description") ?: ""
                    )
                }
                applyFilters()

                if (productList.isEmpty()) {
                    Toast.makeText(this, "No products found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Failed to load products: ${exception.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun applyFilters() {
        val filtered = productList.filter { product ->
            val matchesCategory = selectedCategory == "All" || product.category == selectedCategory
            val matchesQuery = currentQuery.isEmpty() || product.name.contains(currentQuery, ignoreCase = true)
            matchesCategory && matchesQuery
        }
        productAdapter.updateList(filtered)
    }

    private fun addToCart(product: Product, quantity: Int) {
        val cartRef = db.collection("cart")
        cartRef.whereEqualTo("name", product.name).get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) {
                    cartRef.add(product.copy(quantity = quantity))
                        .addOnSuccessListener {
                            Toast.makeText(this, "${product.name} added to cart", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Failed to add to cart: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    // الضغط على + مرة ثانية يزيد الكمية بدل ما يعيّنها
                    val document = querySnapshot.documents.first()
                    val current = document.getLong("quantity")?.toInt() ?: 0
                    val newQuantity = current + quantity
                    cartRef.document(document.id).update("quantity", newQuantity)
                        .addOnSuccessListener {
                            Toast.makeText(this, "${product.name} quantity: $newQuantity", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Failed to update quantity: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to check cart: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun getImageResourceId(imageName: String): Int {
        val cleanName = imageName.substringBefore(".")
        return resources.getIdentifier(cleanName, "drawable", packageName).takeIf { it != 0 }
            ?: R.drawable.ic_launcher_background
    }
}