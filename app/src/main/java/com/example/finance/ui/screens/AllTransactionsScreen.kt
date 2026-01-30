package com.example.finance.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.finance.data.db.Transaction
import com.example.finance.ui.FinanceViewModel
import com.example.finance.ui.components.TransactionItem
import com.example.finance.ui.theme.*
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllTransactionsScreen(
    viewModel: FinanceViewModel,
    onNavigateBack: () -> Unit
) {
    val transactions by viewModel.allTransactions.collectAsState()
    var selectedFilter by remember { mutableStateOf("This Year") }
    val filters = listOf("This Week", "This Month", "This Year", "Previous Year")
    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    currencyFormatter.maximumFractionDigits = 0
    var selectedTransaction by remember { mutableStateOf<Transaction?>(null) }

    val filteredTransactions = remember(transactions, selectedFilter) {
        val calendar = Calendar.getInstance()
        val now = calendar.timeInMillis
        
        // Reset time for comparisons
        fun resetTime() {
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
        }

        when (selectedFilter) {
            "This Week" -> {
                resetTime()
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                val startOfWeek = calendar.timeInMillis
                transactions.filter { it.date >= startOfWeek }
            }
            "This Month" -> {
                resetTime()
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                val startOfMonth = calendar.timeInMillis
                transactions.filter { it.date >= startOfMonth }
            }
            "This Year" -> {
                resetTime()
                calendar.set(Calendar.DAY_OF_YEAR, 1)
                val startOfYear = calendar.timeInMillis
                transactions.filter { it.date >= startOfYear }
            }
            "Previous Year" -> {
                resetTime()
                val currentYear = calendar.get(Calendar.YEAR)
                calendar.set(Calendar.YEAR, currentYear - 1)
                calendar.set(Calendar.DAY_OF_YEAR, 1)
                val startOfPrevYear = calendar.timeInMillis
                
                calendar.set(Calendar.YEAR, currentYear)
                calendar.set(Calendar.DAY_OF_YEAR, 1)
                val endOfPrevYear = calendar.timeInMillis
                
                transactions.filter { it.date >= startOfPrevYear && it.date < endOfPrevYear }
            }
            else -> transactions
        }
    }

    val blurRadius by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (selectedTransaction != null) 16.dp else 0.dp,
        label = "blur"
    )

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text("All Transactions", color = White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = White)
                    }
                },
                colors = TopAppBarDefaults.smallTopAppBarColors(
                    containerColor = BlackBackground
                )
            )
        },
        containerColor = BlackBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .blur(blurRadius) // Apply blur
        ) {
            // Filters
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filters) { filter ->
                    FilterChip(
                        selected = filter == selectedFilter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter, color = if(filter == selectedFilter) White else TextSecondary) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ElectricPurple,
                            containerColor = GlassSurface
                        ),
                        border = null
                    )
                }
            }

            // Transaction List
            LazyColumn(
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                if (filteredTransactions.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No transactions found", color = TextTertiary)
                        }
                    }
                } else {
                    items(filteredTransactions) { transaction ->
                        TransactionItem(
                            iconVector = if (transaction.type == "income") Icons.Default.ArrowDownward else Icons.Default.ShoppingCart,
                            title = transaction.merchant,
                            subtitle = java.text.SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(transaction.date)),
                            amount = (if(transaction.type == "expense") "-" else "+") + currencyFormatter.format(transaction.amount).replace("Rs.", "₹"),
                            isIncome = transaction.type == "income",
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                            onLongClick = { selectedTransaction = transaction }
                        )
                    }
                }
            }
        }
        
        if (selectedTransaction != null) {
            com.example.finance.ui.components.TransactionDetailsDialog(
                transaction = selectedTransaction!!,
                onDismiss = { selectedTransaction = null },
                onDelete = {
                    viewModel.deleteTransaction(selectedTransaction!!)
                    selectedTransaction = null
                }
            )
        }
    }
}
