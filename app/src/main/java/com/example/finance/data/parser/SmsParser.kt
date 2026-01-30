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
        // Pattern to catch "Sent Rs.40.00" or "Spent Rs 500" or "Credited Rs 1000"
        // We use non-capturing groups for the keywords
        val amountRegex = Regex("(?i)(?:sent|spent|paid|debited|credited|received|deposited).*?(?:rs\\.?|inr)\\s*([0-9,]+(?:\\.[0-9]+)?)")
        val match = amountRegex.find(body)

        if (match != null) {
            val amountStr = match.groupValues[1].replace(",", "")
            amount = amountStr.toDoubleOrNull() ?: return null
            
            // Determine type based on keywords present in the MATCHED string or the whole body
            // We check the whole regular expression match to see which keyword triggered it, or just content check
            if (lowerBody.contains("credited") || lowerBody.contains("received") || lowerBody.contains("deposited")) {
                type = "income"
            } else {
                // Default to expense for Sent/Spent/Paid/Debited
                type = "expense"
            }
        } else {
            // Fallback for just "Rs. 40.00" without explicit keyword close by? 
            // The user's format starts with "Sent Rs.40.00", so the above regex should catch it.
            return null
        }

        // 3. Extract Merchant
        // Structure: "To SAINATHCANTEEN one" or "at AMAZON"
        // We look for 'To' or 'At' followed by text until end of line or specific keywords
        var merchant = "Unknown"
        val merchantRegex = Regex("(?i)(?:to|at|vp)\\s+([a-zA-Z0-9\\s]+)")
        val merchantMatch = merchantRegex.find(body)
        if (merchantMatch != null) {
            // Take the captured group
            var extracted = merchantMatch.groupValues[1].trim()
            // Clean up if it grabbed too much (e.g. up to 'On')
            val splitOnKeywords = extracted.split(Regex("(?i)\\s+(on|ref|dated|from|bal)"))
            if (splitOnKeywords.isNotEmpty()) {
                extracted = splitOnKeywords[0]
            }
            merchant = extracted.trim()
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
