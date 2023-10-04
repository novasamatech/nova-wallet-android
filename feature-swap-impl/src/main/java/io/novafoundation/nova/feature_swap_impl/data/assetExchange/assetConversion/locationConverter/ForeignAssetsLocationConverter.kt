package io.novafoundation.nova.feature_swap_impl.data.assetExchange.assetConversion.locationConverter

import io.novafoundation.nova.common.utils.toHexUntypedOrNull
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.bindMultiLocation
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.toEncodableInstance
import io.novafoundation.nova.feature_wallet_api.domain.model.MultiLocation
import io.novafoundation.nova.runtime.ext.requireStatemine
import io.novafoundation.nova.runtime.ext.statemineOrNull
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.StatemineAssetId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.asScaleEncodedOrThrow
import io.novafoundation.nova.runtime.multiNetwork.chain.model.isScaleEncoded
import io.novafoundation.nova.runtime.multiNetwork.chain.model.prepareIdForEncoding
import io.novafoundation.nova.runtime.multiNetwork.chain.model.statemineAssetIdScaleType
import jp.co.soramitsu.fearless_utils.runtime.RuntimeSnapshot

private typealias ScaleEncodedMultiLocation = String
private typealias ForeignAssetsAssetId = ScaleEncodedMultiLocation
private typealias ForeignAssetsMappingKey = ForeignAssetsAssetId // we only allow one pallet so no need to include pallet name into key
private typealias ForeignAssetsMapping = Map<ForeignAssetsMappingKey, Chain.Asset>

private const val FOREIGN_ASSETS_PALLET_NAME = "ForeignAssets"

class ForeignAssetsLocationConverter(
    private val chain: Chain,
    private val runtime: RuntimeSnapshot
) : MultiLocationConverter {

    private val assetIdToAssetMapping by lazy { constructAssetIdToAssetMapping() }

    override suspend fun toMultiLocation(chainAsset: Chain.Asset): MultiLocation? {
        if (chainAsset.chainId != chain.id) return null

        return chainAsset.extractMultiLocation()
    }

    override suspend fun toChainAsset(multiLocation: MultiLocation): Chain.Asset? {
        val assetIdType = statemineAssetIdScaleType(runtime, FOREIGN_ASSETS_PALLET_NAME) ?: return null
        val encodableInstance = multiLocation.toEncodableInstance()
        val multiLocationHex = assetIdType.toHexUntypedOrNull(runtime, encodableInstance) ?: return null

        return assetIdToAssetMapping[multiLocationHex]
    }

    private fun constructAssetIdToAssetMapping(): ForeignAssetsMapping {
        return chain.assets
            .filter {
                val type = it.type
                type is Chain.Asset.Type.Statemine
                    && type.palletName == FOREIGN_ASSETS_PALLET_NAME
                    && type.id is StatemineAssetId.ScaleEncoded
            }
            .associateBy { statemineAsset ->
                val assetsType = statemineAsset.requireStatemine()
                assetsType.id.asScaleEncodedOrThrow()
            }
    }

    private fun Chain.Asset.extractMultiLocation(): MultiLocation? {
        val assetsType = statemineOrNull() ?: return null
        if (!assetsType.id.isScaleEncoded()) return null

        return runCatching {
            val encodableMultiLocation = assetsType.prepareIdForEncoding(runtime)
            bindMultiLocation(encodableMultiLocation)
        }.getOrNull()
    }
}
