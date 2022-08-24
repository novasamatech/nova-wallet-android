package io.novafoundation.nova.feature_currency_impl.di

import com.google.gson.Gson
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.core_db.dao.CurrencyDao

interface CurrencyFeatureDependencies {
    fun resourceManager(): ResourceManager

    fun currencyDao(): CurrencyDao

    fun jsonMapper(): Gson
}
