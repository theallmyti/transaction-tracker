package com.example.finance.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.finance.ui.theme.*

data class NotificationData(
    val title: String,
    val message: String,
    val time: String
)

@Composable
fun NotificationDialog(
    notifications: List<NotificationData>,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        GlassmorphicCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "Notifications",
                    style = MaterialTheme.typography.headlineSmall,
                    color = White
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (notifications.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No new notifications",
                            color = TextTertiary,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyColumn {
                        items(notifications.size) { index ->
                            val item = notifications[index]
                            NotificationItem(
                                title = item.title,
                                message = item.message,
                                time = item.time
                            )
                            if (index < notifications.lastIndex) {
                                Divider(
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    color = TextSecondary.copy(alpha = 0.2f),
                                    thickness = 1.dp
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Close", color = ElectricPurple)
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationItem(title: String, message: String, time: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Default.Notifications,
            contentDescription = null,
            tint = ElectricPurple,
            modifier = Modifier.size(20.dp).padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = title, color = White, fontSize = 14.sp)
            Text(text = message, color = TextSecondary, fontSize = 12.sp)
            Text(text = time, color = TextSecondary.copy(alpha = 0.5f), fontSize = 10.sp)
        }
    }
}
