package io.novafoundation.nova.feature_assets.di.modules

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_assets.domain.send.SendInteractor
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.crosschain.CrossChainTransactor
import io.novafoundation.nova.feature_wallet_api.data.network.crosschain.CrossChainTransfersRepository
import io.novafoundation.nova.feature_wallet_api.data.network.crosschain.CrossChainWeigher
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.runtime.repository.ParachainInfoRepository

@Module
class SendModule {

    @Provides
    @FeatureScope
    fun provideSendInteractor(
        walletRepository: WalletRepository,
        assetSourceRegistry: AssetSourceRegistry,
        crossChainTransfersRepository: CrossChainTransfersRepository,
        crossChainWeigher: CrossChainWeigher,
        crossChainTransactor: CrossChainTransactor,
        parachainInfoRepository: ParachainInfoRepository,
        extrinsicService: ExtrinsicService,
    ) = SendInteractor(
        walletRepository,
        assetSourceRegistry,
        crossChainWeigher,
        crossChainTransactor,
        crossChainTransfersRepository,
        parachainInfoRepository,
        extrinsicService
    )
}
