package io.novafoundation.nova.feature_wallet_impl.presentation.currency

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.list.headers.TextHeader
import io.novafoundation.nova.common.list.toListWithHeaders
import io.novafoundation.nova.common.navigation.ReturnableRouter
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.feature_wallet_api.domain.CurrencyCategory
import io.novafoundation.nova.feature_wallet_api.domain.CurrencyInteractor
import io.novafoundation.nova.feature_wallet_api.domain.model.Currency
import io.novafoundation.nova.feature_wallet_api.presentation.model.CurrencyModel
import io.novafoundation.nova.feature_wallet_impl.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SelectCurrencyViewModel(
    private val currencyInteractor: CurrencyInteractor,
    private val resourceManager: ResourceManager,
    private val router: ReturnableRouter,
) : BaseViewModel() {

    private val currencies = currencyInteractor.observeCurrencies()
        .inBackground()
        .share()

    val currencyModels = currencies.map { groupedList ->
        groupedList.toListWithHeaders(
            keyMapper = ::mapCurrencyCategoryToUI,
            valueMapper = { mapCurrencyToUI(it) }
        )
    }
        .inBackground()
        .share()

    private fun mapCurrencyCategoryToUI(category: CurrencyCategory): TextHeader {
        return TextHeader(
            when (category) {
                CurrencyCategory.CRYPTO -> resourceManager.getString(R.string.wallet_currency_category_cryptocurrencies)
                CurrencyCategory.FIAT -> resourceManager.getString(R.string.wallet_currency_category_feat)
                CurrencyCategory.FEAT_POPULAR -> resourceManager.getString(R.string.wallet_currency_category_popular_feat)
            }
        )
    }

    private fun mapCurrencyToUI(currency: Currency): CurrencyModel {
        return CurrencyModel(
            currency.id,
            currency.symbol ?: currency.code,
            currency.code,
            currency.name,
            currency.selected
        )
    }

    fun selectCurrency(currency: CurrencyModel) {
        launch {
            withContext(Dispatchers.IO) { currencyInteractor.selectCurrency(currency.id) }
            router.back()
        }
    }

    fun backClicked() {
        router.back()
    }
}
