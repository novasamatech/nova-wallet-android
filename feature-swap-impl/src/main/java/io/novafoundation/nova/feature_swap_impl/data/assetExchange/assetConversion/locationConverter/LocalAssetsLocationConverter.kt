package io.novafoundation.nova.feature_swap_impl.data.assetExchange.assetConversion.locationConverter

import io.novafoundation.nova.feature_wallet_api.domain.model.Junctions
import io.novafoundation.nova.feature_wallet_api.domain.model.MultiLocation
import io.novafoundation.nova.feature_wallet_api.domain.model.MultiLocation.Junction
import io.novafoundation.nova.feature_wallet_api.domain.model.junctionList
import io.novafoundation.nova.runtime.ext.palletNameOrDefault
import io.novafoundation.nova.runtime.ext.requireStatemine
import io.novafoundation.nova.runtime.ext.statemineOrNull
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import jp.co.soramitsu.fearless_utils.runtime.RuntimeSnapshot
import jp.co.soramitsu.fearless_utils.runtime.metadata.moduleOrNull
import java.math.BigInteger

private typealias PalletName = String
private typealias StatemineAssetId = BigInteger
private typealias MappingKey = Pair<PalletName, StatemineAssetId>
private typealias Mapping = Map<MappingKey, Chain.Asset>

class LocalAssetsLocationConverter(
    private val chain: Chain,
    private val runtime: RuntimeSnapshot
) : MultiLocationConverter {

    private val assetIdToAssetMapping by lazy { constructAssetIdToAssetMapping() }

    override suspend fun toMultiLocation(chainAsset: Chain.Asset): MultiLocation? {
        if (chainAsset.chainId != chain.id) return null

        val assetsType = chainAsset.statemineOrNull() ?: return null
        val pallet = runtime.metadata.moduleOrNull(assetsType.palletNameOrDefault()) ?: return null

        return MultiLocation(
            parents = BigInteger.ZERO, // For Local Assets chain serves as a reserve
            interior = Junctions(
                Junction.PalletInstance(pallet.index),
                Junction.GeneralIndex(assetsType.id)
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

        val pallet = runtime.metadata.moduleOrNull(maybePalletInstance.index.toInt()) ?: return null
        val assetId = maybeGeneralIndex.index

        return assetIdToAssetMapping[pallet.name to assetId]
    }

    private fun constructAssetIdToAssetMapping(): Mapping {
        return chain.assets
            .filter { it.type is Chain.Asset.Type.Statemine }
            .associateBy { statemineAsset ->
                val assetsType = statemineAsset.requireStatemine()
                val palletName = assetsType.palletNameOrDefault()

                palletName to assetsType.id
            }
    }
}
