package com.example.finance.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.finance.ui.theme.TextPrimary
import com.example.finance.ui.theme.TextSecondary
import com.example.finance.ui.theme.TextTertiary
import com.example.finance.ui.theme.Typography
import com.example.finance.ui.theme.White

@Composable
fun BalanceDisplay(
    amount: String,
    modifier: Modifier = Modifier,
    title: String = "Total Balance"
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = Typography.bodyMedium,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = amount,
            style = Typography.displayLarge,
            color = White
        )
    }
}
