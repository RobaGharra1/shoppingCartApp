package com.example.shoppingcartapp

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class OrderRow(
    val id: String,
    val items: String,
    val total: Int,
    val status: String,
    val time: Long
) {
    val number get() = "#" + id.take(6).uppercase()
}

object OrderStatus {
    val steps = listOf("placed", "preparing", "on_the_way", "delivered")
    private val labels = mapOf(
        "placed" to "Placed",
        "preparing" to "Preparing",
        "on_the_way" to "On the way",
        "delivered" to "Delivered"
    )

    fun label(s: String) = labels[s] ?: s.replaceFirstChar { it.uppercase() }
    fun index(s: String) = steps.indexOf(s).coerceAtLeast(0)

    // (خلفية الشارة، لون النص)
    fun colors(s: String): Pair<Int, Int> = when (s) {
        "preparing" -> Color.parseColor("#F6E1B5") to Color.parseColor("#7A5A00")
        "on_the_way" -> Color.parseColor("#D9E8F5") to Color.parseColor("#1F4E79")
        "delivered" -> Color.parseColor("#D8EBD3") to Color.parseColor("#2E6B2E")
        else -> Color.parseColor("#EFE3D5") to Color.parseColor("#6F4E37")
    }
}

class OrdersAdapter(
    private val rows: List<OrderRow>,
    private val onClick: (OrderRow) -> Unit
) : RecyclerView.Adapter<OrdersAdapter.VH>() {

    private val fmt = SimpleDateFormat("d MMM yyyy, HH:mm", Locale.ENGLISH)

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val number: TextView = view.findViewById(R.id.orderNumber)
        val status: TextView = view.findViewById(R.id.orderStatus)
        val date: TextView = view.findViewById(R.id.orderDate)
        val items: TextView = view.findViewById(R.id.orderItems)
        val total: TextView = view.findViewById(R.id.orderTotal)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_order, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val row = rows[position]
        holder.number.text = "Order ${row.number}"
        holder.date.text = fmt.format(Date(row.time))
        holder.items.text = row.items
        holder.total.text = "₪${row.total}"

        val (bg, fg) = OrderStatus.colors(row.status)
        holder.status.text = OrderStatus.label(row.status)
        holder.status.setTextColor(fg)
        holder.status.background.mutate().setTint(bg)

        holder.itemView.setOnClickListener { onClick(row) }
    }

    override fun getItemCount(): Int = rows.size
}