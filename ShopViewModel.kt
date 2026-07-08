package com.example.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PushNotification(
    val id: String,
    val title: String,
    val message: String,
    val timestamp: String,
    val isRead: Boolean = false
)

class ShopViewModel(
    private val authRepo: AuthRepository,
    private val shopRepo: ShopRepository
) : ViewModel() {

    val currentUser: StateFlow<User?> = authRepo.currentUser
    val products: StateFlow<List<Product>> = shopRepo.products
    val cart: StateFlow<List<CartItem>> = shopRepo.cart
    val orders: StateFlow<List<Order>> = shopRepo.orders

    // Tracking state
    private val _activeTrackingOrder = MutableStateFlow<Order?>(null)
    val activeTrackingOrder = _activeTrackingOrder.asStateFlow()

    // Push notification system (Simulated local push alerts)
    private val _notifications = MutableStateFlow<List<PushNotification>>(listOf(
        PushNotification(
            id = "welcome",
            title = "Welcome to K Darvesh Chicken Shop! 🐔",
            message = "Enjoy premium Boiler and Gavran chicken cut fresh to order, delivered to your doorstep.",
            timestamp = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
        )
    ))
    val notifications = _notifications.asStateFlow()

    // Popup alert state for instant user notification
    private val _activeBannerAlert = MutableStateFlow<PushNotification?>(null)
    val activeBannerAlert = _activeBannerAlert.asStateFlow()

    // Loading & UI States
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _errorMsg = MutableStateFlow<String?>(null)
    val errorMsg = _errorMsg.asStateFlow()

    fun clearError() {
        _errorMsg.value = null
    }

    fun dismissBanner() {
        _activeBannerAlert.value = null
    }

    // AUTH ACTION
    fun login(phone: String, email: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMsg.value = null
            val result = authRepo.login(phone, email, password)
            _isLoading.value = false
            if (result.isSuccess) {
                onSuccess()
            } else {
                _errorMsg.value = result.exceptionOrNull()?.message ?: "Login failed"
            }
        }
    }

    fun signup(phone: String, name: String, email: String, password: String, isAdmin: Boolean, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMsg.value = null
            val result = authRepo.signup(phone, name, email, password, isAdmin)
            _isLoading.value = false
            if (result.isSuccess) {
                onSuccess()
            } else {
                _errorMsg.value = result.exceptionOrNull()?.message ?: "Signup failed"
            }
        }
    }

    fun logout(onSuccess: () -> Unit) {
        authRepo.logout()
        _activeTrackingOrder.value = null
        onSuccess()
    }

    // CART ACTIONS
    fun addToCart(product: Product, quantity: Double) {
        shopRepo.addToCart(product, quantity)
        triggerPushAlert(
            title = "Added to Cart 🛒",
            message = "${product.name} (${if (product.unit == "kg") "$quantity kg" else "${quantity.toInt()} pcs"}) added."
        )
    }

    fun removeFromCart(product: Product) {
        shopRepo.removeFromCart(product)
    }

    fun updateCartQuantity(product: Product, quantity: Double) {
        shopRepo.updateCartQuantity(product, quantity)
    }

    fun clearCart() {
        shopRepo.clearCart()
    }

    // ORDER ACTION
    fun placeOrder(address: String, paymentMethod: PaymentMethod, notes: String, onSuccess: (Order) -> Unit) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val result = shopRepo.placeOrder(
                customerName = user.name,
                customerPhone = user.phone,
                address = address,
                paymentMethod = paymentMethod,
                notes = notes
            )
            _isLoading.value = false
            if (result.isSuccess) {
                val order = result.getOrThrow()
                _activeTrackingOrder.value = order
                
                triggerPushAlert(
                    title = "Order Placed Successfully! 🎉",
                    message = "Your order ${order.id} for ${order.displayTotal} has been received. Track status on the live tracker!"
                )
                
                onSuccess(order)
            } else {
                _errorMsg.value = result.exceptionOrNull()?.message ?: "Failed to place order"
            }
        }
    }

    fun selectTrackingOrder(order: Order) {
        _activeTrackingOrder.value = order
    }

    // ADMIN ACTIONS
    fun updateProductPrice(productId: String, newPrice: Double) {
        viewModelScope.launch {
            val result = shopRepo.updateProductPrice(productId, newPrice)
            if (result.isSuccess) {
                triggerPushAlert(
                    title = "Price Updated 🏷️",
                    message = "Product price successfully updated to ₹${newPrice.toInt()}/unit."
                )
            }
        }
    }

    fun updateOrderStatus(orderId: String, newStatus: OrderStatus) {
        viewModelScope.launch {
            val result = shopRepo.updateOrderStatus(orderId, newStatus)
            if (result.isSuccess) {
                // If the updated order is currently being tracked, update its state
                val currentTracking = _activeTrackingOrder.value
                if (currentTracking != null && currentTracking.id == orderId) {
                    _activeTrackingOrder.value = currentTracking.copy(status = newStatus)
                }
                
                // Fetch the status text
                val statusText = when (newStatus) {
                    OrderStatus.PENDING -> "Order Placed"
                    OrderStatus.PREPARING -> "Preparing & Packing 📦"
                    OrderStatus.OUT_FOR_DELIVERY -> "Out for Delivery 🚚"
                    OrderStatus.DELIVERED -> "Delivered Successfully ✅"
                    OrderStatus.CANCELLED -> "Cancelled ❌"
                }

                triggerPushAlert(
                    title = "Order Status Update! 🔔",
                    message = "Your order $orderId is now: $statusText."
                )
            }
        }
    }

    fun sendAdminBroadcastNotification(title: String, message: String) {
        triggerPushAlert(
            title = "K Darvesh Broadcast: $title 🔔",
            message = message
        )
    }

    private fun triggerPushAlert(title: String, message: String) {
        val newNotification = PushNotification(
            id = System.currentTimeMillis().toString(),
            title = title,
            message = message,
            timestamp = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
        )
        _notifications.value = listOf(newNotification) + _notifications.value
        _activeBannerAlert.value = newNotification
    }

    // Provider Factory Helper
    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val localRepo = LocalPersistentRepository(context.applicationContext)
            
            // Try to set up real Firebase references if present on device
            val authInstance = try {
                com.google.firebase.auth.FirebaseAuth.getInstance()
            } catch (e: Exception) {
                null
            }
            val firestoreInstance = try {
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
            } catch (e: Exception) {
                null
            }
            
            val finalRepo = FirebaseShopRepository(authInstance, firestoreInstance, localRepo)
            return ShopViewModel(finalRepo, finalRepo) as T
        }
    }
}
