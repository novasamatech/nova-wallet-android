package io.novafoundation.nova.feature_currency_api.di

import io.novafoundation.nova.feature_currency_api.domain.CurrencyInteractor
import io.novafoundation.nova.feature_currency_api.domain.interfaces.CurrencyRepository

interface CurrencyFeatureApi {

    fun currencyInteractor(): CurrencyInteractor

    fun currencyRepository(): CurrencyRepository
}
