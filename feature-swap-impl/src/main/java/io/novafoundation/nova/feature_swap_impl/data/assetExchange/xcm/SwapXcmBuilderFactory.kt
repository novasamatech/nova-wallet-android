package io.novafoundation.nova.feature_swap_impl.data.assetExchange.xcm

import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.dryRun.AssetIssuerRegistry
import io.novafoundation.nova.feature_xcm_api.builder.XcmBuilder
import io.novafoundation.nova.feature_xcm_api.versions.XcmVersion
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

class SwapXcmBuilderFactory(
    private val xcmBuilderFactory: XcmBuilder.Factory,
    private val assetIssuerRegistry: AssetIssuerRegistry,
    private val chainLocationConverter: XcmLocationConverter,
) {

    suspend fun create(
        initial: ChainId,
        xcmVersion: XcmVersion,
    ): SwapXcmBuilder {
        val proxy = xcmBuilderFactory.create(
            initial = chainLocationConverter.getChainLocation(initial),
            xcmVersion = xcmVersion,
            measureXcmFees = xcmBuilderFactory.dryRunMeasureFees(assetIssuerRegistry)
        )

        return ProxyingSwapXcmBuilder(proxy, chainLocationConverter)
    }
}
