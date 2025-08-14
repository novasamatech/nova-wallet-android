package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.orml.hydrationEvm

import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.domain.validation.EnoughTotalToStayAboveEDValidationFactory
import io.novafoundation.nova.feature_wallet_api.domain.validation.PhishingValidationFactory
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.orml.OrmlAssetTransfers
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import javax.inject.Inject

@FeatureScope
class HydrationEvmAssetTransfers @Inject constructor(
    chainRegistry: ChainRegistry,
    assetSourceRegistry: AssetSourceRegistry,
    extrinsicServiceFactory: ExtrinsicService.Factory,
    phishingValidationFactory: PhishingValidationFactory,
    enoughTotalToStayAboveEDValidationFactory: EnoughTotalToStayAboveEDValidationFactory
): OrmlAssetTransfers(
    chainRegistry = chainRegistry,
    assetSourceRegistry = assetSourceRegistry,
    extrinsicServiceFactory = extrinsicServiceFactory,
    phishingValidationFactory = phishingValidationFactory,
    enoughTotalToStayAboveEDValidationFactory = enoughTotalToStayAboveEDValidationFactory
) {

    // Force Hydration Evm implementation to always use Currencies.transfer
    // Since Tokens.transfer fail for such tokens
    override val transferFunctions = listOf(
        Modules.CURRENCIES to "transfer",
    )
}
