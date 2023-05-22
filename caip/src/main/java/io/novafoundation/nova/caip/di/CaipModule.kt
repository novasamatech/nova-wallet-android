package io.novafoundation.nova.caip.di

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.caip.caip19.Caip19MatcherFactory
import io.novafoundation.nova.caip.caip19.Caip19Parser
import io.novafoundation.nova.caip.caip19.RealCaip19MatcherFactory
import io.novafoundation.nova.caip.caip19.RealCaip19Parser
import io.novafoundation.nova.caip.caip2.Caip2MatcherFactory
import io.novafoundation.nova.caip.caip2.Caip2Parser
import io.novafoundation.nova.caip.caip2.Caip2Resolver
import io.novafoundation.nova.caip.caip2.RealCaip2MatcherFactory
import io.novafoundation.nova.caip.caip2.RealCaip2Parser
import io.novafoundation.nova.caip.caip2.RealCaip2Resolver
import io.novafoundation.nova.caip.slip44.RealSlip44CoinRepository
import io.novafoundation.nova.caip.slip44.Slip44CoinRepository
import io.novafoundation.nova.caip.slip44.endpoint.Slip44CoinApi
import io.novafoundation.nova.caip.BuildConfig
import io.novafoundation.nova.common.data.network.NetworkApiCreator
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module
class CaipModule {

    @Provides
    @FeatureScope
    fun provideSlip44CoinApi(
        networkApiCreator: NetworkApiCreator
    ): Slip44CoinApi {
        return networkApiCreator.create(Slip44CoinApi::class.java)
    }

    @Provides
    @FeatureScope
    fun provideSlip44CoinRepository(slip44Api: Slip44CoinApi): Slip44CoinRepository {
        return RealSlip44CoinRepository(
            slip44Api = slip44Api,
            slip44CoinsUrl = BuildConfig.SLIP_44_COINS_BASE_URL
        )
    }

    @Provides
    @FeatureScope
    fun provideCaip2MatcherFactory(): Caip2MatcherFactory = RealCaip2MatcherFactory()

    @Provides
    @FeatureScope
    fun provideCaip2Parser(): Caip2Parser = RealCaip2Parser()

    @Provides
    @FeatureScope
    fun provideCaip2Resolver(chainRegistry: ChainRegistry): Caip2Resolver = RealCaip2Resolver(chainRegistry)

    @Provides
    @FeatureScope
    fun provideCaip19MatcherFactory(
        slip44CoinRepository: Slip44CoinRepository,
        caip2MatcherFactory: Caip2MatcherFactory,
    ): Caip19MatcherFactory {
        return RealCaip19MatcherFactory(slip44CoinRepository, caip2MatcherFactory)
    }

    @Provides
    @FeatureScope
    fun provideCaip19Parser(caip2Parser: Caip2Parser): Caip19Parser = RealCaip19Parser(caip2Parser)
}
