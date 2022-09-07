package io.novafoundation.nova.feature_currency_api.presentation

import io.novafoundation.nova.common.navigation.ReturnableRouter

interface CurrencyRouter : ReturnableRouter {

    fun returnToWallet()
}
