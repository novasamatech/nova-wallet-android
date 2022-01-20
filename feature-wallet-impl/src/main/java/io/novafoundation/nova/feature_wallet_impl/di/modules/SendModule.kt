package io.novafoundation.nova.feature_wallet_impl.di.modules

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.AssetTransfersProvider
import io.novafoundation.nova.feature_wallet_impl.domain.send.SendInteractor
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module
class SendModule {

    @Provides
    @FeatureScope
    fun provideSendInteractor(
        chainRegistry: ChainRegistry,
        walletRepository: WalletRepository,
        assetTransfersProvider: AssetTransfersProvider
    ) = SendInteractor(
        chainRegistry,
        walletRepository,
        assetTransfersProvider
    )
}
