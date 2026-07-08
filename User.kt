package com.example.data

enum class UserRole {
    CUSTOMER,
    ADMIN
}

data class User(
    val phone: String,
    val name: String,
    val email: String,
    val role: UserRole = UserRole.CUSTOMER
)
