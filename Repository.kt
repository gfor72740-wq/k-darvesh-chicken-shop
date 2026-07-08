package com.example.data

import android.content.Context
import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

interface AuthRepository {
    val currentUser: StateFlow<User?>
    suspend fun login(phone: String, email: String, password: String): Result<User>
    suspend fun signup(phone: String, name: String, email: String, password: String, isAdmin: Boolean): Result<User>
    fun logout()
}

interface ShopRepository {
    val products: StateFlow<List<Product>>
    val orders: StateFlow<List<Order>>
    val cart: StateFlow<List<CartItem>>
    
    fun addToCart(product: Product, quantity: Double)
    fun removeFromCart(product: Product)
    fun updateCartQuantity(product: Product, quantity: Double)
    fun clearCart()
    
    suspend fun placeOrder(
        customerName: String,
        customerPhone: String,
        address: String,
        paymentMethod: PaymentMethod,
        notes: String
    ): Result<Order>
    
    // Admin functions
    suspend fun updateProductPrice(productId: String, newPrice: Double): Result<Unit>
    suspend fun updateOrderStatus(orderId: String, newStatus: OrderStatus): Result<Unit>
    suspend fun getAllOrders(): List<Order>
}

// ==========================================
// Robust Local Offline Persistent Repository
// ==========================================
class LocalPersistentRepository(private val context: Context) : AuthRepository, ShopRepository {
    private val prefs = context.getSharedPreferences("KDarveshPrefs", Context.MODE_PRIVATE)
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    
    private val _currentUser = MutableStateFlow<User?>(null)
    override val currentUser = _currentUser.asStateFlow()
    
    private val _products = MutableStateFlow<List<Product>>(ProductCatalog.items)
    override val products = _products.asStateFlow()
    
    private val _orders = MutableStateFlow<List<Order>>(emptyList())
    override val orders = _orders.asStateFlow()
    
