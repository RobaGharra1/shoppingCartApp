package com.example.shoppingcartapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CartAdapter(
    private val cartItems: MutableList<CartItem>,
    private val onQuantityChange: (String, Int, Int) -> Unit,
    private val onDeleteClick: (String, Int) -> Unit
) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    class CartViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.cartItemImage)
        val itemName: TextView = view.findViewById(R.id.cartItemName)
        val options: TextView = view.findViewById(R.id.cartItemOptions)
        val itemPrice: TextView = view.findViewById(R.id.cartItemPrice)
        val quantityText: TextView = view.findViewById(R.id.cartItemQuantity)
        val increaseButton: Button = view.findViewById(R.id.increaseButton)
        val decreaseButton: Button = view.findViewById(R.id.decreaseButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_cart, parent, false)
        return CartViewHolder(view)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        val item = cartItems[position]
        val ctx = holder.itemView.context

        holder.itemName.text = item.name
        holder.options.text = item.options
        holder.options.visibility = if (item.options.isEmpty()) View.GONE else View.VISIBLE
        holder.itemPrice.text = "₪${item.price * item.quantity}"
        holder.quantityText.text = item.quantity.toString()

        val resId = ctx.resources.getIdentifier(item.image, "drawable", ctx.packageName)
        holder.image.setImageResource(if (resId != 0) resId else R.drawable.ic_launcher_background)

        holder.increaseButton.setOnClickListener {
            val pos = holder.adapterPosition
            if (pos == RecyclerView.NO_POSITION) return@setOnClickListener
            val newQ = item.quantity + 1
            if (newQ <= 10) {
                onQuantityChange(item.id, item.price, newQ)
            }
        }

        holder.decreaseButton.setOnClickListener {
            val pos = holder.adapterPosition
            if (pos == RecyclerView.NO_POSITION) return@setOnClickListener
            val newQ = item.quantity - 1
            if (newQ >= 1) {
                onQuantityChange(item.id, item.price, newQ)
            }
        }
    }

    override fun getItemCount(): Int = cartItems.size
}