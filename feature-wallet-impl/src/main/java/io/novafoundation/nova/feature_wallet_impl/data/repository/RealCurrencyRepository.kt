package io.novafoundation.nova.feature_wallet_impl.data.repository

import io.novafoundation.nova.core_db.dao.CurrencyDao
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.CurrencyRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.Currency
import io.novafoundation.nova.feature_wallet_impl.data.datasource.CurrencyRemoteDataSource
import kotlinx.coroutines.flow.Flow

class RealCurrencyRepository(
    private val currencyDao: CurrencyDao,
    private val currencyRemoteDataSource: CurrencyRemoteDataSource,
): CurrencyRepository {

    override suspend fun syncCurrencies() {
        return
    }

    override suspend fun observeCurrencies(): Flow<List<Currency>> {
        TODO("Not yet implemented")
    }

    override suspend fun observeSelectCurrency(): Flow<Currency> {
        TODO("Not yet implemented")
    }

    override suspend fun selectCurrency(currencyId: Int) {
        TODO("Not yet implemented")
    }
}
