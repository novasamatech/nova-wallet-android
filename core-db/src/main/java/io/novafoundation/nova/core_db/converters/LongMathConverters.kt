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
            // BigInteger.toString() may sometimes return value in scientific notation which cant be parsed by BigInteger constructor
            // I've got one such case but weren't able to reproduce it with exact same number
            // Ideally we need to research why and when it happens and maybe switch to more reliable bigInt -> string conversion
            // Meanwhile, we retry with BigDecimal constructor in case of failure
            try {
                BigInteger(it)
            } catch (e: NumberFormatException) {
                BigDecimal(it).toBigInteger()
            }
        }
    }
}