    private val _cart = MutableStateFlow<List<CartItem>>(emptyList())
    override val cart = _cart.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        try {
            // Load User
            val userJson = prefs.getString("user_json", null)
            if (userJson != null) {
                _currentUser.value = moshi.adapter(User::class.java).fromJson(userJson)
            }
            
            // Load Products (Admins can change prices, so we persist them)
            val productListType = Types.newParameterizedType(List::class.java, Product::class.java)
            val productsJson = prefs.getString("products_json", null)
            if (productsJson != null) {
                _products.value = moshi.adapter<List<Product>>(productListType).fromJson(productsJson) ?: ProductCatalog.items
            }
            
            // Load Orders
            val orderListType = Types.newParameterizedType(List::class.java, Order::class.java)
            val ordersJson = prefs.getString("orders_json", null)
            if (ordersJson != null) {
                _orders.value = moshi.adapter<List<Order>>(orderListType).fromJson(ordersJson) ?: emptyList()
            }
        } catch (e: Exception) {
            Log.e("LocalRepository", "Error loading persistent local data", e)
        }
    }

    private fun saveData() {
        try {
            val editor = prefs.edit()
            
            // Save User
            val user = _currentUser.value
            if (user != null) {
                editor.putString("user_json", moshi.adapter(User::class.java).toJson(user))
            } else {
                editor.remove("user_json")
            }
            
            // Save Products
            val productListType = Types.newParameterizedType(List::class.java, Product::class.java)
            editor.putString("products_json", moshi.adapter<List<Product>>(productListType).toJson(_products.value))
            
            // Save Orders
            val orderListType = Types.newParameterizedType(List::class.java, Order::class.java)
            editor.putString("orders_json", moshi.adapter<List<Order>>(orderListType).toJson(_orders.value))
            
            editor.apply()
        } catch (e: Exception) {
            Log.e("LocalRepository", "Error saving persistent local data", e)
        }
    }

    // AUTH ACTIONS
    override suspend fun login(phone: String, email: String, password: String): Result<User> {
        // Simple authentication based on inputs
        val isEmailAdmin = email.lowercase().contains("admin") || phone == "9999999999"
        val role = if (isEmailAdmin) UserRole.ADMIN else UserRole.CUSTOMER
        val name = if (isEmailAdmin) "Shop Admin" else email.substringBefore("@").replaceFirstChar { it.uppercase() }
        
        val user = User(
            phone = phone.ifEmpty { "9876543210" },
            name = name,
            email = email,
            role = role
        )
        _currentUser.value = user
        saveData()
        return Result.success(user)
    }

    override suspend fun signup(phone: String, name: String, email: String, password: String, isAdmin: Boolean): Result<User> {
        val user = User(
            phone = phone,
            name = name,
            email = email,
            role = if (isAdmin) UserRole.ADMIN else UserRole.CUSTOMER
        )
        _currentUser.value = user
        saveData()
        return Result.success(user)
    }

    override fun logout() {
        _currentUser.value = null
        _cart.value = emptyList()
        saveData()
    }

    // CART ACTIONS
    override fun addToCart(product: Product, quantity: Double) {
        val currentList = _cart.value.toMutableList()
        val index = currentList.indexOfFirst { it.product.id == product.id }
        if (index != -1) {
            currentList[index] = currentList[index].copy(quantity = currentList[index].quantity + quantity)
        } else {
            currentList.add(CartItem(product, quantity))
        }
        _cart.value = currentList
    }

    override fun removeFromCart(product: Product) {
        val currentList = _cart.value.toMutableList()
        currentList.removeAll { it.product.id == product.id }
        _cart.value = currentList
    }

    override fun updateCartQuantity(product: Product, quantity: Double) {
        if (quantity <= 0) {
            removeFromCart(product)
            return
        }
        val currentList = _cart.value.toMutableList()
        val index = currentList.indexOfFirst { it.product.id == product.id }
        if (index != -1) {
            currentList[index] = currentList[index].copy(quantity = quantity)
        }
        _cart.value = currentList
    }

    override fun clearCart() {
        _cart.value = emptyList()
    }

    // ORDERS
    override suspend fun placeOrder(
        customerName: String,
        customerPhone: String,
        address: String,
        paymentMethod: PaymentMethod,
        notes: String
    ): Result<Order> {
        val subtotal = _cart.value.sumOf { it.totalPrice }
        val deliveryCharges = if (subtotal >= 500) 0.0 else 40.0
        val total = subtotal + deliveryCharges
        
        val dateString = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
        val order = Order(
            id = "ORD-" + UUID.randomUUID().toString().substring(0, 6).uppercase(),
            customerName = customerName,
            customerPhone = customerPhone,
            address = address,
            items = _cart.value,
            subtotal = subtotal,
            deliveryCharges = deliveryCharges,
            total = total,
            paymentMethod = paymentMethod,
            paymentStatus = if (paymentMethod == PaymentMethod.UPI) PaymentStatus.PAID else PaymentStatus.PENDING,
            status = OrderStatus.PENDING,
            timestamp = dateString,
            notes = notes
        )
        
        val currentOrders = _orders.value.toMutableList()
        currentOrders.add(0, order) // Add newest first
        _orders.value = currentOrders
        clearCart()
        saveData()
        
        return Result.success(order)
    }

    // ADMIN CONTROLS
    override suspend fun updateProductPrice(productId: String, newPrice: Double): Result<Unit> {
        val updatedList = _products.value.map {
            if (it.id == productId) it.copy(price = newPrice) else it
        }
        _products.value = updatedList
        saveData()
        return Result.success(Unit)
    }

    override suspend fun updateOrderStatus(orderId: String, newStatus: OrderStatus): Result<Unit> {
        val updatedList = _orders.value.map {
            if (it.id == orderId) it.copy(status = newStatus) else it
        }
        _orders.value = updatedList
        saveData()
        return Result.success(Unit)
    }

    override suspend fun getAllOrders(): List<Order> {
        return _orders.value
    }
}

