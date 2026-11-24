package io.novafoundation.nova.feature_swap_impl.di

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.data.network.NetworkApiCreator
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_swap_impl.data.exchanger.ExchangerAApi
import io.novafoundation.nova.feature_swap_impl.data.exchanger.ExchangerAAssetsRepository
import io.novafoundation.nova.feature_swap_impl.data.exchanger.ExchangerAQuoteRepository
import io.novafoundation.nova.feature_swap_impl.data.exchanger.ExchangerAQuoteSource
import io.novafoundation.nova.feature_swap_impl.domain.exchanger.ExchangerAAmountConstraints
import io.novafoundation.nova.feature_swap_impl.domain.exchanger.ExchangerATaoInteractor
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.infrastructure.InfrastructureUrls
import io.novafoundation.nova.infrastructure.di.Attested

@Module
class ExchangerAModule {

    @Provides
    @FeatureScope
    fun provideExchangerAAssetsRepository(source: ExchangerAQuoteSource, chainRegistry: ChainRegistry): ExchangerAAssetsRepository {
        return ExchangerAAssetsRepository(source, chainRegistry)
    }

    @Provides
    @FeatureScope
    fun provideExchangerATaoInteractor(assetsRepository: ExchangerAAssetsRepository): ExchangerATaoInteractor {
        return ExchangerATaoInteractor(assetsRepository)
    }

    @Provides
    @FeatureScope
    fun provideExchangerAAmountConstraints(): ExchangerAAmountConstraints {
        return ExchangerAAmountConstraints()
    }

    @Provides
    @FeatureScope
    fun provideExchangerAQuoteRepository(source: ExchangerAQuoteSource, amountConstraints: ExchangerAAmountConstraints): ExchangerAQuoteRepository {
        return ExchangerAQuoteRepository(source, amountConstraints)
    }

    @Provides
    @FeatureScope
    fun provideExchangerAApi(@Attested networkApiCreator: NetworkApiCreator): ExchangerAApi {
        return networkApiCreator.create(ExchangerAApi::class.java)
    }

    @Provides
    @FeatureScope
    fun provideExchangerAQuoteSource(api: ExchangerAApi, infrastructureUrls: InfrastructureUrls): ExchangerAQuoteSource {
        return ExchangerAQuoteSource(api, infrastructureUrls)
    }
}
