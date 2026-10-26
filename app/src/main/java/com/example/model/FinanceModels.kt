package com.example.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class FinancialAccount(
    @DocumentId val id: String = "",
    val name: String = "Cash",
    val accountType: String = "Cash", // Cash, Bank account, Other
    val openingBalance: Double = 0.0,
    val currentBalance: Double = 0.0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FinancialTransaction(
    @DocumentId val id: String = "",
    val type: String = "Expense", // Income, Expense, Transfer
    val amount: Double = 0.0,
    val category: String = "Food",
    val date: String = "", // YYYY-MM-DD
    val accountId: String = "",
    val toAccountId: String = "", // for transfers
    val paymentMethod: String = "Cash", // Cash, UPI, Card, NetBanking
    val description: String = "",
    val tags: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FinancialBudget(
    @DocumentId val id: String = "",
    val monthlyExpenseBudget: Double = 15000.0,
    val monthlySavingsTarget: Double = 5000.0,
    val categoryBudgets: String = "{}", // JSON map of category to double limit
    val monthYear: String = "", // YYYY-MM
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
