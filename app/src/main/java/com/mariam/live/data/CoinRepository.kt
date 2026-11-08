package com.mariam.live.data

import com.mariam.live.model.CoinTransaction

class CoinRepository {

    private val transactions = mutableListOf<CoinTransaction>()

    fun getTransactions(userId: String): List<CoinTransaction> {
        return transactions.filter { it.userId == userId }
    }

    fun addTransaction(transaction: CoinTransaction) {
        transactions.add(transaction)
    }

    fun getBalance(userId: String): Long {
        return transactions
            .filter { it.userId == userId }
            .sumOf { it.amount }
    }
}
