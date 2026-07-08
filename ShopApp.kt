package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.viewmodel.PushNotification
import com.example.viewmodel.ShopViewModel

enum class AppScreen {
    LOGIN,
    SIGNUP,
    HOME,
    CART,
    CHECKOUT,
    TRACKING,
    HISTORY,
    ADMIN,
    NOTIFICATIONS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopApp(viewModel: ShopViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val products by viewModel.products.collectAsState()
    val cart by viewModel.cart.collectAsState()
    val orders by viewModel.orders.collectAsState()
    val activeTrackingOrder by viewModel.activeTrackingOrder.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val activeBannerAlert by viewModel.activeBannerAlert.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMsg by viewModel.errorMsg.collectAsState()

    var currentScreen by remember { mutableStateOf(AppScreen.LOGIN) }

    // Navigation Helper
    val navigateTo: (AppScreen) -> Unit = { screen ->
        currentScreen = screen
    }

    // Auto navigate on successful authentication
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            if (currentUser?.role == UserRole.ADMIN) {
                currentScreen = AppScreen.ADMIN
            } else if (currentScreen == AppScreen.LOGIN || currentScreen == AppScreen.SIGNUP) {
                currentScreen = AppScreen.HOME
            }
        } else {
            currentScreen = AppScreen.LOGIN
        }
    }

    Scaffold(
        topBar = {
            if (currentUser != null) {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Restaurant,
                                contentDescription = "Logo",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "K Darvesh",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    actions = {
                        if (currentUser?.role == UserRole.ADMIN) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Text(
                                    text = "Admin",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        IconButton(onClick = { viewModel.logout { navigateTo(AppScreen.LOGIN) } }) {
                            Icon(
                                imageVector = Icons.Default.ExitToApp,
                                contentDescription = "Log Out",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        },
        bottomBar = {
            if (currentUser != null) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    val unreadNotifications = notifications.size

                    if (currentUser?.role == UserRole.ADMIN) {
                        // Admin-specific Navigation Bar
                        NavigationBarItem(
                            selected = currentScreen == AppScreen.ADMIN,
                            onClick = { navigateTo(AppScreen.ADMIN) },
                            icon = { Icon(Icons.Default.Dashboard, "Admin Panel") },
                            label = { Text("Admin") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        NavigationBarItem(
                            selected = currentScreen == AppScreen.HOME,
                            onClick = { navigateTo(AppScreen.HOME) },
                            icon = { Icon(Icons.Default.Storefront, "Shop View") },
                            label = { Text("Shop") }
                        )
                        NavigationBarItem(
                            selected = currentScreen == AppScreen.NOTIFICATIONS,
                            onClick = { navigateTo(AppScreen.NOTIFICATIONS) },
                            icon = { 
                                BadgedBox(badge = { if (unreadNotifications > 0) Badge { Text("$unreadNotifications") } }) {
                                    Icon(Icons.Default.Notifications, "Notifications")
                                }
                            },
                            label = { Text("Alerts") }
                        )
                    } else {
                        // Customer Navigation Bar
                        NavigationBarItem(
                            selected = currentScreen == AppScreen.HOME,
                            onClick = { navigateTo(AppScreen.HOME) },
                            icon = { Icon(Icons.Default.Restaurant, "Products") },
                            label = { Text("Products") }
                        )
                        NavigationBarItem(
                            selected = currentScreen == AppScreen.CART,
                            onClick = { navigateTo(AppScreen.CART) },
                            icon = {
                                BadgedBox(badge = { if (cart.isNotEmpty()) Badge { Text("${cart.size}") } }) {
                                    Icon(Icons.Default.ShoppingCart, "Cart")
                                }
                            },
                            label = { Text("Cart") },
                            modifier = Modifier.testTag("nav_cart")
                        )
                        NavigationBarItem(
                            selected = currentScreen == AppScreen.HISTORY || currentScreen == AppScreen.TRACKING,
                            onClick = { navigateTo(AppScreen.HISTORY) },
                            icon = { Icon(Icons.Default.ListAlt, "Orders") },
                            label = { Text("Orders") },
                            modifier = Modifier.testTag("nav_orders")
                        )
                        NavigationBarItem(
                            selected = currentScreen == AppScreen.NOTIFICATIONS,
                            onClick = { navigateTo(AppScreen.NOTIFICATIONS) },
                            icon = {
                                BadgedBox(badge = { if (unreadNotifications > 0) Badge { Text("$unreadNotifications") } }) {
                                    Icon(Icons.Default.Notifications, "Alerts")
                                }
                            },
                            label = { Text("Alerts") }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Screen switching logic
            Crossfade(targetState = currentScreen, label = "ScreenTransition") { screen ->
                when (screen) {
                    AppScreen.LOGIN -> LoginScreen(viewModel, navigateTo)
                    AppScreen.SIGNUP -> SignupScreen(viewModel, navigateTo)
                    AppScreen.HOME -> HomeScreen(viewModel, navigateTo, products, cart)
                    AppScreen.CART -> CartScreen(viewModel, navigateTo, cart)
                    AppScreen.CHECKOUT -> CheckoutScreen(viewModel, navigateTo, cart)
                    AppScreen.TRACKING -> OrderTrackingScreen(viewModel, navigateTo, activeTrackingOrder)
                    AppScreen.HISTORY -> OrderHistoryScreen(viewModel, navigateTo, orders)
                    AppScreen.ADMIN -> AdminPanelScreen(viewModel, navigateTo, products, orders)
                    AppScreen.NOTIFICATIONS -> NotificationsScreen(viewModel, notifications)
                }
            }

            // Global Loading Indicator Overlays
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable(enabled = false) {},
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Text("Processing...", fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            // Floating Custom Push Notification Popup Banner (Top Slide-In)
            AnimatedVisibility(
                visible = activeBannerAlert != null,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                activeBannerAlert?.let { alert ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = "Notification",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = alert.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = alert.message,
                                    fontSize = 12.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(
                                onClick = { viewModel.dismissBanner() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, "Dismiss", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// SCREEN IMPLEMENTATIONS
// ==========================================

@Composable
fun LoginScreen(viewModel: ShopViewModel, navigateTo: (AppScreen) -> Unit) {
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val errorMsg by viewModel.errorMsg.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        MaterialTheme.colorScheme.background
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Shop Icon Branding
            Surface(
                modifier = Modifier.size(100.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                tonalElevation = 8.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Restaurant,
                        contentDescription = "Chicken Shop Logo",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(54.dp)
                    )
                }
            }

            Text(
                text = "K DARVESH",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Fresh Boiler & Gavran Chicken Shop",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Text Inputs
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone Number") },
                leadingIcon = { Icon(Icons.Default.Phone, "Phone") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("login_phone"),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email Address") },
                leadingIcon = { Icon(Icons.Default.Email, "Email") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("login_email"),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                leadingIcon = { Icon(Icons.Default.Lock, "Password") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("login_password"),
                shape = RoundedCornerShape(12.dp)
            )

            if (errorMsg != null) {
                Text(
                    text = errorMsg ?: "",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )
            }

            Button(
                onClick = {
                    if (email.isNotEmpty() && password.isNotEmpty()) {
                        viewModel.login(phone, email, password) {
                            // On Auth success, auto navigation occurs via LaunchedEffect
                        }
                    } else {
                        viewModel.login(phone, email, password) { }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("login_submit"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Login", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            TextButton(onClick = { navigateTo(AppScreen.SIGNUP) }) {
                Text("Don't have an account? Sign Up", color = MaterialTheme.colorScheme.primary)
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            Text(
                text = "Tip: Log in with 'admin@shop.com' and password 'admin' or phone '9999999999' to access the Admin Dashboard directly!",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

@Composable
fun SignupScreen(viewModel: ShopViewModel, navigateTo: (AppScreen) -> Unit) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isAdmin by remember { mutableStateOf(false) }
    val errorMsg by viewModel.errorMsg.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        MaterialTheme.colorScheme.background
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Create Account",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Join K Darvesh and order fresh poultry items instantly",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Full Name") },
                leadingIcon = { Icon(Icons.Default.Person, "Name") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("signup_name"),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone Number") },
                leadingIcon = { Icon(Icons.Default.Phone, "Phone") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("signup_phone"),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email Address") },
                leadingIcon = { Icon(Icons.Default.Email, "Email") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("signup_email"),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                leadingIcon = { Icon(Icons.Default.Lock, "Password") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("signup_password"),
                shape = RoundedCornerShape(12.dp)
            )

            // Dynamic Switch to create Admin role for testing ease
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Register as Administrator", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Enable to access shop admin controls", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                }
                Switch(
                    checked = isAdmin,
                    onCheckedChange = { isAdmin = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
                )
            }

            if (errorMsg != null) {
                Text(
                    text = errorMsg ?: "",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Button(
                onClick = {
                    if (name.isNotEmpty() && phone.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty()) {
                        viewModel.signup(phone, name, email, password, isAdmin) {}
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("signup_submit"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Sign Up", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            TextButton(onClick = { navigateTo(AppScreen.LOGIN) }) {
                Text("Already have an account? Log In", color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun HomeScreen(
    viewModel: ShopViewModel,
    navigateTo: (AppScreen) -> Unit,
    products: List<Product>,
    cart: List<CartItem>
) {
    var selectedCategory by remember { mutableStateOf(ProductCategory.ALL) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Welcome Banner and Announcement (Simulated Push banner trigger)
        Card(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "100% Fresh & Hygienic",
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "K Darvesh Chicken",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Order delicious meats, eggs & masale locally.",
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                        fontSize = 12.sp
                    )
                }
                Icon(
                    imageVector = Icons.Default.Restaurant,
                    contentDescription = "Announce",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(54.dp)
                )
            }
        }

        // Category Filter Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ProductCategory.values().forEach { category ->
                val isSelected = selectedCategory == category
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedCategory = category },
                    label = { 
                        Text(
                            text = when(category) {
                                ProductCategory.ALL -> "All Products"
                                ProductCategory.MEAT -> "Fresh Meat"
                                ProductCategory.EGGS -> "Eggs"
                                ProductCategory.SPICES -> "Spices & Masale"
                            }
                        ) 
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Product Listings
        val filteredProducts = products.filter {
            selectedCategory == ProductCategory.ALL || it.category == selectedCategory
        }

        if (filteredProducts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("No products found in this category.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(filteredProducts) { product ->
                    val cartItem = cart.find { it.product.id == product.id }
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("product_card_${product.id}"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Product Icon Graphic
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                modifier = Modifier.size(72.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = product.icon,
                                        contentDescription = product.name,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            // Product details
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = product.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = product.description,
                                    fontSize = 11.sp,
                                    color = Color.Gray,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = product.displayPrice,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            // Dynamic Cart Actions (Add / Increase)
                            if (cartItem == null) {
                                Button(
                                    onClick = { viewModel.addToCart(product, if (product.unit == "kg") 1.0 else 1.0) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    modifier = Modifier.testTag("add_to_cart_${product.id}")
                                ) {
                                    Text("Add", fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val decrementAmount = if (product.unit == "kg") 0.5 else 1.0
                                    val incrementAmount = if (product.unit == "kg") 0.5 else 1.0

                                    IconButton(
                                        onClick = { 
                                            viewModel.updateCartQuantity(product, cartItem.quantity - decrementAmount)
                                        },
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(
                                                MaterialTheme.colorScheme.primaryContainer,
                                                CircleShape
                                            )
                                    ) {
                                        Icon(
                                            Icons.Default.Remove,
                                            "Decrease",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    
                                    Text(
                                        text = cartItem.displayQuantity,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    
                                    IconButton(
                                        onClick = { 
                                            viewModel.updateCartQuantity(product, cartItem.quantity + incrementAmount)
                                        },
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(
                                                MaterialTheme.colorScheme.primary,
                                                CircleShape
                                            )
                                    ) {
                                        Icon(
                                            Icons.Default.Add,
                                            "Increase",
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CartScreen(viewModel: ShopViewModel, navigateTo: (AppScreen) -> Unit, cart: List<CartItem>) {
    if (cart.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ShoppingCart,
                    contentDescription = "Empty",
                    tint = Color.LightGray,
                    modifier = Modifier.size(100.dp)
                )
                Text(
                    text = "Your Cart is Empty",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )
                Text(
                    text = "Add fresh boiler chicken, country eggs, and spices to get started.",
                    fontSize = 13.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
                Button(
                    onClick = { navigateTo(AppScreen.HOME) },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Browse Products", fontWeight = FontWeight.Bold)
                }
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Shopping Cart",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(cart) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.product.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "${item.displayQuantity} × ₹${item.product.price.toInt()}/${item.product.unit}",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                            
                            Text(
                                text = item.displayTotalPrice,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )

                            IconButton(onClick = { viewModel.removeFromCart(item.product) }) {
                                Icon(Icons.Default.Delete, "Remove", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Billing details
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val subtotal = cart.sumOf { it.totalPrice }
                    val deliveryFee = if (subtotal >= 500) 0.0 else 40.0
                    val grandTotal = subtotal + deliveryFee

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Subtotal", color = Color.Gray)
                        Text("₹${subtotal.toInt()}", fontWeight = FontWeight.Medium)
                    }
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Delivery Charges", color = Color.Gray)
                        Text(
                            text = if (deliveryFee == 0.0) "FREE" else "₹${deliveryFee.toInt()}",
                            fontWeight = FontWeight.Medium,
                            color = if (deliveryFee == 0.0) Color.Green else Color.Black
                        )
                    }
                    if (subtotal < 500) {
                        Text(
                            text = "Add ₹${(500 - subtotal).toInt()} more for FREE delivery!",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Divider(color = Color.LightGray, modifier = Modifier.padding(vertical = 4.dp))
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Grand Total", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("₹${grandTotal.toInt()}", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { navigateTo(AppScreen.CHECKOUT) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("checkout_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Proceed to Checkout", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun CheckoutScreen(viewModel: ShopViewModel, navigateTo: (AppScreen) -> Unit, cart: List<CartItem>) {
    val currentUser by viewModel.currentUser.collectAsState()
    var address by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf(PaymentMethod.CASH_ON_DELIVERY) }
    var notes by remember { mutableStateOf("") }
    
    // Scan code simulation
    var showUpiScanner by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = { navigateTo(AppScreen.CART) }) {
                Icon(Icons.AutoMirrored.Default.ArrowBack, "Back")
            }
            Text("Checkout", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }

        Text("Delivery Information", fontWeight = FontWeight.Bold, fontSize = 16.sp)

        OutlinedTextField(
            value = currentUser?.name ?: "",
            onValueChange = {},
            label = { Text("Customer Name") },
            enabled = false,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        OutlinedTextField(
            value = currentUser?.phone ?: "",
            onValueChange = {},
            label = { Text("Contact Phone") },
            enabled = false,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            label = { Text("Delivery Address *") },
            placeholder = { Text("Enter full address, landmark, floor no.") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("checkout_address"),
            shape = RoundedCornerShape(12.dp)
        )

        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Special Cutting/Delivery Instructions") },
            placeholder = { Text("E.g. Clean cut, skinless chicken, deliver by afternoon") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Divider(color = Color.LightGray.copy(alpha = 0.5f))

        Text("Payment Options", fontWeight = FontWeight.Bold, fontSize = 16.sp)

        // COD option
        Surface(
            border = BorderStroke(1.dp, if (paymentMethod == PaymentMethod.CASH_ON_DELIVERY) MaterialTheme.colorScheme.primary else Color.LightGray),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { paymentMethod = PaymentMethod.CASH_ON_DELIVERY }
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                RadioButton(
                    selected = paymentMethod == PaymentMethod.CASH_ON_DELIVERY,
                    onClick = { paymentMethod = PaymentMethod.CASH_ON_DELIVERY }
                )
                Column {
                    Text("Cash on Delivery (COD)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Pay with cash or local UPI at your door.", fontSize = 11.sp, color = Color.Gray)
                }
            }
        }

        // UPI option
        Surface(
            border = BorderStroke(1.dp, if (paymentMethod == PaymentMethod.UPI) MaterialTheme.colorScheme.primary else Color.LightGray),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { paymentMethod = PaymentMethod.UPI }
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                RadioButton(
                    selected = paymentMethod == PaymentMethod.UPI,
                    onClick = { paymentMethod = PaymentMethod.UPI }
                )
                Column {
                    Text("Pay instantly via UPI (GPay / PhonePe / Paytm)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Generates standard checkout link for payment.", fontSize = 11.sp, color = Color.Gray)
                }
            }
        }

        // Simulating UPI scan qr codes if selected
        if (paymentMethod == PaymentMethod.UPI) {
            Button(
                onClick = { showUpiScanner = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.QrCode, "UPI")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Show K Darvesh UPI QR Code", fontWeight = FontWeight.Bold)
            }
        }

        if (showUpiScanner) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Scan to Pay K Darvesh Shop", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    
                    // Simple simulated QR graphic using canvas/vector
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .background(Color.White, RoundedCornerShape(8.dp))
                            .border(1.dp, Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = "Simulated QR",
                            tint = Color.Black,
                            modifier = Modifier.size(120.dp)
                        )
                    }

                    Text("UPI ID: kdarvesh@okaxis", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("Once payment is completed in your app, press 'Place Order' below.", fontSize = 10.sp, textAlign = TextAlign.Center, color = Color.Gray)
                    
                    TextButton(onClick = { showUpiScanner = false }) {
                        Text("Close QR Code")
                    }
                }
            }
        }

        Button(
            onClick = {
                if (address.trim().isNotEmpty()) {
                    viewModel.placeOrder(address, paymentMethod, notes) { order ->
                        navigateTo(AppScreen.TRACKING)
                    }
                }
            },
            enabled = address.trim().isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("submit_order"),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Place Order", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
fun OrderTrackingScreen(viewModel: ShopViewModel, navigateTo: (AppScreen) -> Unit, order: Order?) {
    if (order == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("No Active Order to Track", fontWeight = FontWeight.Bold, color = Color.Gray)
                Button(onClick = { navigateTo(AppScreen.HOME) }) {
                    Text("Go to Shop")
                }
            }
        }
        return;
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = { navigateTo(AppScreen.HISTORY) }) {
                Icon(Icons.AutoMirrored.Default.ArrowBack, "Back")
            }
            Text("Order Tracking", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }

        // Live status banner
        Card(
            colors = CardDefaults.cardColors(
                containerColor = when(order.status) {
                    OrderStatus.DELIVERED -> Color(0xFFE8F5E9)
                    OrderStatus.CANCELLED -> Color(0xFFFFEBEE)
                    else -> MaterialTheme.colorScheme.primaryContainer
                }
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = when(order.status) {
                        OrderStatus.PENDING -> Icons.Default.ReceiptLong
                        OrderStatus.PREPARING -> Icons.Default.SoupKitchen
                        OrderStatus.OUT_FOR_DELIVERY -> Icons.Default.LocalShipping
                        OrderStatus.DELIVERED -> Icons.Default.CheckCircle
                        OrderStatus.CANCELLED -> Icons.Default.Cancel
                    },
                    contentDescription = "Status",
                    tint = when(order.status) {
                        OrderStatus.DELIVERED -> Color(0xFF2E7D32)
                        OrderStatus.CANCELLED -> Color(0xFFC62828)
                        else -> MaterialTheme.colorScheme.primary
                    },
                    modifier = Modifier.size(36.dp)
                )
                Column {
                    Text(
                        text = order.statusText,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = when(order.status) {
                            OrderStatus.DELIVERED -> Color(0xFF2E7D32)
                            OrderStatus.CANCELLED -> Color(0xFFC62828)
                            else -> MaterialTheme.colorScheme.primary
                        }
                    )
                    Text(text = "Order ID: ${order.id}", fontSize = 12.sp, color = Color.Gray)
                }
            }
        }

        // Visual Stepper
        Text("Order Journey", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        
        val steps = listOf(
            Triple(OrderStatus.PENDING, "Placed", "Order registered, preparing meat cuts"),
            Triple(OrderStatus.PREPARING, "Preparing", "Chicken cut, cleaned, hygienically packed"),
            Triple(OrderStatus.OUT_FOR_DELIVERY, "Dispatched", "Rider is heading to your address"),
            Triple(OrderStatus.DELIVERED, "Delivered", "Handed over fresh to you!")
        )

        steps.forEachIndexed { index, step ->
            val isActive = order.status == step.first
            val isCompleted = when(order.status) {
                OrderStatus.CANCELLED -> false
                OrderStatus.DELIVERED -> true
                OrderStatus.OUT_FOR_DELIVERY -> index <= 2
                OrderStatus.PREPARING -> index <= 1
                OrderStatus.PENDING -> index <= 0
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Stepper Line and Node
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(36.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(
                                color = if (isCompleted) MaterialTheme.colorScheme.primary else Color.LightGray,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCompleted) {
                            Icon(Icons.Default.Check, "Done", tint = Color.White, modifier = Modifier.size(14.dp))
                        } else {
                            Box(modifier = Modifier.size(8.dp).background(Color.White, CircleShape))
                        }
                    }
                    if (index < steps.lastIndex) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(40.dp)
                                .background(if (isCompleted) MaterialTheme.colorScheme.primary else Color.LightGray)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Step Content
                Column {
                    Text(
                        text = step.second,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (isActive) MaterialTheme.colorScheme.primary else if (isCompleted) Color.Black else Color.Gray
                    )
                    Text(
                        text = step.third,
                        fontSize = 11.sp,
                        color = Color.Gray,
                        lineHeight = 15.sp
                    )
                }
            }
        }

        Divider(color = Color.LightGray.copy(alpha = 0.5f))

        // Order Summary
        Text("Itemized Summary", fontWeight = FontWeight.Bold, fontSize = 16.sp)

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                order.items.forEach { item ->
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("${item.product.name} (x${item.displayQuantity})", fontSize = 13.sp)
                        Text(item.displayTotalPrice, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                    }
                }
                Divider(color = Color.LightGray.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 4.dp))
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Total Amount Paid", fontWeight = FontWeight.Bold)
                    Text(order.displayTotal, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                }
                Text(
                    text = "Payment Method: ${order.paymentMethod.name} (${order.paymentStatus.name})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }

        // Driver details
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.AccountBox,
                    "Driver",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Delivery Rider Assigned", fontSize = 11.sp, color = Color.Gray)
                    Text("Sanjay Patil", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Contact: +91 91234 56789", fontSize = 11.sp, color = Color.Gray)
                }
                IconButton(
                    onClick = { /* Simulated Call action */ },
                    modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                ) {
                    Icon(Icons.Default.Phone, "Call Rider", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
fun OrderHistoryScreen(viewModel: ShopViewModel, navigateTo: (AppScreen) -> Unit, orders: List<Order>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Your Orders",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (orders.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.ListAlt, "Empty", modifier = Modifier.size(64.dp), tint = Color.LightGray)
                    Text("You have placed no orders yet.", color = Color.Gray)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(orders) { order ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.selectTrackingOrder(order)
                                navigateTo(AppScreen.TRACKING)
                            },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = order.id,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = order.statusText,
                                    fontWeight = FontWeight.Bold,
                                    color = when (order.status) {
                                        OrderStatus.DELIVERED -> Color(0xFF2E7D32)
                                        OrderStatus.CANCELLED -> Color(0xFFC62828)
                                        else -> MaterialTheme.colorScheme.secondary
                                    },
                                    fontSize = 13.sp
                                )
                            }
                            Text(text = order.timestamp, fontSize = 11.sp, color = Color.Gray)
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = order.items.joinToString { "${it.product.name} (x${it.displayQuantity})" },
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Total Paid: ${order.displayTotal}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    // Quick action Reorder button
                                    OutlinedButton(
                                        onClick = {
                                            order.items.forEach { item ->
                                                viewModel.addToCart(item.product, item.quantity)
                                            }
                                            navigateTo(AppScreen.CART)
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text("Reorder", fontSize = 11.sp)
                                    }

                                    Button(
                                        onClick = {
                                            viewModel.selectTrackingOrder(order)
                                            navigateTo(AppScreen.TRACKING)
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text("Track", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(
    viewModel: ShopViewModel,
    navigateTo: (AppScreen) -> Unit,
    products: List<Product>,
    orders: List<Order>
) {
    var selectedTab by remember { mutableStateOf(0) } // 0 = Orders, 1 = Catalog Edit, 2 = Push Broadcaster
    var broadcastTitle by remember { mutableStateOf("") }
    var broadcastMsg by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "K Darvesh Admin Dashboard",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Tab Selector Row
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.background,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                Text("Manage Orders", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                Text("Edit Prices", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }) {
                Text("Broadcast Alerts", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        when (selectedTab) {
            0 -> {
                // Manage Orders
                if (orders.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No customer orders received yet.", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(orders) { order ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(text = "Order: ${order.id}", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                                        Text(
                                            text = order.statusText,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = when(order.status) {
                                                OrderStatus.DELIVERED -> Color(0xFF2E7D32)
                                                else -> MaterialTheme.colorScheme.secondary
                                            }
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = "Customer: ${order.customerName} (${order.customerPhone})", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text(text = "Address: ${order.address}", fontSize = 12.sp)
                                    if (order.notes.isNotEmpty()) {
                                        Text(text = "Cutting Instructions: ${order.notes}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    }
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Divider(color = Color.LightGray.copy(alpha = 0.5f))
                                    Spacer(modifier = Modifier.height(8.dp))

                                    order.items.forEach { item ->
                                        Text(text = "• ${item.product.name} (x${item.displayQuantity})", fontSize = 12.sp)
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(text = "Total Value: ${order.displayTotal}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        
                                        // Progression Controls
                                        if (order.status != OrderStatus.DELIVERED && order.status != OrderStatus.CANCELLED) {
                                            val nextStatus = when (order.status) {
                                                OrderStatus.PENDING -> OrderStatus.PREPARING
                                                OrderStatus.PREPARING -> OrderStatus.OUT_FOR_DELIVERY
                                                OrderStatus.OUT_FOR_DELIVERY -> OrderStatus.DELIVERED
                                                else -> OrderStatus.DELIVERED
                                            }
                                            
                                            val buttonText = when (order.status) {
                                                OrderStatus.PENDING -> "Start Packing"
                                                OrderStatus.PREPARING -> "Dispatch Rider"
                                                OrderStatus.OUT_FOR_DELIVERY -> "Complete Delivery"
                                                else -> "Advance"
                                            }

                                            Button(
                                                onClick = { viewModel.updateOrderStatus(order.id, nextStatus) },
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                                modifier = Modifier.height(34.dp)
                                            ) {
                                                Text(buttonText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        } else {
                                            Text("Completed", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            1 -> {
                // Edit Catalog Prices
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(products) { product ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = product.name, fontWeight = FontWeight.Bold)
                                    Text(text = "Current: ₹${product.price.toInt()}/${product.unit}", fontSize = 12.sp, color = Color.Gray)
                                }
                                
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Price adjustments
                                    IconButton(
                                        onClick = { viewModel.updateProductPrice(product.id, product.price - 10.0) },
                                        modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer, CircleShape).size(32.dp)
                                    ) {
                                        Text("-10", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    }
                                    IconButton(
                                        onClick = { viewModel.updateProductPrice(product.id, product.price + 10.0) },
                                        modifier = Modifier.background(MaterialTheme.colorScheme.primary, CircleShape).size(32.dp)
                                    ) {
                                        Text("+10", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            2 -> {
                // Broadcast Alerts
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("Simulate Storewide Broadcast Push Notification", fontWeight = FontWeight.Bold)
                            
                            OutlinedTextField(
                                value = broadcastTitle,
                                onValueChange = { broadcastTitle = it },
                                label = { Text("Notification Title") },
                                placeholder = { Text("E.g. Gavran Fresh stock has arrived! 🐔") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )

                            OutlinedTextField(
                                value = broadcastMsg,
                                onValueChange = { broadcastMsg = it },
                                label = { Text("Notification Message") },
                                placeholder = { Text("E.g. High quality Gavran chicken cuts now available @ ₹450/kg. Order now before stock runs out!") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Button(
                                onClick = {
                                    if (broadcastTitle.trim().isNotEmpty() && broadcastMsg.trim().isNotEmpty()) {
                                        viewModel.sendAdminBroadcastNotification(broadcastTitle, broadcastMsg)
                                        broadcastTitle = ""
                                        broadcastMsg = ""
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Send, "Send")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Broadcast Push Notification", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Text(
                        text = "Note: Sending this broadcast triggers an animated overlay banner alert immediately on all screens, mimicking real FCM (Firebase Cloud Messaging) background behaviors. The notification will also be logged in the client's 'Alerts' tab.",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun NotificationsScreen(viewModel: ShopViewModel, notifications: List<PushNotification>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Alerts & Notifications",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (notifications.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No recent alerts received.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(notifications) { notification ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = "Notification alert",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(text = notification.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(text = notification.timestamp, fontSize = 10.sp, color = Color.Gray)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = notification.message,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
