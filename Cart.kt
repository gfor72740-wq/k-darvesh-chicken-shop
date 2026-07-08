package com.example.data

data class CartItem(
    val product: Product,
    val quantity: Double // Supporting fractional units like 1.5 kg for chicken
) {
    val totalPrice: Double
        get() = product.price * quantity

    val displayTotalPrice: String
        get() = "₹${totalPrice.toInt()}"

    val displayQuantity: String
        get() = if (product.unit == "kg") {
            String.format("%.1f kg", quantity)
        } else {
            "${quantity.toInt()} ${product.unit}"
        }
}
