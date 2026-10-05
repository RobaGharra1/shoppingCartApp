package com.example.shoppingcartapp

data class Product(
    val name: String,
    val price: Int,
    val inStock: String,
    val imageResIds: List<Int>,
    val category: String = "",
    val description: String = "",
    var quantity: Int = 0
)