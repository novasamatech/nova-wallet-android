package io.novafoundation.nova.feature_assets.di.modules

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_assets.domain.send.SendInteractor
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.crosschain.CrossChainTransactor
import io.novafoundation.nova.feature_wallet_api.data.network.crosschain.CrossChainTransfersRepository
import io.novafoundation.nova.feature_wallet_api.data.network.crosschain.CrossChainValidationSystemProvider
import io.novafoundation.nova.feature_wallet_api.domain.SendUseCase
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.CrossChainTransfersUseCase
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.runtime.repository.ParachainInfoRepository

@Module
class SendModule {

    @Provides
    @FeatureScope
    fun provideSendInteractor(
        assetSourceRegistry: AssetSourceRegistry,
        crossChainTransfersRepository: CrossChainTransfersRepository,
        crossChainTransactor: CrossChainTransactor,
        parachainInfoRepository: ParachainInfoRepository,
        extrinsicService: ExtrinsicService,
        sendUseCase: SendUseCase,
        crossChainTransfersUseCase: CrossChainTransfersUseCase,
        crossChainValidationProvider: CrossChainValidationSystemProvider
    ) = SendInteractor(
        assetSourceRegistry,
        crossChainTransactor,
        crossChainTransfersRepository,
        parachainInfoRepository,
        crossChainTransfersUseCase,
        extrinsicService,
        sendUseCase,
        crossChainValidationProvider
    )
}
