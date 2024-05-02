package io.novafoundation.nova.core_db.converters

import androidx.room.TypeConverter
import java.math.BigDecimal
import java.math.BigInteger

class LongMathConverters {

    @TypeConverter
    fun fromBigDecimal(balance: BigDecimal?): String? {
        return balance?.toString()
    }

    @TypeConverter
    fun toBigDecimal(balance: String?): BigDecimal? {
        return balance?.let { BigDecimal(it) }
    }

    @TypeConverter
    fun fromBigInteger(balance: BigInteger?): String? {
        return balance?.toString()
    }

    @TypeConverter
    fun toBigInteger(balance: String?): BigInteger? {
        return balance?.let {
            // When using aggregates like SUM in SQL queries, SQLite might return the result in a scientific notation especially if aggregation is done
            // BigInteger, which is stored as a string and SQLite casts it to REAL which causing the scientific notation on big numbers
            // This can be avoided by adjusting the query but we keep the fallback to BigDecimal parsing here anyways to avoid unpleasant crashes
            // It doesn't bring much impact since try-catch doesn't have an overhead unless the exception is thrown
            try {
                BigInteger(it)
            } catch (e: NumberFormatException) {
                BigDecimal(it).toBigInteger()
            }
        }
    }
}
