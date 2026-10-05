package com.example.shoppingcartapp

data class CartItem(
    val id: String,
    val name: String,
    val price: Int,
    var quantity: Int = 0,
    val options: String = "",
    val image: String = ""
)