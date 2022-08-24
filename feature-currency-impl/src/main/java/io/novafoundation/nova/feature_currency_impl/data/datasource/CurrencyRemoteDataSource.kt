package io.novafoundation.nova.feature_currency_impl.data.datasource

import com.google.gson.Gson
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.fromJson
import io.novafoundation.nova.feature_currency_impl.R

class CurrencyRemote(
    val code: String,
    val name: String,
    val symbol: String?,
    val category: String,
    val popular: Boolean,
    val id: Int,
    val coingeckoId: String,
)

interface CurrencyRemoteDataSource {

    suspend fun getCurrenciesRemote(): List<CurrencyRemote>
}

class AssetsCurrencyRemoteDataSource(
    private val resourceManager: ResourceManager,
    private val gson: Gson
) : CurrencyRemoteDataSource {

    override suspend fun getCurrenciesRemote(): List<CurrencyRemote> {
        val rawCurrencies = resourceManager.loadRawString(R.raw.currencies)

        return gson.fromJson(rawCurrencies)
    }
}
