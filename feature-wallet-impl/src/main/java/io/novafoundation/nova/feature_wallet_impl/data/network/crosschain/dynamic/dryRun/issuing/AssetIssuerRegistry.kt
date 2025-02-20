package io.novafoundation.nova.feature_wallet_impl.data.network.crosschain.dynamic.dryRun.issuing

import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_wallet_api.data.repository.StatemineAssetsRepository
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.Type
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import javax.inject.Inject

interface AssetIssuerRegistry {

    suspend fun create(chainAsset: Chain.Asset): AssetIssuer
}

@FeatureScope
class RealAssetIssuerRegistry @Inject constructor(
    private val chainRegistry: ChainRegistry,
    private val statemineAssetsRepository: StatemineAssetsRepository,
): AssetIssuerRegistry {

    override suspend fun create(chainAsset: Chain.Asset): AssetIssuer {
        val runtime = chainRegistry.getRuntime(chainAsset.chainId)

        return when(val type = chainAsset.type) {
            is Type.Native -> NativeAssetIssuer(runtime)
            is Type.Statemine -> StatemineAssetIssuer(chainAsset.chainId, type, runtime, statemineAssetsRepository)
            is Type.Orml -> OrmlAssetIssuer(type, runtime)
            else -> error("Unsupported asset type: $type for ${chainAsset.symbol} on ${chainAsset.chainId}")
        }
    }
}
