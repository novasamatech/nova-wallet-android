package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets

import io.novafoundation.nova.common.data.network.runtime.binding.bindNumberOrNull
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.HydraDxAssetId
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.HydraDxAssetIdConverter
import io.novafoundation.nova.runtime.ext.decodeOrNull
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot
import java.math.BigInteger

private val SYSTEM_ON_CHAIN_ASSET_ID = BigInteger.ZERO

internal class RealHydraDxAssetIdConverter(
    private val chainRegistry: ChainRegistry
) : HydraDxAssetIdConverter {

    override val systemAssetId: HydraDxAssetId = SYSTEM_ON_CHAIN_ASSET_ID

    override suspend fun toOnChainIdOrNull(chainAsset: Chain.Asset): HydraDxAssetId? {
        val runtime = chainRegistry.getRuntime(chainAsset.chainId)
        return chainAsset.omniPoolTokenIdOrNull(runtime)
    }

    override suspend fun toChainAssetOrNull(chain: Chain, onChainId: HydraDxAssetId): Chain.Asset? {
        val runtime = chainRegistry.getRuntime(chain.id)

        return chain.assets.find { chainAsset ->
            val omniPoolId = chainAsset.omniPoolTokenIdOrNull(runtime)

            omniPoolId == onChainId
        }
    }

    override suspend fun allOnChainIds(chain: Chain): Map<HydraDxAssetId, Chain.Asset> {
        val runtime = chainRegistry.getRuntime(chain.id)

        return chain.assets.mapNotNull { chainAsset ->
            chainAsset.omniPoolTokenIdOrNull(runtime)?.let { it to chainAsset }
        }.toMap()
    }

    private fun Chain.Asset.omniPoolTokenIdOrNull(runtimeSnapshot: RuntimeSnapshot): HydraDxAssetId? {
        return when (val type = type) {
            is Chain.Asset.Type.Orml -> bindNumberOrNull(type.decodeOrNull(runtimeSnapshot))
            is Chain.Asset.Type.Native -> systemAssetId
            else -> null
        }
    }

    private fun Chain.Asset.requireHydraDxAssetId(runtimeSnapshot: RuntimeSnapshot): HydraDxAssetId {
        return requireNotNull(omniPoolTokenIdOrNull(runtimeSnapshot))
    }
}
