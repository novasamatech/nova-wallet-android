package io.novafoundation.nova.web3names.di

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.data.network.NetworkApiCreator
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.runtime.di.REMOTE_STORAGE_SOURCE
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novafoundation.nova.web3names.data.caip19.Caip19MatcherFactory
import io.novafoundation.nova.web3names.data.caip19.Caip19Parser
import io.novafoundation.nova.web3names.data.caip19.RealCaip19MatcherFactory
import io.novafoundation.nova.web3names.data.caip19.repositories.Slip44CoinRepository
import io.novafoundation.nova.web3names.data.endpoints.TransferRecipientsApi
import io.novafoundation.nova.web3names.data.provider.RealWeb3NamesServiceChainIdProvider
import io.novafoundation.nova.web3names.data.provider.Web3NamesServiceChainIdProvider
import io.novafoundation.nova.web3names.data.repository.RealWeb3NamesRepository
import io.novafoundation.nova.web3names.domain.networking.RealWeb3NamesInteractor
import io.novafoundation.nova.web3names.domain.networking.Web3NamesInteractor
import io.novafoundation.nova.web3names.domain.repository.Web3NamesRepository
import javax.inject.Named

@Module
class Web3NamesModule {

    @Provides
    @FeatureScope
    fun provideSlip44CoinRepository(): Slip44CoinRepository {
        // TODO stub
        return object : Slip44CoinRepository {
            override fun getCoinCode(chainAsset: Chain.Asset): Int {
                return 354
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

    @Provides
    @FeatureScope
    fun provideCaip19Parser(): Caip19Parser {
        return Caip19Parser()
    }

    @Provides
    @FeatureScope
    fun provideWeb4NamesServiceChainIdProvider(): Web3NamesServiceChainIdProvider {
        // TODO change to ChainGeneses.KILT
        return RealWeb3NamesServiceChainIdProvider("a0c6e3bac382b316a68bca7141af1fba507207594c761076847ce358aeedcc21")
    }

    @Provides
    @FeatureScope
    fun provideTransferRecipientApi(
        networkApiCreator: NetworkApiCreator
    ): TransferRecipientsApi {
        return networkApiCreator.create(TransferRecipientsApi::class.java)
    }

    @Provides
    @FeatureScope
    fun provideWeb3NamesRepository(
        @Named(REMOTE_STORAGE_SOURCE) storageDataSource: StorageDataSource,
        web3NamesServiceChainIdProvider: Web3NamesServiceChainIdProvider,
        transferRecipientApi: TransferRecipientsApi,
        caip19MatcherFactory: Caip19MatcherFactory,
        caip19Parser: Caip19Parser
    ): Web3NamesRepository {
        return RealWeb3NamesRepository(
            storageDataSource,
            web3NamesServiceChainIdProvider,
            transferRecipientApi,
            caip19MatcherFactory,
            caip19Parser
        )
    }

    @Provides
    @FeatureScope
    fun provideWeb3NamesInteractor(
        web3NamesRepository: Web3NamesRepository
    ): Web3NamesInteractor {
        return RealWeb3NamesInteractor(
            web3NamesRepository
        )
    }
}
