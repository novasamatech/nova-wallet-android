package io.novafoundation.nova.runtime.multiNetwork.multiLocation.converter

import io.novafoundation.nova.common.utils.PalletName
import io.novafoundation.nova.common.utils.invoke
import io.novafoundation.nova.runtime.ext.palletNameOrDefault
import io.novafoundation.nova.runtime.ext.requireStatemine
import io.novafoundation.nova.runtime.ext.statemineOrNull
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.StatemineAssetId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.asNumberOrNull
import io.novafoundation.nova.runtime.multiNetwork.chain.model.asNumberOrThrow
import io.novafoundation.nova.runtime.multiNetwork.multiLocation.Junctions
import io.novafoundation.nova.runtime.multiNetwork.multiLocation.MultiLocation
import io.novafoundation.nova.runtime.multiNetwork.multiLocation.MultiLocation.Junction
import io.novafoundation.nova.runtime.multiNetwork.multiLocation.junctionList
import jp.co.soramitsu.fearless_utils.runtime.RuntimeSnapshot
import jp.co.soramitsu.fearless_utils.runtime.metadata.moduleOrNull
import kotlinx.coroutines.Deferred
import java.math.BigInteger

private typealias LocalAssetsAssetId = BigInteger
private typealias LocalAssetsMappingKey = Pair<PalletName, LocalAssetsAssetId>
private typealias LocalAssetsMapping = Map<LocalAssetsMappingKey, Chain.Asset>

class LocalAssetsLocationConverter(
    private val chain: Chain,
    private val runtime: Deferred<RuntimeSnapshot>
) : MultiLocationConverter {

    private val assetIdToAssetMapping by lazy { constructAssetIdToAssetMapping() }

    override suspend fun toMultiLocation(chainAsset: Chain.Asset): MultiLocation? {
        if (chainAsset.chainId != chain.id) return null

        val assetsType = chainAsset.statemineOrNull() ?: return null
        // LocalAssets converter only supports number ids to use as GeneralIndex
        val index = assetsType.id.asNumberOrNull() ?: return null
        val pallet = runtime().metadata.moduleOrNull(assetsType.palletNameOrDefault()) ?: return null

        return MultiLocation(
            parents = BigInteger.ZERO, // For Local Assets chain serves as a reserve
            interior = Junctions(
                Junction.PalletInstance(pallet.index),
                Junction.GeneralIndex(index)
            )
        )
    }

    override suspend fun toChainAsset(multiLocation: MultiLocation): Chain.Asset? {
        // We only consider local reserves for LocalAssets
        if (multiLocation.parents > BigInteger.ZERO) return null

        val junctions = multiLocation.interior.junctionList
        if (junctions.size != 2) return null

        val (maybePalletInstance, maybeGeneralIndex) = junctions
        if (maybePalletInstance !is Junction.PalletInstance || maybeGeneralIndex !is Junction.GeneralIndex) return null

        val pallet = runtime().metadata.moduleOrNull(maybePalletInstance.index.toInt()) ?: return null
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
