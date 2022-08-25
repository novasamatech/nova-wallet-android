package io.novafoundation.nova.feature_currency_impl.domain

import io.novafoundation.nova.common.list.GroupedList
import io.novafoundation.nova.feature_currency_api.domain.CurrencyCategory
import io.novafoundation.nova.feature_currency_api.domain.CurrencyInteractor
import io.novafoundation.nova.feature_currency_api.domain.interfaces.CurrencyRepository
import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CurrencyInteractorImpl(
    private val currencyRepository: CurrencyRepository
) : CurrencyInteractor {
    override suspend fun syncCurrencies() {
        currencyRepository.syncCurrencies()
    }

    override fun observeCurrencies(): Flow<GroupedList<CurrencyCategory, Currency>> {
        return currencyRepository.observeCurrencies().map { list ->
            list.groupBy { mapCurrencyCategory(it) }
                .toSortedMap(getCurrencyCategoryComparator())
        }
    }

    override fun observeSelectCurrency(): Flow<Currency> {
        return currencyRepository.observeSelectCurrency()
    }

    override suspend fun getSelectedCurrency(): Currency {
        return currencyRepository.getSelectedCurrency()
    }

    override suspend fun selectCurrency(currencyId: Int) {
        currencyRepository.selectCurrency(currencyId)
    }

    private fun getCurrencyCategoryComparator() = compareBy<CurrencyCategory> {
        when (it) {
            CurrencyCategory.CRYPTO -> 0
            CurrencyCategory.FIAT_POPULAR -> 1
            CurrencyCategory.FIAT -> 2
        }
    }

    private fun mapCurrencyCategory(category: Currency): CurrencyCategory {
        return when (category.category) {
            Currency.Category.CRYPTO -> CurrencyCategory.CRYPTO
            Currency.Category.FIAT -> {
                if (category.popular) {
                    CurrencyCategory.FIAT_POPULAR
                } else {
                    CurrencyCategory.FIAT
                }
            }
        }
    }
}
