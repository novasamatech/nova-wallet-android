package io.novafoundation.nova.core_db.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "currencies"
)
data class CurrencyLocal(
    val code: String,
    val name: String,
    val symbol: String?,
    val category: Category,
    val popular: Boolean,
    @PrimaryKey val id: Int,
    val coingeckoId: String,
    val selected: Boolean,
) {

    enum class Category {
        FIAT, CRYPTO
    }
}
