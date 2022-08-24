package io.novafoundation.nova.feature_currency_api.di

import io.novafoundation.nova.feature_currency_api.domain.CurrencyInteractor

interface CurrencyFeatureApi {
    fun currencyInteractor(): CurrencyInteractor
}
