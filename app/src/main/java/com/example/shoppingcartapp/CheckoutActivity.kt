package com.example.shoppingcartapp

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

data class SavedAddress(
    val id: String,
    val label: String,
    val city: String,
    val street: String,
    val notes: String
)

class CheckoutActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val items = mutableListOf<CartItem>()
    private val addresses = mutableListOf<SavedAddress>()
    private var selectedAddressId: String? = null

    private lateinit var uid: String
    private var userName = ""
    private var userPhone = ""
    private var subtotal = 0
    private var isExpress = false
    private var step = 1
    private var payByCard = true
    private val deliveryFee get() = if (isExpress) 10 else 5

    private lateinit var mainScroll: ScrollView
    private lateinit var stepAddress: LinearLayout
    private lateinit var stepPayment: LinearLayout
    private lateinit var stepDot1: TextView
    private lateinit var stepDot2: TextView
    private lateinit var stepLabel1: TextView
    private lateinit var stepLabel2: TextView
    private lateinit var addressContainer: LinearLayout
    private lateinit var emptyAddressText: TextView
    private lateinit var optStandard: LinearLayout
    private lateinit var optExpress: LinearLayout
    private lateinit var tabCard: TextView
    private lateinit var tabCash: TextView
    private lateinit var cardSection: LinearLayout
    private lateinit var cashSection: LinearLayout
    private lateinit var cardBrand: TextView
    private lateinit var cardNumberPreview: TextView
    private lateinit var cardNamePreview: TextView
    private lateinit var cardExpiryPreview: TextView
    private lateinit var cardNumberLayout: TextInputLayout
    private lateinit var cardNameLayout: TextInputLayout
    private lateinit var cardExpiryLayout: TextInputLayout
    private lateinit var cardCvvLayout: TextInputLayout
    private lateinit var cardNumberInput: TextInputEditText
    private lateinit var cardNameInput: TextInputEditText
    private lateinit var cardExpiryInput: TextInputEditText
    private lateinit var cardCvvInput: TextInputEditText
    private lateinit var recapAddress: TextView
    private lateinit var actionButton: MaterialButton
    private lateinit var orderDone: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_checkout)

        window.statusBarColor = Color.parseColor("#FBF3EA")
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) { finish(); return }
        uid = currentUser.uid

        bindViews()

        findViewById<ImageView>(R.id.backArrow).setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    orderDone.visibility == View.VISIBLE -> goHome()
                    step == 2 -> showStep(1)
                    else -> finish()
                }
            }
        })

        optStandard.setOnClickListener { selectDelivery(false) }
        optExpress.setOnClickListener { selectDelivery(true) }
        tabCard.setOnClickListener { selectPayment(true) }
        tabCash.setOnClickListener { selectPayment(false) }
        findViewById<TextView>(R.id.addAddressButton).setOnClickListener { showAddAddressDialog() }
        findViewById<MaterialButton>(R.id.backHomeButton).setOnClickListener { goHome() }
        actionButton.setOnClickListener {
            if (step == 1) goToPayment() else placeOrder()
        }

        setupCardInputs()
        selectDelivery(false)
        selectPayment(true)
        showStep(1)

        loadProfile(currentUser.displayName ?: "")
        loadCart()
    }

    private fun bindViews() {
        mainScroll = findViewById(R.id.mainScroll)
        stepAddress = findViewById(R.id.stepAddress)
        stepPayment = findViewById(R.id.stepPayment)
        stepDot1 = findViewById(R.id.stepDot1)
        stepDot2 = findViewById(R.id.stepDot2)
        stepLabel1 = findViewById(R.id.stepLabel1)
        stepLabel2 = findViewById(R.id.stepLabel2)
        addressContainer = findViewById(R.id.addressContainer)
        emptyAddressText = findViewById(R.id.emptyAddressText)
        optStandard = findViewById(R.id.optStandard)
        optExpress = findViewById(R.id.optExpress)
        tabCard = findViewById(R.id.tabCard)
        tabCash = findViewById(R.id.tabCash)
        cardSection = findViewById(R.id.cardSection)
        cashSection = findViewById(R.id.cashSection)
        cardBrand = findViewById(R.id.cardBrand)
        cardNumberPreview = findViewById(R.id.cardNumberPreview)
        cardNamePreview = findViewById(R.id.cardNamePreview)
        cardExpiryPreview = findViewById(R.id.cardExpiryPreview)
        cardNumberLayout = findViewById(R.id.cardNumberLayout)
        cardNameLayout = findViewById(R.id.cardNameLayout)
        cardExpiryLayout = findViewById(R.id.cardExpiryLayout)
        cardCvvLayout = findViewById(R.id.cardCvvLayout)
        cardNumberInput = findViewById(R.id.cardNumberInput)
        cardNameInput = findViewById(R.id.cardNameInput)
        cardExpiryInput = findViewById(R.id.cardExpiryInput)
        cardCvvInput = findViewById(R.id.cardCvvInput)
        recapAddress = findViewById(R.id.recapAddress)
        actionButton = findViewById(R.id.actionButton)
        orderDone = findViewById(R.id.orderDone)
    }

    // ---------------------------------------------------------------- data

    private fun addressesRef() =
        db.collection("users").document(uid).collection("addresses")

    private fun loadProfile(fallbackName: String) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                userName = doc.getString("name") ?: fallbackName
                userPhone = doc.getString("phone") ?: ""
                findViewById<TextView>(R.id.contactName).text = userName
                findViewById<TextView>(R.id.contactPhone).text =
                    if (userPhone.isEmpty()) "No phone saved" else userPhone
                loadAddresses(doc.get("address") as? Map<*, *>)
            }
            .addOnFailureListener { loadAddresses(null) }
    }

    private fun loadAddresses(legacy: Map<*, *>?) {
        addressesRef().get().addOnSuccessListener { result ->
            // ترحيل العنوان القديم (من النسخة السابقة) مرة وحدة
            if (result.isEmpty && legacy != null) {
                val city = legacy["city"] as? String ?: ""
                val street = legacy["street"] as? String ?: ""
                if (city.isNotEmpty() && street.isNotEmpty()) {
                    addressesRef().add(
                        mapOf(
                            "label" to "Home",
                            "city" to city,
                            "street" to street,
                            "notes" to (legacy["notes"] as? String ?: ""),
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    ).addOnSuccessListener {
                        db.collection("users").document(uid).update("address", FieldValue.delete())
                        loadAddresses(null)
                    }
                    return@addOnSuccessListener
                }
            }

            addresses.clear()
            for (doc in result) {
                addresses.add(
                    SavedAddress(
                        id = doc.id,
                        label = doc.getString("label") ?: "",
                        city = doc.getString("city") ?: "",
                        street = doc.getString("street") ?: "",
                        notes = doc.getString("notes") ?: ""
                    )
                )
            }
            addresses.sortBy { it.label.lowercase() }
            if (addresses.none { it.id == selectedAddressId }) {
                selectedAddressId = addresses.firstOrNull()?.id
            }
            renderAddresses()
        }
    }

    private fun loadCart() {
        db.collection("users").document(uid).collection("cart").get()
            .addOnSuccessListener { result ->
                items.clear()
                for (doc in result) {
                    items.add(
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
                subtotal = items.sumOf { it.price * it.quantity }
                updateTotals()
            }
    }

    // ---------------------------------------------------------------- addresses UI

    private fun renderAddresses() {
        addressContainer.removeAllViews()
        emptyAddressText.visibility = if (addresses.isEmpty()) View.VISIBLE else View.GONE
        val d = resources.displayMetrics.density

        addresses.forEach { a ->
            val selected = a.id == selectedAddressId
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundResource(
                    if (selected) R.drawable.option_selected else R.drawable.option_unselected
                )
                setPadding((14 * d).toInt(), (12 * d).toInt(), (14 * d).toInt(), (12 * d).toInt())
                layoutParams = LinearLayout.LayoutParams(
                    (170 * d).toInt(), LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { marginEnd = (10 * d).toInt() }
                setOnClickListener {
                    selectedAddressId = a.id
                    renderAddresses()
                }
                setOnLongClickListener {
                    confirmDelete(a)
                    true
                }
            }
            card.addView(TextView(this).apply {
                text = a.label
                textSize = 15f
                setTextColor(Color.parseColor("#2B1D14"))
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            })
            card.addView(TextView(this).apply {
                text = "${a.city}, ${a.street}"
                textSize = 12f
                maxLines = 2
                ellipsize = TextUtils.TruncateAt.END
                setTextColor(Color.parseColor("#8A7968"))
                setPadding(0, (4 * d).toInt(), 0, 0)
            })
            addressContainer.addView(card)
        }
    }

    private fun confirmDelete(a: SavedAddress) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Delete \"${a.label}\"?")
            .setMessage("${a.city}, ${a.street}")
            .setPositiveButton("Delete") { _, _ ->
                addressesRef().document(a.id).delete()
                    .addOnSuccessListener { loadAddresses(null) }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAddAddressDialog() {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_add_address, null)
        dialog.setContentView(view)

        val labelLayout = view.findViewById<TextInputLayout>(R.id.dlgLabelLayout)
        val cityLayout = view.findViewById<TextInputLayout>(R.id.dlgCityLayout)
        val streetLayout = view.findViewById<TextInputLayout>(R.id.dlgStreetLayout)
        val labelInput = view.findViewById<TextInputEditText>(R.id.dlgLabelInput)
        val cityInput = view.findViewById<TextInputEditText>(R.id.dlgCityInput)
        val streetInput = view.findViewById<TextInputEditText>(R.id.dlgStreetInput)
        val notesInput = view.findViewById<TextInputEditText>(R.id.dlgNotesInput)
        val saveButton = view.findViewById<MaterialButton>(R.id.dlgSaveButton)

        saveButton.setOnClickListener {
            val label = labelInput.text.toString().trim()
            val city = cityInput.text.toString().trim()
            val street = streetInput.text.toString().trim()
            val notes = notesInput.text.toString().trim()

            labelLayout.error = null
            cityLayout.error = null
            streetLayout.error = null
            var ok = true
            if (label.isEmpty()) { labelLayout.error = "Give this address a name"; ok = false }
            if (city.isEmpty()) { cityLayout.error = "Enter the city"; ok = false }
            if (street.isEmpty()) { streetLayout.error = "Enter the street"; ok = false }
            if (!ok) return@setOnClickListener

            saveButton.isEnabled = false
            addressesRef().add(
                mapOf(
                    "label" to label,
                    "city" to city,
                    "street" to street,
                    "notes" to notes,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).addOnSuccessListener { ref ->
                selectedAddressId = ref.id
                dialog.dismiss()
                loadAddresses(null)
            }.addOnFailureListener { e ->
                saveButton.isEnabled = true
                Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
        dialog.show()
    }

    // ---------------------------------------------------------------- steps

    private fun goToPayment() {
        val addr = addresses.firstOrNull { it.id == selectedAddressId }
        if (addr == null) {
            Toast.makeText(this, "Please add or select a delivery address", Toast.LENGTH_SHORT).show()
            return
        }
        if (items.isEmpty()) {
            Toast.makeText(this, "Your cart is empty", Toast.LENGTH_SHORT).show()
            return
        }
        recapAddress.text = "Delivering to ${addr.label}: ${addr.city}, ${addr.street}"
        showStep(2)
    }

    private fun showStep(n: Int) {
        step = n
        stepAddress.visibility = if (n == 1) View.VISIBLE else View.GONE
        stepPayment.visibility = if (n == 2) View.VISIBLE else View.GONE
        styleStep(stepDot1, stepLabel1, true)
        styleStep(stepDot2, stepLabel2, n == 2)
        updateTotals()
        mainScroll.scrollTo(0, 0)
    }

    private fun styleStep(dot: TextView, label: TextView, active: Boolean) {
        dot.setBackgroundResource(if (active) R.drawable.step_active else R.drawable.step_inactive)
        dot.setTextColor(if (active) Color.WHITE else Color.parseColor("#8A7968"))
        label.setTextColor(Color.parseColor(if (active) "#3E2A1C" else "#8A7968"))
    }

    private fun selectDelivery(express: Boolean) {
        isExpress = express
        optStandard.setBackgroundResource(
            if (!express) R.drawable.option_selected else R.drawable.option_unselected
        )
        optExpress.setBackgroundResource(
            if (express) R.drawable.option_selected else R.drawable.option_unselected
        )
        updateTotals()
    }

    private fun selectPayment(card: Boolean) {
        payByCard = card
        cardSection.visibility = if (card) View.VISIBLE else View.GONE
        cashSection.visibility = if (card) View.GONE else View.VISIBLE
        stylePayTab(tabCard, card)
        stylePayTab(tabCash, !card)
    }

    private fun stylePayTab(tab: TextView, selected: Boolean) {
        if (selected) {
            tab.setBackgroundResource(R.drawable.segment_selected)
            tab.setTextColor(Color.WHITE)
        } else {
            tab.setBackgroundColor(Color.TRANSPARENT)
            tab.setTextColor(Color.parseColor("#8A7968"))
        }
    }

    private fun updateTotals() {
        val total = subtotal + deliveryFee
        findViewById<TextView>(R.id.sumSubtotal).text = "₪$subtotal"
        findViewById<TextView>(R.id.sumDelivery).text = "₪$deliveryFee"
        findViewById<TextView>(R.id.sumTotal).text = "₪$total"
        actionButton.text =
            if (step == 1) "Continue to payment" else "Place Order  |  ₪$total"
    }

    // ---------------------------------------------------------------- card inputs

    private fun setupCardInputs() {
        cardNumberInput.addTextChangedListener(object : TextWatcher {
            private var editing = false
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (s == null || editing) return
                editing = true
                val digits = s.toString().filter { it.isDigit() }.take(16)
                s.replace(0, s.length, digits.chunked(4).joinToString(" "))
                editing = false
                updateCardPreview()
            }
        })

        cardExpiryInput.addTextChangedListener(object : TextWatcher {
            private var editing = false
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (s == null || editing) return
                editing = true
                val digits = s.toString().filter { it.isDigit() }.take(4)
                val formatted =
                    if (digits.length >= 3) digits.substring(0, 2) + "/" + digits.substring(2)
                    else digits
                s.replace(0, s.length, formatted)
                editing = false
                updateCardPreview()
            }
        })

        cardNameInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) { updateCardPreview() }
        })
    }

    private fun updateCardPreview() {
        val num = cardNumberInput.text.toString().filter { it.isDigit() }
        cardNumberPreview.text = num.padEnd(16, '•').chunked(4).joinToString(" ")
        val name = cardNameInput.text.toString().trim()
        cardNamePreview.text = if (name.isEmpty()) "YOUR NAME" else name.uppercase()
        val exp = cardExpiryInput.text.toString()
        cardExpiryPreview.text = if (exp.isEmpty()) "MM/YY" else exp
        cardBrand.text = detectBrand(num)
    }

    private fun detectBrand(num: String): String = when {
        num.startsWith("4") -> "VISA"
        num.length >= 2 && num.substring(0, 2).toInt() in 51..55 -> "MASTERCARD"
        num.length >= 4 && num.substring(0, 4).toInt() in 2221..2720 -> "MASTERCARD"
        num.startsWith("34") || num.startsWith("37") -> "AMEX"
        else -> "CARD"
    }

    private fun luhnValid(num: String): Boolean {
        var sum = 0
        var alt = false
        for (i in num.length - 1 downTo 0) {
            var n = num[i] - '0'
            if (alt) {
                n *= 2
                if (n > 9) n -= 9
            }
            sum += n
            alt = !alt
        }
        return sum % 10 == 0
    }

    private fun expiryValid(text: String): Boolean {
        val m = Regex("^(\\d{2})/(\\d{2})$").find(text) ?: return false
        val month = m.groupValues[1].toInt()
        val year = 2000 + m.groupValues[2].toInt()
        if (month !in 1..12) return false
        val cal = Calendar.getInstance()
        val curYear = cal.get(Calendar.YEAR)
        val curMonth = cal.get(Calendar.MONTH) + 1
        return year > curYear || (year == curYear && month >= curMonth)
    }

    private fun validateCard(): Boolean {
        cardNumberLayout.error = null
        cardNameLayout.error = null
        cardExpiryLayout.error = null
        cardCvvLayout.error = null

        val num = cardNumberInput.text.toString().filter { it.isDigit() }
        val name = cardNameInput.text.toString().trim()
        val exp = cardExpiryInput.text.toString()
        val cvv = cardCvvInput.text.toString()
        var ok = true

        if (num.length !in 13..16 || !luhnValid(num)) {
            cardNumberLayout.error = "Enter a valid card number"; ok = false
        }
        if (name.length < 3) {
            cardNameLayout.error = "Enter the name on the card"; ok = false
        }
        if (!expiryValid(exp)) {
            cardExpiryLayout.error = "Invalid or expired"; ok = false
        }
        val cvvLen = if (detectBrand(num) == "AMEX") 4 else 3
        if (cvv.length != cvvLen) {
            cardCvvLayout.error = "$cvvLen digits"; ok = false
        }
        return ok
    }

    // ---------------------------------------------------------------- place order

    private fun placeOrder() {
        val addr = addresses.firstOrNull { it.id == selectedAddressId }
        if (addr == null) { showStep(1); return }
        if (items.isEmpty()) {
            Toast.makeText(this, "Your cart is empty", Toast.LENGTH_SHORT).show()
            return
        }

        val payment: Map<String, Any>
        val payLabel: String
        if (payByCard) {
            if (!validateCard()) return
            val num = cardNumberInput.text.toString().filter { it.isDigit() }
            val brand = detectBrand(num)
            val last4 = num.takeLast(4)
            // نخزّن فقط النوع وآخر 4 أرقام. لا رقم كامل ولا CVV
            payment = mapOf("method" to "card", "brand" to brand, "last4" to last4, "demo" to true)
            payLabel = "$brand •••• $last4"
        } else {
            payment = mapOf("method" to "cash")
            payLabel = "Cash on delivery"
        }

        actionButton.isEnabled = false

        val orderItems = items.map {
            mapOf(
                "name" to it.name,
                "options" to it.options,
                "price" to it.price,
                "quantity" to it.quantity,
                "image" to it.image
            )
        }
        val order = hashMapOf(
            "uid" to uid,
            "name" to userName,
            "phone" to userPhone,
            "address" to mapOf(
                "label" to addr.label,
                "city" to addr.city,
                "street" to addr.street,
                "notes" to addr.notes
            ),
            "items" to orderItems,
            "subtotal" to subtotal,
            "deliveryFee" to deliveryFee,
            "total" to subtotal + deliveryFee,
            "deliveryType" to if (isExpress) "Express" else "Standard",
            "payment" to payment,
            "status" to "placed",
            "createdAt" to FieldValue.serverTimestamp()
        )

        val cartRef = db.collection("users").document(uid).collection("cart")
        val batch = db.batch()
        batch.set(db.collection("orders").document(), order)
        items.forEach { batch.delete(cartRef.document(it.id)) }

        batch.commit()
            .addOnSuccessListener {
                cardNumberInput.setText("")
                cardNameInput.setText("")
                cardExpiryInput.setText("")
                cardCvvInput.setText("")
                findViewById<TextView>(R.id.doneEta).text =
                    "Estimated delivery: " + (if (isExpress) "15-25 min" else "35-45 min") +
                            "\nPayment: $payLabel"
                orderDone.visibility = View.VISIBLE
            }
            .addOnFailureListener { e ->
                actionButton.isEnabled = true
                Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun goHome() {
        val intent = Intent(this, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }
}