package io.novafoundation.nova.feature_assets.di.modules

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_assets.domain.send.SendInteractor
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module
class SendModule {

    @Provides
    @FeatureScope
    fun provideSendInteractor(
        chainRegistry: ChainRegistry,
        walletRepository: WalletRepository,
        assetSourceRegistry: AssetSourceRegistry,
    ) = SendInteractor(
        chainRegistry,
        walletRepository,
        assetSourceRegistry
    )
}
