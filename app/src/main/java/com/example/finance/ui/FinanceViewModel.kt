package com.example.finance.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.finance.data.db.Transaction
import com.example.finance.data.repository.FinanceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class UpdateInfo(val isAvailable: Boolean, val downloadUrl: String, val latestVersion: String)
data class GraphResult(val label: String, val points: List<Float>)

class FinanceViewModel(private val repository: FinanceRepository) : ViewModel() {

    val allTransactions: StateFlow<List<Transaction>> = repository.allTransactions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Main Balance (Excluding Slice)
    // We assume 'Main' is default. But we check NOT Slice to be safe if 'account' logic is partial?
    // User wants "Slice don't count". So Main = Total - Slice?
    // Or filter for "Main".
    // Since I added 'account' field, I'll trust it.
    // If account field is missing (compilation error?), I must ensuring Transaction.kt IS updated.
    
    val totalBalance: StateFlow<Double> = allTransactions.map { list ->
        list.filter { it.account == "Main" }.sumOf { 
            if (it.type == "income") it.amount else -it.amount 
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    // Slice Balance
    val sliceBalance: StateFlow<Double> = allTransactions.map { list ->
        list.filter { it.account == "Slice" }.sumOf { 
            if (it.type == "income") it.amount else -it.amount 
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    // Update Logic
    private val _updateInfo = MutableStateFlow<UpdateInfo?>(null)
    val updateInfo: StateFlow<UpdateInfo?> = _updateInfo.asStateFlow()

    // Graph Logic
    private val _isMonthlyMode = MutableStateFlow(true)
    val isMonthlyMode: StateFlow<Boolean> = _isMonthlyMode.asStateFlow()
    
    private val _dateOffset = MutableStateFlow(0)
    val dateOffset: StateFlow<Int> = _dateOffset.asStateFlow()

    val graphData = combine(
        allTransactions, 
        _isMonthlyMode, 
        _dateOffset
    ) { transactions, isMonthly, offset ->
        // Filter for Main only
        val relevantTransactions = transactions.filter { it.account == "Main" }

        val calendar = java.util.Calendar.getInstance()
        val dateFormat = java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.getDefault())
        val yearFormat = java.text.SimpleDateFormat("yyyy", java.util.Locale.getDefault())

        if (isMonthly) {
            calendar.add(java.util.Calendar.MONTH, offset)
            val targetMonth = calendar.get(java.util.Calendar.MONTH)
            val targetYear = calendar.get(java.util.Calendar.YEAR)
            val label = dateFormat.format(calendar.time)
            
            val maxDays = calendar.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
            val dailyExpenses = FloatArray(maxDays) { 0f }
            
            relevantTransactions.filter { 
                it.type == "expense" 
            }.forEach {
                val cal = java.util.Calendar.getInstance()
                cal.timeInMillis = it.date
                if (cal.get(java.util.Calendar.MONTH) == targetMonth && 
                    cal.get(java.util.Calendar.YEAR) == targetYear) {
                    val day = cal.get(java.util.Calendar.DAY_OF_MONTH) - 1
                    if (day in 0 until maxDays) {
                        dailyExpenses[day] += it.amount.toFloat()
                    }
                }
            }
            GraphResult(label, dailyExpenses.toList())
        } else {
            calendar.add(java.util.Calendar.YEAR, offset)
            val targetYear = calendar.get(java.util.Calendar.YEAR)
            val label = yearFormat.format(calendar.time)
            
            val monthlyExpenses = FloatArray(12) { 0f }
            
            relevantTransactions.filter { 
                it.type == "expense" 
            }.forEach {
                val cal = java.util.Calendar.getInstance()
                cal.timeInMillis = it.date
                if (cal.get(java.util.Calendar.YEAR) == targetYear) {
                    val month = cal.get(java.util.Calendar.MONTH) // 0-11
                    monthlyExpenses[month] += it.amount.toFloat()
                }
            }
            GraphResult(label, monthlyExpenses.toList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GraphResult("", emptyList()))

    init {
        checkForUpdates()
    }
    
    fun toggleGraphMode() {
        _isMonthlyMode.value = !_isMonthlyMode.value
        _dateOffset.value = 0 
    }
    
    fun incrementDateOffset(delta: Int) {
        _dateOffset.value += delta
    }

    private fun checkForUpdates() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Real GitHub Raw URL
                val url = java.net.URL("https://raw.githubusercontent.com/theallmyti/transaction-tracker/main/version.json")
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.requestMethod = "GET"
                
                if (connection.responseCode == 200) {
                    val stream = connection.inputStream
                    val reader = java.io.BufferedReader(java.io.InputStreamReader(stream))
                    val response = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        response.append(line)
                    }
                    reader.close()
                    
                    val jsonString = response.toString()
                    val json = org.json.JSONObject(jsonString)
                    val remoteVersionCode = json.getInt("versionCode")
                    val downloadUrl = json.getString("downloadUrl")
                    val latestVersionName = json.getString("versionName")
                    
                    val currentVersionCode = com.example.finance.BuildConfig.VERSION_CODE 
                    
                    if (remoteVersionCode > currentVersionCode) {
                         _updateInfo.value = UpdateInfo(true, downloadUrl, latestVersionName)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun addTransaction(transaction: Transaction) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insert(transaction)
        }
    }
    
    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.delete(transaction)
        }
    }

    fun clearAllData() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteAll()
        }
    }

    fun scanHistoricalSms(context: android.content.Context) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val cr = context.contentResolver
            val uri = android.provider.Telephony.Sms.Inbox.CONTENT_URI
            val projection = arrayOf("address", "body", "date")
            
            // Filter: Current Year Only
            val calendar = java.util.Calendar.getInstance()
            calendar.set(java.util.Calendar.MONTH, 0)
            calendar.set(java.util.Calendar.DAY_OF_MONTH, 1)
            calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
            calendar.set(java.util.Calendar.MINUTE, 0)
            calendar.set(java.util.Calendar.SECOND, 0)
            calendar.set(java.util.Calendar.MILLISECOND, 0)
            val startOfYear = calendar.timeInMillis
            
            val selection = "date >= ?"
            val selectionArgs = arrayOf(startOfYear.toString())
            val sortOrder = "date DESC"

            try {
                val cursor = cr.query(uri, projection, selection, selectionArgs, sortOrder)
                cursor?.use {
                    val addressIdx = it.getColumnIndex("address")
                    val bodyIdx = it.getColumnIndex("body")
                    val dateIdx = it.getColumnIndex("date")

                    while (it.moveToNext()) {
                        val address = it.getString(addressIdx)
                        val body = it.getString(bodyIdx)
                        val date = it.getLong(dateIdx)

                        val transaction = com.example.finance.data.parser.SmsParser.parseSms(address, body, date)
                        if (transaction != null) {
                            repository.insert(transaction)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

class FinanceViewModelFactory(private val repository: FinanceRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FinanceViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FinanceViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
