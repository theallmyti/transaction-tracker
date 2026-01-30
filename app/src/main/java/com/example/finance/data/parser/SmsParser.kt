package com.example.finance.data.parser

import com.example.finance.data.db.Transaction
import java.util.regex.Pattern

object SmsParser {

    fun parseSms(sender: String, body: String, date: Long): Transaction? {
        // 1. Basic Filter
        // Allow testing from arbitrary numbers if needed, or standard bank headers
        // Real bank SMS usually have headers like "AX-HDFCBK"
        // We permit generic ones for now to ensure we catch the user's messages
        if (body.length < 10) return null
        
        val lowerBody = body.lowercase()
        
        // Filter out future-tense notifications (e.g., "Rs.89 will be deducted")
        // Filter out future-tense notifications
        // Targeted regex for the specific "Upcoming mandate" message and other future-tense warnings
        val ignorePattern = Regex("(?i)(will\\s+be\\s+(?:deducted|debited)|upcoming\\s+mandate|execution\\s+for\\s+the\\s+same|request\\s+received\\s+(?:to|for)\\s+debit|verification\\s+code|otp)")
        if (ignorePattern.containsMatchIn(body)) return null
        var type = "expense"
        var amount = 0.0

        // 2. Amount & Type Detection
        // Strategy A: "Sent Rs. 40" (Keyword ... Amount)
        val amountRegexA = Regex("(?i)(?:sent|spent|paid|debited|credited|received|deposited|transferred).*?(?:rs\\.?|inr)\\s*([0-9,]+(?:\\.[0-9]+)?)")
        var match = amountRegexA.find(body)

        // Strategy B: "Rs. 66 Credited" (Amount ... Keyword) - Common in Bank of Baroda
        if (match == null) {
            val amountRegexB = Regex("(?i)(?:rs\\.?|inr)\\s*([0-9,]+(?:\\.[0-9]+)?).*?(?:credited|debited|transferred|deposited)")
            match = amountRegexB.find(body)
        }

        if (match != null) {
            val amountStr = match!!.groupValues[1].replace(",", "")
            amount = amountStr.toDoubleOrNull() ?: return null
            
            // Determine type
            if (lowerBody.contains("credited") || lowerBody.contains("received") || lowerBody.contains("deposited")) {
                type = "income"
            } else if (lowerBody.contains("debited") || lowerBody.contains("spent") || lowerBody.contains("paid") || lowerBody.contains("sent") || lowerBody.contains("transferred")) {
                type = "expense"
            }
        } else {
             return null
        }

        // 3. Extract Merchant
        // Patterns:
        // 1. "to SAINATH" / "at AMAZON"
        // 2. "by LIC..." (IncomeSource)
        // 3. "to:UPI/..."
        var merchant = "Unknown"
        
        // Try 'to:' or 'by' or 'at' or 'vp'
        val merchantRegex = Regex("(?i)(?:to|at|vp|by)\\s*[:]?\\s*([a-zA-Z0-9\\s/\\-_]+)")
        val merchantMatch = merchantRegex.find(body)
        
        if (merchantMatch != null) {
             var extracted = merchantMatch.groupValues[1].trim()
             
             // Cleanup: Stop at common delimiters like " thru", " on", " ref", " bal"
             val stopWords = listOf(" thru", " on", " ref", " bal", " dated", " from")
             var lowestIndex = extracted.length
             
             for (word in stopWords) {
                 val idx = extracted.lowercase().indexOf(word)
                 if (idx != -1 && idx < lowestIndex) {
                     lowestIndex = idx
                 }
             }
             
             extracted = extracted.substring(0, lowestIndex).trim()
             if (extracted.isNotEmpty()) {
                 merchant = extracted
             }
        }
        
        // Final sanity check: Ignore OTP messages which often contain amounts
        if (lowerBody.contains("otp") || lowerBody.contains("verification code")) return null

        // 4. Extract Reference ID
        // "Ref 638677073472"
        var refId: String? = null
        val refRegex = Regex("(?i)(?:ref|txn|no)\\s*[:.]?\\s*([a-zA-Z0-9]+)")
        val refMatch = refRegex.find(body)
        if (refMatch != null) {
            refId = refMatch.groupValues[1]
        }

        // Auto-categorize
        val category = categorizeMerchant(merchant)
        
        // Generate a deterministic ID to prevent duplicates on re-scan
        // Seed with sender, body, and date so the same SMS always produces the same ID
        val uniqueContent = "$sender$body$date"
        val deterministicId = java.util.UUID.nameUUIDFromBytes(uniqueContent.toByteArray()).toString()

        // Detect Account (Slice vs Main)
        var account = "Main"
        if (body.contains("slice", ignoreCase = true) || sender.contains("slice", ignoreCase = true)) {
            account = "Slice"
        }

        return Transaction(
            id = deterministicId,
            amount = amount,
            type = type,
            category = category,
            merchant = merchant,
            description = body,
            date = date,
            referenceId = refId ?: "GEN-${deterministicId}", 
            isAutoCaptured = true,
            account = account
        )
    }

    private fun categorizeMerchant(merchant: String): String {
        val m = merchant.uppercase()
        return when {
            m.contains("SWIGGY") || m.contains("ZOMATO") || m.contains("MC DONALES") || m.contains("CANTEEN") || m.contains("FOOD") -> "Food"
            m.contains("UBER") || m.contains("OLA") || m.contains("PETROL") || m.contains("FUEL") -> "Transport"
            m.contains("AMAZON") || m.contains("FLIPKART") || m.contains("MYNTRA") || m.contains("SHOP") -> "Shopping"
            m.contains("NETFLIX") || m.contains("SPOTIFY") || m.contains("MOVIE") -> "Entertainment"
            m.contains("UPI") -> "Transfer"
            else -> "Others"
        }
    }
}
