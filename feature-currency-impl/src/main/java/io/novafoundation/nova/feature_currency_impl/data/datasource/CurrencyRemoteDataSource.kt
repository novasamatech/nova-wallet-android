package io.novafoundation.nova.feature_currency_impl.data.datasource

interface CurrencyRemoteDataSource {

    suspend fun getCurrenciesRemote(): List<CurrencyRemote>
}
