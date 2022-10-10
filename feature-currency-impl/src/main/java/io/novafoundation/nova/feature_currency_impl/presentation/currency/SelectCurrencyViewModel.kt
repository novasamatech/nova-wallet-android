package io.novafoundation.nova.feature_currency_impl.presentation.currency

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.list.headers.TextHeader
import io.novafoundation.nova.common.list.toListWithHeaders
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.feature_currency_api.domain.CurrencyCategory
import io.novafoundation.nova.feature_currency_api.presentation.CurrencyRouter
import io.novafoundation.nova.feature_currency_api.presentation.mapper.mapCurrencyToUI
import io.novafoundation.nova.feature_currency_api.presentation.model.CurrencyModel
import io.novafoundation.nova.feature_currency_impl.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SelectCurrencyViewModel(
    private val currencyInteractor: io.novafoundation.nova.feature_currency_api.domain.CurrencyInteractor,
    private val resourceManager: ResourceManager,
    private val router: CurrencyRouter,
) : BaseViewModel() {

    private val currencies = currencyInteractor.observeCurrencies()
        .inBackground()
        .share()

    val currencyModels = currencies.map { groupedList ->
        groupedList.toListWithHeaders(
            keyMapper = { category, _ -> mapCurrencyCategoryToUI(category) },
            valueMapper = { mapCurrencyToUI(it) }
        )
    }
        .inBackground()
        .share()

    private fun mapCurrencyCategoryToUI(category: CurrencyCategory): TextHeader {
        return TextHeader(
            when (category) {
                CurrencyCategory.CRYPTO -> resourceManager.getString(R.string.wallet_currency_category_cryptocurrencies)
                CurrencyCategory.FIAT -> resourceManager.getString(R.string.wallet_currency_category_fiat)
                CurrencyCategory.FIAT_POPULAR -> resourceManager.getString(R.string.wallet_currency_category_popular_fiat)
            }
        )
    }

    fun selectCurrency(currency: CurrencyModel) {
        launch {
            withContext(Dispatchers.IO) { currencyInteractor.selectCurrency(currency.id) }
            router.returnToWallet()
        }
    }

    fun backClicked() {
        router.back()
    }
}
