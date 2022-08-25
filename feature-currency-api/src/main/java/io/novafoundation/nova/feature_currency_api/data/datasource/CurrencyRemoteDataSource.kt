package io.novafoundation.nova.feature_currency_api.data.datasource

import io.novafoundation.nova.feature_currency_api.presentation.model.CurrencyRemote

interface CurrencyRemoteDataSource {

    suspend fun getCurrenciesRemote(): List<CurrencyRemote>
}
