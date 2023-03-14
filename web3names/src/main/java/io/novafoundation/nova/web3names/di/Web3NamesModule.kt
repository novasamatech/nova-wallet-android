package io.novafoundation.nova.web3names.di

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.web3names.domain.caip19.Caip19MatcherFactory
import io.novafoundation.nova.web3names.domain.caip19.Caip19Parser
import io.novafoundation.nova.web3names.domain.caip19.RealCaip19MatcherFactory
import io.novafoundation.nova.web3names.domain.caip19.repositories.Slip44CoinRepository

@Module
class Web3NamesModule {

    @Provides
    @FeatureScope
    fun provideSlip44CoinRepository(): Slip44CoinRepository {
        // TODO stub
        return object : Slip44CoinRepository {
            override fun getCoinCode(chainAsset: Chain.Asset): Int {
                TODO("Not yet implemented")
            }
        }
    }

    @Provides
    @FeatureScope
    fun provideCoip19MatcherFactory(
        slip44CoinRepository: Slip44CoinRepository
    ): Caip19MatcherFactory {
        return RealCaip19MatcherFactory(slip44CoinRepository)
    }

    fun provideCaip19Parser(): Caip19Parser {
        return Caip19Parser()
    }
}
