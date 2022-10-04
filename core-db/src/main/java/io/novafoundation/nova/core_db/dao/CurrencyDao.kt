package io.novafoundation.nova.core_db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import io.novafoundation.nova.common.utils.CollectionDiffer
import io.novafoundation.nova.core_db.model.CurrencyLocal
import kotlinx.coroutines.flow.Flow

private const val RETRIEVE_CURRENCIES = "SELECT * FROM currencies"

private const val RETRIEVE_SELECTED_CURRENCY = "SELECT * FROM currencies WHERE selected = 1"

@Dao
abstract class CurrencyDao {

    @Transaction
    open suspend fun updateCurrencies(currencies: CollectionDiffer.Diff<CurrencyLocal>) {
        deleteCurrencies(currencies.removed)
        insertCurrencies(currencies.added)
        updateCurrencies(currencies.updated)

        if (getSelectedCurrency() == null) {
            selectCurrency(0)
        }
    }

    @Query("SELECT * FROM currencies WHERE id = 0")
    abstract fun getFirst(): CurrencyLocal

    @Query(RETRIEVE_CURRENCIES)
    abstract suspend fun getCurrencies(): List<CurrencyLocal>

    @Query(RETRIEVE_CURRENCIES)
    abstract fun observeCurrencies(): Flow<List<CurrencyLocal>>

    @Query(RETRIEVE_SELECTED_CURRENCY)
    abstract suspend fun getSelectedCurrency(): CurrencyLocal?

    @Query(RETRIEVE_SELECTED_CURRENCY)
    abstract fun observeSelectCurrency(): Flow<CurrencyLocal>

    @Query("UPDATE currencies SET selected = (id = :currencyId)")
    abstract fun selectCurrency(currencyId: Int)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun insert(currency: CurrencyLocal)

    @Delete
    protected abstract suspend fun deleteCurrencies(currencies: List<CurrencyLocal>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    protected abstract suspend fun insertCurrencies(currencies: List<CurrencyLocal>)

    @Update
    protected abstract suspend fun updateCurrencies(currencies: List<CurrencyLocal>)
}
