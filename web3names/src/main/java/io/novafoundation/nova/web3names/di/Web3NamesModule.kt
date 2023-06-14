package io.novafoundation.nova.web3names.di

import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import io.novafoundation.nova.caip.caip19.Caip19MatcherFactory
import io.novafoundation.nova.caip.caip19.Caip19Parser
import io.novafoundation.nova.common.data.network.NetworkApiCreator
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.runtime.di.REMOTE_STORAGE_SOURCE
import io.novafoundation.nova.runtime.ext.Geneses
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novafoundation.nova.web3names.data.endpoints.TransferRecipientsApi
import io.novafoundation.nova.web3names.data.provider.RealWeb3NamesServiceChainIdProvider
import io.novafoundation.nova.web3names.data.provider.Web3NamesServiceChainIdProvider
import io.novafoundation.nova.web3names.data.repository.RealWeb3NamesRepository
import io.novafoundation.nova.web3names.data.repository.Web3NamesRepository
import io.novafoundation.nova.web3names.data.serviceEndpoint.W3NServiceEndpointHandlerFactory
import io.novafoundation.nova.web3names.domain.networking.RealWeb3NamesInteractor
import io.novafoundation.nova.web3names.domain.networking.Web3NamesInteractor
import javax.inject.Named

@Module
class Web3NamesModule {

    @Provides
    @FeatureScope
    fun provideWeb3NamesServiceChainIdProvider(): Web3NamesServiceChainIdProvider {
        return RealWeb3NamesServiceChainIdProvider(Chain.Geneses.KILT)
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
    fun provideW3NServiceEndpointHandlerFactory(
        transferRecipientApi: TransferRecipientsApi,
        gson: Gson
    ) = W3NServiceEndpointHandlerFactory(
        transferRecipientApi,
        gson
    )

    @Provides
    @FeatureScope
    fun provideWeb3NamesRepository(
        @Named(REMOTE_STORAGE_SOURCE) storageDataSource: StorageDataSource,
        web3NamesServiceChainIdProvider: Web3NamesServiceChainIdProvider,
        caip19MatcherFactory: Caip19MatcherFactory,
        caip19Parser: Caip19Parser,
        w3NServiceEndpointHandlerFactory: W3NServiceEndpointHandlerFactory
    ): Web3NamesRepository {
        return RealWeb3NamesRepository(
            storageDataSource,
            web3NamesServiceChainIdProvider,
            caip19MatcherFactory,
            caip19Parser,
            w3NServiceEndpointHandlerFactory
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
