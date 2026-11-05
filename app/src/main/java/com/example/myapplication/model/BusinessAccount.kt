package com.example.myapplication.model

data class BusinessAccount(
    val businessId: String = "default_business",
    val businessName: String = "Mi Emprendimiento",
    val vendors: List<String> = listOf("Admin"),
    val currentVendor: String = "Admin"
)
