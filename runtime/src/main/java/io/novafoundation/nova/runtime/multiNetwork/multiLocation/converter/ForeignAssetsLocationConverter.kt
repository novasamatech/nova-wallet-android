package io.novafoundation.nova.runtime.multiNetwork.multiLocation.converter

import io.novafoundation.nova.common.utils.lazyAsync
import io.novafoundation.nova.common.utils.toHexUntypedOrNull
import io.novafoundation.nova.runtime.ext.requireStatemine
import io.novafoundation.nova.runtime.ext.statemineOrNull
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.StatemineAssetId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.asScaleEncodedOrThrow
import io.novafoundation.nova.runtime.multiNetwork.chain.model.isScaleEncoded
import io.novafoundation.nova.runtime.multiNetwork.chain.model.prepareIdForEncoding
import io.novafoundation.nova.runtime.multiNetwork.chain.model.statemineAssetIdScaleType
import io.novafoundation.nova.runtime.multiNetwork.multiLocation.MultiLocation
import io.novafoundation.nova.runtime.multiNetwork.multiLocation.XcmVersion
import io.novafoundation.nova.runtime.multiNetwork.multiLocation.XcmVersionDetector
import io.novafoundation.nova.runtime.multiNetwork.multiLocation.bindMultiLocation
import io.novafoundation.nova.runtime.multiNetwork.multiLocation.orDefault
import io.novafoundation.nova.runtime.multiNetwork.multiLocation.toEncodableInstance
import io.novasama.substrate_sdk_android.runtime.definitions.types.RuntimeType

private typealias ScaleEncodedMultiLocation = String
private typealias ForeignAssetsAssetId = ScaleEncodedMultiLocation
private typealias ForeignAssetsMappingKey = ForeignAssetsAssetId // we only allow one pallet so no need to include pallet name into key
private typealias ForeignAssetsMapping = Map<ForeignAssetsMappingKey, Chain.Asset>

private const val FOREIGN_ASSETS_PALLET_NAME = "ForeignAssets"

internal class ForeignAssetsLocationConverter(
    private val chain: Chain,
    private val runtime: RuntimeSource,
    private val xcmVersionDetector: XcmVersionDetector,
) : MultiLocationConverter {

    private val assetIdToAssetMapping by lazy { constructAssetIdToAssetMapping() }

    private var assetIdEncodingContext = lazyAsync { constructAssetIdEncodingContext() }

    override suspend fun toMultiLocation(chainAsset: Chain.Asset): MultiLocation? {
        if (chainAsset.chainId != chain.id) return null

        return chainAsset.extractMultiLocation()
    }

    override suspend fun toChainAsset(multiLocation: MultiLocation): Chain.Asset? {
        val (xcmVersion, assetIdType) = assetIdEncodingContext.get() ?: return null

        val encodableInstance = multiLocation.toEncodableInstance(xcmVersion)
        val multiLocationHex = assetIdType.toHexUntypedOrNull(runtime.getRuntime(), encodableInstance) ?: return null

        return assetIdToAssetMapping[multiLocationHex]
    }

    private fun constructAssetIdToAssetMapping(): ForeignAssetsMapping {
        return chain.assets
            .filter {
                val type = it.type
                type is Chain.Asset.Type.Statemine &&
                    type.palletName == FOREIGN_ASSETS_PALLET_NAME &&
                    type.id is StatemineAssetId.ScaleEncoded
            }
            .associateBy { statemineAsset ->
                val assetsType = statemineAsset.requireStatemine()
                assetsType.id.asScaleEncodedOrThrow()
            }
    }

    private suspend fun Chain.Asset.extractMultiLocation(): MultiLocation? {
        val assetsType = statemineOrNull() ?: return null
        if (!assetsType.id.isScaleEncoded()) return null

        return runCatching {
            val encodableMultiLocation = assetsType.prepareIdForEncoding(runtime.getRuntime())
            bindMultiLocation(encodableMultiLocation)
        }.getOrNull()
    }

    private suspend fun constructAssetIdEncodingContext(): AssetIdEncodingContext? {
        val assetIdType = statemineAssetIdScaleType(runtime.getRuntime(), FOREIGN_ASSETS_PALLET_NAME) ?: return null
        val xcmVersion = xcmVersionDetector.detectMultiLocationVersion(chain.id, assetIdType).orDefault()

        return AssetIdEncodingContext(xcmVersion, assetIdType)
    }

    private data class AssetIdEncodingContext(
        val xcmVersion: XcmVersion,
        val assetIdType: RuntimeType<*, *>
    )
}
