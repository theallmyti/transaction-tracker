package com.example.finance.data.repository

import com.example.finance.data.db.Transaction
import com.example.finance.data.db.TransactionDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FinanceRepository(private val transactionDao: TransactionDao) {

    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()
    
    val totalBalance: Flow<Double> = transactionDao.getAllTransactions().map { list ->
        list.sumOf { if (it.type == "income") it.amount else -it.amount }
    }

    fun getTransactionsInRange(startDate: Long, endDate: Long): Flow<List<Transaction>> {
        return transactionDao.getTransactionsInRange(startDate, endDate)
    }

    suspend fun insert(transaction: Transaction) {
        // We rely on the deterministic ID generated in SmsParser and OnConflictStrategy.REPLACE
        // in the DAO to handle deduplication logic.
        transactionDao.insertTransaction(transaction)
    }

    suspend fun delete(transaction: Transaction) {
        transactionDao.deleteTransaction(transaction)
    }

    suspend fun deleteAll() {
        transactionDao.deleteAll()
    }
}
