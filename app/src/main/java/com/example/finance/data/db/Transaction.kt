package com.example.finance.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val amount: Double,
    val type: String, // "expense" or "income"
    val category: String,
    val merchant: String,
    val description: String?,
    val date: Long, // timestamp
    val referenceId: String?, // UTR/UPI Ref for duplicate checking
    val isAutoCaptured: Boolean = false,
    val merchantIcon: String? = null, // URL or resource name for logo
    val account: String = "Main" // "Main", "Slice", etc.
)
