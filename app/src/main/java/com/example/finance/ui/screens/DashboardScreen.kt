package com.example.finance.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.finance.data.db.Transaction
import com.example.finance.ui.FinanceViewModel
import com.example.finance.ui.components.BalanceDisplay
import com.example.finance.ui.components.GlassmorphicCard
import com.example.finance.ui.components.TransactionItem
import com.example.finance.ui.theme.*
import com.patrykandpatrick.vico.compose.axis.horizontal.bottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.startAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.core.entry.entryModelOf
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

@Composable
fun DashboardScreen(
    viewModel: FinanceViewModel,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToAdd: () -> Unit,
    onNavigateToAllTransactions: () -> Unit
) {
    // collected as "mainBalance" to be clear
    val mainBalance by viewModel.totalBalance.collectAsState()
    val sliceBalance by viewModel.sliceBalance.collectAsState()
    
    val transactions by viewModel.allTransactions.collectAsState()
    val updateInfo by viewModel.updateInfo.collectAsState()
    val graphData by viewModel.graphData.collectAsState()
    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    currencyFormatter.maximumFractionDigits = 0
    
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // Combined Total
    val combinedBalance = mainBalance + sliceBalance
    
    // Title Logic
    val balanceTitle = if (combinedBalance < 0) "Total Spend" else "Total Balance"
    val displayCombined = abs(combinedBalance)
    
    var selectedTransaction by remember { mutableStateOf<Transaction?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(updateInfo) {
        if (updateInfo != null) {
            showUpdateDialog = true
        }
    }

    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
        onResult = { isGranted: Boolean ->
            if (isGranted) {
                viewModel.scanHistoricalSms(context)
            } else {
                android.widget.Toast.makeText(context, "SMS Permission needed to scan transactions", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    )

    androidx.compose.runtime.LaunchedEffect(Unit) {
        val permissionCheckResult = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_SMS
        )
        if (permissionCheckResult == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            viewModel.scanHistoricalSms(context)
        } else {
            launcher.launch(android.Manifest.permission.READ_SMS)
        }
    }

    var showNotificationDialog by remember { mutableStateOf(false) }
    
    // Manage notification state
    var notifications by remember { mutableStateOf(listOf(
        com.example.finance.ui.components.NotificationData("Welcome!", "Thanks for using Transaction Tracker.", "Just now"),
        com.example.finance.ui.components.NotificationData("SMS Scan Complete", "Your transactions are up to date.", "2 mins ago")
    )) }
    var hasUnreadNotifications by remember { mutableStateOf(true) }

    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    val blurRadius by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (showAddDialog || selectedTransaction != null || showNotificationDialog || showUpdateDialog) 16.dp else 0.dp,
        label = "blur"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BlackBackground)
            .windowInsetsPadding(WindowInsets.systemBars)
            .pointerInput(Unit) {
                var totalDrag = 0f
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (totalDrag < -100) { 
                             onNavigateToAllTransactions()
                        }
                        totalDrag = 0f
                    }
                ) { change, dragAmount ->
                    totalDrag += dragAmount
                }
            } 
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .blur(blurRadius), // Apply blur here
            contentPadding = PaddingValues(bottom = 80.dp) 
        ) {
            // Header
            item {
                TopBar(
                    onAnalyticsClick = onNavigateToAnalytics,
                    onClearData = { viewModel.clearAllData() },
                    onNotificationClick = { 
                        showNotificationDialog = true
                        hasUnreadNotifications = false
                    },
                    hasUnreadNotifications = hasUnreadNotifications
                )
            }

            // Combined Balance (Top)
            item {
                Spacer(modifier = Modifier.height(32.dp))
                BalanceDisplay(
                    amount = currencyFormatter.format(displayCombined),
                    modifier = Modifier.fillMaxWidth(),
                    title = balanceTitle 
                )
            }

            // Quick Actions
            item {
                Spacer(modifier = Modifier.height(24.dp))
                QuickActionsRow(
                    onAddClick = { showAddDialog = true },
                    onScanClick = { 
                        scope.launch {
                            snackbarHostState.showSnackbar("Scanning for transactions...")
                        }
                        viewModel.scanHistoricalSms(context) 
                    },
                    onSendClick = {
                        scope.launch {
                            snackbarHostState.showSnackbar("Send feature coming soon")
                        }
                    },
                    onReceiveClick = {
                        scope.launch {
                            snackbarHostState.showSnackbar("Receive feature coming soon")
                        }
                    }
                )
            }

            // Graph Section
            item {
                Spacer(modifier = Modifier.height(32.dp)) 
                
                GlassmorphicCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                        .padding(horizontal = 16.dp)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onLongPress = { viewModel.toggleGraphMode() }
                            )
                        }
                        .pointerInput(Unit) {
                            var dragSum = 0f
                            detectHorizontalDragGestures(
                                onDragEnd = {
                                    if (dragSum > 100) { // Swipe Right -> Prev
                                        viewModel.incrementDateOffset(-1)
                                    } else if (dragSum < -100) { // Swipe Left -> Next
                                        viewModel.incrementDateOffset(1)
                                    }
                                    dragSum = 0f
                                }
                            ) { change, dragAmount ->
                                change.consume() 
                                dragSum += dragAmount
                            }
                        },
                    cornerRadius = 16.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Spending: ${graphData.label}",
                            style = Typography.titleMedium,
                            color = White
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        if (graphData.points.isNotEmpty()) {
                            val chartEntryModel = entryModelOf(*graphData.points.toTypedArray())
                            Chart(
                                chart = lineChart(
                                    lines = listOf(
                                        com.patrykandpatrick.vico.compose.chart.line.lineSpec(
                                            lineColor = androidx.compose.ui.graphics.Color(0xFF8B5CF6),
                                            lineBackgroundShader = com.patrykandpatrick.vico.compose.component.shape.shader.verticalGradient(
                                                colors = arrayOf(
                                                    androidx.compose.ui.graphics.Color(0xFF8B5CF6).copy(alpha = 0.4f), 
                                                    androidx.compose.ui.graphics.Color.Transparent
                                                )
                                            )
                                        )
                                    )
                                ),
                                model = chartEntryModel,
                                startAxis = startAxis(
                                    maxLabelCount = 5,
                                    valueFormatter = { value, _ -> 
                                        if (value >= 1000) {
                                             val k = value / 1000
                                             if (k % 1.0 == 0.0) "₹${k.toInt()}K" else String.format(java.util.Locale.US, "₹%.1fK", k)
                                         } else {
                                             "₹${value.toInt()}"
                                         }
                                    }
                                ),
                                bottomAxis = bottomAxis(
                                    guideline = null,
                                    valueFormatter = { value, _ -> "${value.toInt() + 1}" }
                                ),
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No data for this period", color = TextTertiary)
                            }
                        }
                    }
                }
            }

            // Accounts Breakdown (Below Graph)
            item {
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = "Accounts",
                    style = Typography.titleMedium,
                    color = White,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                GlassmorphicCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    cornerRadius = 16.dp
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        // Main Account
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Main Account", style = Typography.bodyLarge, color = White)
                            Text(
                                currencyFormatter.format(abs(mainBalance)), 
                                style = Typography.bodyLarge, 
                                color = if(mainBalance < 0) RoseRed else EmeraldGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Divider(
                            color = White.copy(alpha = 0.1f), 
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                        
                        // Slice Account
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Slice Account", style = Typography.bodyLarge, color = White)
                            Text(
                                currencyFormatter.format(abs(sliceBalance)), 
                                style = Typography.bodyLarge, 
                                color = if(sliceBalance < 0) RoseRed else EmeraldGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
        
        // Dialogs
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

        if (showNotificationDialog) {
            com.example.finance.ui.components.NotificationDialog(
                notifications = notifications,
                onDismiss = { 
                    showNotificationDialog = false
                    notifications = emptyList() // Clear notifications after viewing
                }
            )
        }
        
        if (showAddDialog) {
            com.example.finance.ui.components.AddTransactionDialog(
                onDismiss = { showAddDialog = false },
                onSave = { amount, merchant, type ->
                    val transaction = Transaction(
                        amount = amount,
                        merchant = merchant,
                        type = type,
                        category = "Manual",
                        date = System.currentTimeMillis(),
                        description = "Manual Entry",
                        referenceId = java.util.UUID.randomUUID().toString(),
                        isAutoCaptured = false,
                        account = "Main" // Default to Main
                    )
                    viewModel.addTransaction(transaction)
                }
            )
        }

        // Custom Snackbar
        androidx.compose.material3.SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        ) { data ->
             GlassmorphicCard(
                 modifier = Modifier.padding(16.dp),
                 cornerRadius = 12.dp
             ) {
                 Text(
                     text = data.visuals.message,
                     color = White,
                     modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                     style = Typography.bodyMedium
                 )
             }
        }

        // Update Dialog
        if (showUpdateDialog && updateInfo != null) {
            androidx.compose.ui.window.Dialog(onDismissRequest = { showUpdateDialog = false }) {
                GlassmorphicCard(
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("New Update Available! \uD83D\uDE80", style = Typography.titleLarge, color = White)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Version ${updateInfo!!.latestVersion} is now available.", 
                            color = TextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        androidx.compose.material3.Button(
                            onClick = {
                                try {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(updateInfo!!.downloadUrl))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = ElectricPurple)
                        ) {
                            Text("Download Update")
                        }
                        androidx.compose.material3.TextButton(onClick = { showUpdateDialog = false }) {
                            Text("Later", color = TextTertiary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TopBar(
    onAnalyticsClick: () -> Unit, 
    onClearData: () -> Unit, 
    onNotificationClick: () -> Unit,
    hasUnreadNotifications: Boolean
) {
    var showMenu by remember { mutableStateOf(false) }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            IconButton(onClick = { showMenu = true }) {
                Icon(
                    imageVector = Icons.Default.GridView,
                    contentDescription = "Menu",
                    tint = White,
                    modifier = Modifier.size(28.dp)
                )
            }
            androidx.compose.material3.DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier.background(GlassSurface)
            ) {
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text("Clear All Data", color = RoseRed) },
                    onClick = {
                        onClearData()
                        showMenu = false
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = RoseRed)
                    }
                )
            }
        }

        Box {
            IconButton(onClick = onNotificationClick) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Notifications",
                    tint = White,
                    modifier = Modifier.size(28.dp)
                )
            }
            // Red dot
            if (hasUnreadNotifications) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp) // Adjust padding to sit on top of the icon button
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(RoseRed)
                )
            }
        }
    }
}

@Composable
fun QuickActionsRow(
    onAddClick: () -> Unit, 
    onScanClick: () -> Unit,
    onSendClick: () -> Unit,
    onReceiveClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly // Changed to SpaceEvenly for 4 buttons
    ) {
        QuickActionButton(Icons.Default.ArrowOutward, "Send", onClick = onSendClick)
        QuickActionButton(Icons.Default.ArrowDownward, "Receive", onClick = onReceiveClick) // Restored Receive
        QuickActionButton(Icons.Default.Sync, "Scan SMS", onClick = onScanClick)
        QuickActionButton(Icons.Default.Add, "Add", onClick = onAddClick)
    }
}

@Composable
fun QuickActionButton(icon: ImageVector, label: String, onClick: () -> Unit = {}) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        GlassmorphicCard(
            modifier = Modifier
                .size(72.dp)
                .clickable(onClick = onClick),
            cornerRadius = 36.dp // Circle
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(imageVector = icon, contentDescription = label, tint = White)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(label, style = Typography.bodySmall)
    }
}

// Marker removed due to version compatibility issues. Will re-implement later.
@Composable
fun Placeholder() {}
