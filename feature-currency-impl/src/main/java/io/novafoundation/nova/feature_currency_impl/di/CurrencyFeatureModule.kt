package io.novafoundation.nova.feature_currency_impl.di

import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.core_db.dao.CurrencyDao
import io.novafoundation.nova.feature_currency_impl.data.datasource.CurrencyRemoteDataSource
import io.novafoundation.nova.feature_currency_api.domain.CurrencyInteractor
import io.novafoundation.nova.feature_currency_api.domain.interfaces.CurrencyRepository
import io.novafoundation.nova.feature_currency_impl.data.datasource.AssetsCurrencyRemoteDataSource
import io.novafoundation.nova.feature_currency_impl.data.repository.RealCurrencyRepository
import io.novafoundation.nova.feature_currency_impl.domain.CurrencyInteractorImpl

@Module
class CurrencyFeatureModule {

    @Provides
    @FeatureScope
    fun provideCurrencyRemoteDataSource(
        resourceManager: ResourceManager,
        gson: Gson
    ): CurrencyRemoteDataSource {
        return AssetsCurrencyRemoteDataSource(resourceManager, gson)
    }

    @Provides
    @FeatureScope
    fun provideCurrencyRepository(
        currencyDao: CurrencyDao,
        currencyRemoteDataSource: CurrencyRemoteDataSource
    ): CurrencyRepository {
        return RealCurrencyRepository(currencyDao, currencyRemoteDataSource)
    }

    @Provides
    @FeatureScope
    fun provideCurrencyInteractor(currencyRepository: CurrencyRepository): CurrencyInteractor {
        return CurrencyInteractorImpl(currencyRepository)
    }
}
