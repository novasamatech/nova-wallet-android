package io.novafoundation.nova.core_db.model

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import io.novafoundation.nova.common.utils.Identifiable

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
) : Identifiable {

    enum class Category {
        FIAT, CRYPTO
    }

    @Ignore
    override val identifier: String = id.toString()
}
