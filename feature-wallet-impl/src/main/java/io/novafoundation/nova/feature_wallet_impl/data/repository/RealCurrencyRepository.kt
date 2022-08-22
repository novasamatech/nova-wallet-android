package io.novafoundation.nova.feature_wallet_impl.data.repository

import io.novafoundation.nova.common.utils.CollectionDiffer
import io.novafoundation.nova.core_db.dao.CurrencyDao
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.CurrencyRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.Currency
import io.novafoundation.nova.feature_wallet_impl.data.datasource.CurrencyRemoteDataSource
import io.novafoundation.nova.feature_wallet_impl.data.mappers.mapCurrencyFromLocal
import io.novafoundation.nova.feature_wallet_impl.data.mappers.mapRemoteCurrencyToLocal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RealCurrencyRepository(
    private val currencyDao: CurrencyDao,
    private val currencyRemoteDataSource: CurrencyRemoteDataSource,
) : CurrencyRepository {

    override suspend fun syncCurrencies() {
        val selectedCurrency = currencyDao.getSelectedCurrency()
        val remoteCurrencies = currencyRemoteDataSource.getCurrenciesRemote()
        val newCurrencies = remoteCurrencies.map { mapRemoteCurrencyToLocal(it, selectedCurrency?.id == it.id) }
        val oldCurrencies = currencyDao.getCurrencies()
        val resultCurrencies = CollectionDiffer.findDiff(newCurrencies, oldCurrencies, true)
        currencyDao.updateCurrencies(resultCurrencies)
    }

    override fun observeCurrencies(): Flow<List<Currency>> {
        return currencyDao.observeCurrencies()
            .map { currencyList -> currencyList.map { mapCurrencyFromLocal(it) } }
    }

    override fun observeSelectCurrency(): Flow<Currency> {
        return currencyDao.observeSelectCurrency()
            .map { mapCurrencyFromLocal(it) }
    }

    override suspend fun selectCurrency(currencyId: Int) {
        currencyDao.selectCurrency(currencyId)
    }
}