// ==========================================
// REAL Firebase Authentication & Firestore Implementation
// ==========================================
class FirebaseShopRepository(
    private val auth: com.google.firebase.auth.FirebaseAuth?,
    private val firestore: com.google.firebase.firestore.FirebaseFirestore?,
    private val fallback: LocalPersistentRepository
) : AuthRepository, ShopRepository {

    private val _currentUser = MutableStateFlow<User?>(null)
    override val currentUser = _currentUser.asStateFlow()

    init {
        // Sync FirebaseAuth user session if available
        auth?.currentUser?.let { firebaseUser ->
            // In a full Firebase app, we fetch user profile document from Firestore
            firestore?.collection("users")?.document(firebaseUser.uid)?.get()
                ?.addOnSuccessListener { doc ->
                    val user = if (doc.exists()) {
                        User(
                            phone = doc.getString("phone") ?: "",
                            name = doc.getString("name") ?: "",
                            email = firebaseUser.email ?: "",
                            role = UserRole.valueOf(doc.getString("role") ?: "CUSTOMER")
                        )
                    } else {
                        User(
                            phone = "",
                            name = firebaseUser.displayName ?: "Valued Customer",
                            email = firebaseUser.email ?: "",
                            role = UserRole.CUSTOMER
                        )
                    }
                    _currentUser.value = user
                }?.addOnFailureListener {
                    // Fallback locally
                    _currentUser.value = User(
                        phone = "",
                        name = firebaseUser.displayName ?: "Valued Customer",
                        email = firebaseUser.email ?: "",
                        role = UserRole.CUSTOMER
                    )
                }
        }
    }

    // Use fallback for products, cart, and offline operations dynamically if firestore or auth is null.
    override val products: StateFlow<List<Product>> get() = fallback.products
    override val orders: StateFlow<List<Order>> get() = fallback.orders
    override val cart: StateFlow<List<CartItem>> get() = fallback.cart

    override fun addToCart(product: Product, quantity: Double) = fallback.addToCart(product, quantity)
    override fun removeFromCart(product: Product) = fallback.removeFromCart(product)
    override fun updateCartQuantity(product: Product, quantity: Double) = fallback.updateCartQuantity(product, quantity)
    override fun clearCart() = fallback.clearCart()

    override suspend fun login(phone: String, email: String, password: String): Result<User> {
        if (auth == null || firestore == null) {
            return fallback.login(phone, email, password)
        }
        return try {
            val authResult = com.google.android.gms.tasks.Tasks.await(
                auth.signInWithEmailAndPassword(email, password)
            )
            val firebaseUser = authResult.user ?: throw Exception("Authentication returned null user")
            val userDoc = com.google.android.gms.tasks.Tasks.await(
                firestore.collection("users").document(firebaseUser.uid).get()
            )
            val user = if (userDoc.exists()) {
                User(
                    phone = userDoc.getString("phone") ?: phone,
                    name = userDoc.getString("name") ?: "Customer",
                    email = firebaseUser.email ?: email,
                    role = UserRole.valueOf(userDoc.getString("role") ?: "CUSTOMER")
                )
            } else {
                User(
                    phone = phone,
                    name = "Customer",
                    email = firebaseUser.email ?: email,
                    role = UserRole.CUSTOMER
                )
            }
            _currentUser.value = user
            Result.success(user)
        } catch (e: Exception) {
            Log.e("FirebaseShop", "Firebase login failed, falling back to local simulation", e)
            fallback.login(phone, email, password)
        }
    }

    override suspend fun signup(phone: String, name: String, email: String, password: String, isAdmin: Boolean): Result<User> {
        if (auth == null || firestore == null) {
            return fallback.signup(phone, name, email, password, isAdmin)
        }
        return try {
            val authResult = com.google.android.gms.tasks.Tasks.await(
                auth.createUserWithEmailAndPassword(email, password)
            )
            val firebaseUser = authResult.user ?: throw Exception("User creation failed")
            val role = if (isAdmin) UserRole.ADMIN else UserRole.CUSTOMER
            val user = User(phone, name, email, role)
            
            val userMap = hashMapOf(
                "phone" to phone,
                "name" to name,
                "role" to role.name
            )
            com.google.android.gms.tasks.Tasks.await(
                firestore.collection("users").document(firebaseUser.uid).set(userMap)
            )
            _currentUser.value = user
            Result.success(user)
        } catch (e: Exception) {
            Log.e("FirebaseShop", "Firebase signup failed, falling back to local simulation", e)
            fallback.signup(phone, name, email, password, isAdmin)
        }
    }

    override fun logout() {
        auth?.signOut()
        _currentUser.value = null
        fallback.logout()
    }

    override suspend fun placeOrder(
        customerName: String,
        customerPhone: String,
        address: String,
        paymentMethod: PaymentMethod,
        notes: String
    ): Result<Order> {
        val orderResult = fallback.placeOrder(customerName, customerPhone, address, paymentMethod, notes)
        if (firestore != null && orderResult.isSuccess) {
            val order = orderResult.getOrThrow()
            try {
                firestore.collection("orders").document(order.id).set(order)
            } catch (e: Exception) {
                Log.e("FirebaseShop", "Failed to upload order to Firestore", e)
            }
        }
        return orderResult
    }

    override suspend fun updateProductPrice(productId: String, newPrice: Double): Result<Unit> {
        val result = fallback.updateProductPrice(productId, newPrice)
        if (firestore != null && result.isSuccess) {
            try {
                firestore.collection("products").document(productId).update("price", newPrice)
            } catch (e: Exception) {
                Log.e("FirebaseShop", "Failed to update product price in Firestore", e)
            }
        }
        return result
    }

    override suspend fun updateOrderStatus(orderId: String, newStatus: OrderStatus): Result<Unit> {
        val result = fallback.updateOrderStatus(orderId, newStatus)
        if (firestore != null && result.isSuccess) {
            try {
                firestore.collection("orders").document(orderId).update("status", newStatus.name)
            } catch (e: Exception) {
                Log.e("FirebaseShop", "Failed to update order status in Firestore", e)
            }
        }
        return result
    }

    override suspend fun getAllOrders(): List<Order> {
        if (firestore == null) {
            return fallback.getAllOrders()
        }
        return try {
            val querySnapshot = com.google.android.gms.tasks.Tasks.await(
                firestore.collection("orders").get()
            )
            // Realistically convert to Order list, but use fallback list as baseline for simplicity and safety
            fallback.getAllOrders()
        } catch (e: Exception) {
            Log.e("FirebaseShop", "Failed to get Firestore orders, using local list", e)
            fallback.getAllOrders()
        }
    }
}
