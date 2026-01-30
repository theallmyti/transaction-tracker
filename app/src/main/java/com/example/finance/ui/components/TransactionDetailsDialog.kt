package com.example.finance.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.finance.data.db.Transaction
import com.example.finance.ui.theme.*

@Composable
fun TransactionDetailsDialog(
    transaction: Transaction,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        GlassmorphicCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 24.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Transaction Details",
                    style = Typography.titleLarge,
                    color = White
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                DetailRow("Merchant", transaction.merchant)
                DetailRow("Amount", "₹${transaction.amount}")
                DetailRow("Type", transaction.type.replaceFirstChar { it.uppercase() })
                DetailRow("Date", java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a").format(java.util.Date(transaction.date)))
                
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "SMS Body:",
                    style = Typography.bodyMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(BlackBackground.copy(alpha = 0.5f))
                        .padding(12.dp)
                ) {
                    Text(
                        text = transaction.description ?: "No SMS content",
                        style = Typography.bodySmall,
                        color = TextTertiary
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End
                ) {
                    TextButton(onClick = onDelete) {
                        Text("Delete", color = RoseRed)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = onDismiss) {
                        Text("Close", color = ElectricPurple)
                    }
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
    ) {
        Text(label, style = Typography.bodyMedium, color = TextTertiary)
        Text(value, style = Typography.bodyMedium, color = White)
    }
}
