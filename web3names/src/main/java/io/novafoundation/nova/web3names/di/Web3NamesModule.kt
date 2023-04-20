package io.novafoundation.nova.web3names.di

import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.data.network.NetworkApiCreator
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.runtime.di.REMOTE_STORAGE_SOURCE
import io.novafoundation.nova.runtime.ext.Geneses
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novafoundation.nova.web3names.BuildConfig
import io.novafoundation.nova.web3names.data.caip19.Caip19MatcherFactory
import io.novafoundation.nova.web3names.data.caip19.Caip19Parser
import io.novafoundation.nova.web3names.data.caip19.RealCaip19MatcherFactory
import io.novafoundation.nova.web3names.data.caip19.repositories.RealSlip44CoinRepository
import io.novafoundation.nova.web3names.data.caip19.repositories.Slip44CoinRepository
import io.novafoundation.nova.web3names.data.endpoints.Slip44CoinApi
import io.novafoundation.nova.web3names.data.endpoints.TransferRecipientsApi
import io.novafoundation.nova.web3names.data.integrity.RealWe3NamesIntegrityVerifier
import io.novafoundation.nova.web3names.data.integrity.Web3NamesIntegrityVerifier
import io.novafoundation.nova.web3names.data.provider.RealWeb3NamesServiceChainIdProvider
import io.novafoundation.nova.web3names.data.provider.Web3NamesServiceChainIdProvider
import io.novafoundation.nova.web3names.data.repository.RealWeb3NamesRepository
import io.novafoundation.nova.web3names.data.repository.Web3NamesRepository
import io.novafoundation.nova.web3names.domain.networking.RealWeb3NamesInteractor
import io.novafoundation.nova.web3names.domain.networking.Web3NamesInteractor
import javax.inject.Named

@Module
class Web3NamesModule {

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
        return RealSlip44CoinRepository(slip44Api, BuildConfig.SLIP_44_COINS_BASE_URL)
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
        val chainId = if (BuildConfig.DEBUG) {
            // TODO we should use kilt mainnet in debug as well after all corner-cases will be tested on testnet
            Chain.Geneses.KILT_TESTNET
        } else {
            Chain.Geneses.KILT
        }

        return RealWeb3NamesServiceChainIdProvider(chainId)
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
    fun provideWe3NamesIntegrityVerifier(): Web3NamesIntegrityVerifier {
        return RealWe3NamesIntegrityVerifier()
    }

    @Provides
    @FeatureScope
    fun provideWeb3NamesRepository(
        @Named(REMOTE_STORAGE_SOURCE) storageDataSource: StorageDataSource,
        web3NamesServiceChainIdProvider: Web3NamesServiceChainIdProvider,
        transferRecipientApi: TransferRecipientsApi,
        caip19MatcherFactory: Caip19MatcherFactory,
        caip19Parser: Caip19Parser,
        we3NamesIntegrityVerifier: Web3NamesIntegrityVerifier,
        gson: Gson,
    ): Web3NamesRepository {
        return RealWeb3NamesRepository(
            storageDataSource,
            web3NamesServiceChainIdProvider,
            transferRecipientApi,
            caip19MatcherFactory,
            caip19Parser,
            we3NamesIntegrityVerifier,
            gson
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
