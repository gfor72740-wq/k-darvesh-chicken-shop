package com.example.data

enum class OrderStatus {
    PENDING,
    PREPARING,
    OUT_FOR_DELIVERY,
    DELIVERED,
    CANCELLED
}

enum class PaymentMethod {
    UPI,
    CASH_ON_DELIVERY
}

enum class PaymentStatus {
    PENDING,
    PAID
}

data class Order(
    val id: String,
    val customerName: String,
    val customerPhone: String,
    val address: String,
    val items: List<CartItem>,
    val subtotal: Double,
    val deliveryCharges: Double,
    val total: Double,
    val paymentMethod: PaymentMethod,
    val paymentStatus: PaymentStatus,
    val status: OrderStatus,
    val timestamp: String,
    val notes: String = ""
) {
    val displayTotal: String
        get() = "₹${total.toInt()}"

    val statusText: String
        get() = when (status) {
            OrderStatus.PENDING -> "Order Placed"
            OrderStatus.PREPARING -> "Preparing & Packing"
            OrderStatus.OUT_FOR_DELIVERY -> "Out for Delivery"
            OrderStatus.DELIVERED -> "Delivered Successfully"
            OrderStatus.CANCELLED -> "Cancelled"
        }
}
