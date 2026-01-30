package com.example.finance

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.finance.data.db.AppDatabase
import com.example.finance.data.repository.FinanceRepository
import com.example.finance.ui.FinanceApp
import com.example.finance.ui.FinanceViewModel
import com.example.finance.ui.FinanceViewModelFactory
import com.example.finance.ui.theme.FinanceTrackerTheme

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Handle permissions
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = AppDatabase.getDatabase(this)
        val repository = FinanceRepository(database.transactionDao())
        val viewModelFactory = FinanceViewModelFactory(repository)
        val viewModel = ViewModelProvider(this, viewModelFactory)[FinanceViewModel::class.java]

        checkSmsPermissions()

        setContent {
            FinanceTrackerTheme {
                FinanceApp(viewModel)
            }
        }
    }

    private fun checkSmsPermissions() {
        val permissions = mutableListOf(
            android.Manifest.permission.RECEIVE_SMS,
            android.Manifest.permission.READ_SMS
        )
        // Add POST_NOTIFICATIONS for Android 13+
        if (Build.VERSION.SDK_INT >= 33) {
            permissions.add(android.Manifest.permission.POST_NOTIFICATIONS)
        }

        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            requestPermissionLauncher.launch(missingPermissions.toTypedArray())
        }
    }
}
