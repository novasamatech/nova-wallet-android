package io.novafoundation.nova.feature_swap_impl.di.exchanges

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.crossChain.CrossChainTransferAssetExchangeFactory
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.CrossChainTransfersUseCase
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module
class CrossChainTransferExchangeModule {

    @Provides
    @FeatureScope
    fun provideAssetConversionExchangeFactory(
        crossChainTransfersUseCase: CrossChainTransfersUseCase,
        chainRegistry: ChainRegistry,
        accountRepository: AccountRepository
    ): CrossChainTransferAssetExchangeFactory {
        return CrossChainTransferAssetExchangeFactory(
            crossChainTransfersUseCase = crossChainTransfersUseCase,
            chainRegistry = chainRegistry,
            accountRepository = accountRepository
        )
    }
}
