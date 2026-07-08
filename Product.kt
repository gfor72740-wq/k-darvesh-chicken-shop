package com.example.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Egg
import androidx.compose.material.icons.filled.FoodBank
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.OutdoorGrill
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.SoupKitchen
import androidx.compose.ui.graphics.vector.ImageVector

data class Product(
    val id: String,
    val name: String,
    val category: ProductCategory,
    val price: Double,
    val unit: String,
    val description: String,
    val iconName: String // Using strings to represent icon mapping
) {
    val displayPrice: String
        get() = "₹${price.toInt()}/$unit"

    val icon: ImageVector
        get() = when (iconName) {
            "boiler" -> Icons.Default.Restaurant
            "gavran" -> Icons.Default.OutdoorGrill
            "eggs" -> Icons.Default.Egg
            "masale" -> Icons.Default.SoupKitchen
            "curry" -> Icons.Default.Kitchen
            else -> Icons.Default.FoodBank
        }
}

enum class ProductCategory {
    ALL,
    MEAT,
    EGGS,
    SPICES
}

object ProductCatalog {
    val items = listOf(
        Product(
            id = "boiler_chicken",
            name = "Boiler Chicken",
            category = ProductCategory.MEAT,
            price = 270.0,
            unit = "kg",
            description = "Freshly cut boiler chicken. Clean, hygienic, and processed to order.",
            iconName = "boiler"
        ),
        Product(
            id = "gavran_chicken",
            name = "Gavran Chicken",
            category = ProductCategory.MEAT,
            price = 450.0,
            unit = "kg",
            description = "Traditional country range (Gavran) chicken. Known for its rich, aromatic flavor and premium taste.",
            iconName = "gavran"
        ),
        Product(
            id = "farm_eggs",
            name = "Fresh Farm Eggs",
            category = ProductCategory.EGGS,
            price = 6.0,
            unit = "pc",
            description = "High-protein, fresh white farm eggs, locally sourced.",
            iconName = "eggs"
        ),
        Product(
            id = "special_masala",
            name = "K Darvesh Special Masala",
            category = ProductCategory.SPICES,
            price = 50.0,
            unit = "pack",
            description = "Our house-blend recipe of rich spices to cook the ultimate chicken curry.",
            iconName = "masale"
        ),
        Product(
            id = "chicken_curry_cut",
            name = "Chicken Curry Cut",
            category = ProductCategory.MEAT,
            price = 290.0,
            unit = "kg",
            description = "Expertly cut bone-in pieces ideal for chicken curry and slow cooking.",
            iconName = "curry"
        )
    )
}
