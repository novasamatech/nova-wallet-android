package io.novafoundation.nova.core_db.dao

import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import io.novafoundation.nova.common.utils.CollectionDiffer
import io.novafoundation.nova.core_db.model.CurrencyLocal

abstract class CurrencyDao {

    @Transaction
    suspend fun updateCurrencies(currencies: CollectionDiffer.Diff<CurrencyLocal>) {
        deleteCurrencies(currencies.removed)
        insertCurrencies(currencies.added)
        updateCurrencies(currencies.updated)

        if (getSelectedCurrency() == null) {
            selectCurrency(0)
        }
    }

    @Query("SELECT * FROM currencies")
    abstract suspend fun getCurrencies(): List<CurrencyLocal>

    abstract suspend fun getSelectedCurrency(): CurrencyLocal?

    @Query("UPDATE currencies SET selected = (id = :currencyId)")
    abstract fun selectCurrency(currencyId: Int)

    @Delete
    protected abstract suspend fun deleteCurrencies(currencies: List<CurrencyLocal>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    protected abstract suspend fun insertCurrencies(currencies: List<CurrencyLocal>)

    @Update
    protected abstract suspend fun updateCurrencies(currencies: List<CurrencyLocal>)
}
