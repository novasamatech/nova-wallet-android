package io.novafoundation.nova.feature_xcm_impl.converter

import io.novafoundation.nova.common.utils.PalletName
import io.novafoundation.nova.feature_xcm_api.converter.MultiLocationConverter
import io.novafoundation.nova.feature_xcm_api.multiLocation.Junctions
import io.novafoundation.nova.feature_xcm_api.multiLocation.MultiLocation
import io.novafoundation.nova.feature_xcm_api.multiLocation.RelativeMultiLocation
import io.novafoundation.nova.feature_xcm_api.multiLocation.junctions
import io.novafoundation.nova.runtime.ext.palletNameOrDefault
import io.novafoundation.nova.runtime.ext.requireStatemine
import io.novafoundation.nova.runtime.ext.statemineOrNull
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.StatemineAssetId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.asNumberOrNull
import io.novafoundation.nova.runtime.multiNetwork.chain.model.asNumberOrThrow
import io.novasama.substrate_sdk_android.runtime.metadata.moduleOrNull
import java.math.BigInteger

private typealias LocalAssetsAssetId = BigInteger
private typealias LocalAssetsMappingKey = Pair<PalletName, LocalAssetsAssetId>
private typealias LocalAssetsMapping = Map<LocalAssetsMappingKey, Chain.Asset>

class LocalAssetsLocationConverter(
    private val chain: Chain,
    private val runtimeSource: RuntimeSource
) : MultiLocationConverter {

    private val assetIdToAssetMapping by lazy { constructAssetIdToAssetMapping() }

    override suspend fun toMultiLocation(chainAsset: Chain.Asset): RelativeMultiLocation? {
        if (chainAsset.chainId != chain.id) return null

        val assetsType = chainAsset.statemineOrNull() ?: return null
        // LocalAssets converter only supports number ids to use as GeneralIndex
        val index = assetsType.id.asNumberOrNull() ?: return null
        val pallet = runtimeSource.getRuntime().metadata.moduleOrNull(assetsType.palletNameOrDefault()) ?: return null

        return RelativeMultiLocation(
            parents = 0, // For Local Assets chain serves as a reserve
            interior = Junctions(
                MultiLocation.Junction.PalletInstance(pallet.index),
                MultiLocation.Junction.GeneralIndex(index)
            )
        )
    }

    override suspend fun toChainAsset(multiLocation: RelativeMultiLocation): Chain.Asset? {
        // We only consider local reserves for LocalAssets
        if (multiLocation.parents > 0) return null

        val junctions = multiLocation.junctions
        if (junctions.size != 2) return null

        val (maybePalletInstance, maybeGeneralIndex) = junctions
        if (maybePalletInstance !is MultiLocation.Junction.PalletInstance || maybeGeneralIndex !is MultiLocation.Junction.GeneralIndex) return null

        val pallet = runtimeSource.getRuntime().metadata.moduleOrNull(maybePalletInstance.index.toInt()) ?: return null
        val assetId = maybeGeneralIndex.index

        return assetIdToAssetMapping[pallet.name to assetId]
    }

    private fun constructAssetIdToAssetMapping(): LocalAssetsMapping {
        return chain.assets
            .filter {
                val type = it.type
                type is Chain.Asset.Type.Statemine && type.id is StatemineAssetId.Number
            }
            .associateBy { statemineAsset ->
                val assetsType = statemineAsset.requireStatemine()
                val palletName = assetsType.palletNameOrDefault()

                palletName to assetsType.id.asNumberOrThrow()
            }
    }
}
