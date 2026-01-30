package com.example.finance.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.finance.ui.screens.DashboardScreen

@Composable
fun FinanceApp(viewModel: FinanceViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "dashboard") {
        composable("dashboard") {
            DashboardScreen(
                viewModel = viewModel,
                onNavigateToAnalytics = { navController.navigate("analytics") },
                onNavigateToAdd = { navController.navigate("add_transaction") },
                onNavigateToAllTransactions = { navController.navigate("all_transactions") }
            )
        }
        composable("analytics") {
            // AnalyticsScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable("add_transaction") {
            // AddTransactionScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable("all_transactions") {
            com.example.finance.ui.screens.AllTransactionsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
