package io.novafoundation.nova.feature_swap_impl.data.assetExchange.assetConversion.locationConverter

import io.novafoundation.nova.common.utils.isZero
import io.novafoundation.nova.feature_wallet_api.domain.model.MultiLocation
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.ext.isUtilityAsset
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.math.BigInteger

class NativeAssetLocationConverter(
    private val chain: Chain,
    private val parentChain: Chain?,
) : MultiLocationConverter {

    /**
     * This is needed because we cannot uniquely identify multi-location for the native asset in the pure way with current interfaces
     * We either need to hold multi-location with chain-asset all along the way or make [NativeAssetLocationConverter] statefull to hold already processed
     * associations in in-memory cache
     * The later approach is chosen for its simplicity. Its main downside is that it implicitly relies on assumption that [toChainAsset] is called
     * before [toMultiLocation]. This is true at the moment, since [toChainAsset] is called at pool's resolution whereas [toMultiLocation] is called
     * at fee calculation and extrinsic construction
     */
    private val knownChainAssetMultiLocations = mutableMapOf<FullChainAssetId, MultiLocation>()
    private val knownChainAssetMultiLocationsMutex = Mutex()

    override suspend fun toMultiLocation(chainAsset: Chain.Asset): MultiLocation? {
        val known = getKnownLocation(chainAsset)
        if (known != null) return known

        return if (chainAsset.chainId == chain.id && chainAsset.isUtilityAsset) {
            MultiLocation(
                parents = BigInteger.ZERO,
                interior = MultiLocation.Interior.Here
            )
        } else {
            null
        }
    }

    override suspend fun toChainAsset(multiLocation: MultiLocation): Chain.Asset? {
        return if (multiLocation.isSelfReserve() || multiLocation.isSharedWithParentReserve()) {
            val chainAsset = chain.utilityAsset

            chainAsset.also { addKnownLocation(chainAsset, multiLocation) }
        } else {
            null
        }
    }

    private fun MultiLocation.isSelfReserve(): Boolean {
        return parents.isZero && interior is MultiLocation.Interior.Here
    }

    private fun MultiLocation.isSharedWithParentReserve(): Boolean {
        if (parentChain == null) return false

        val selfUtilitySymbol = chain.utilityAsset.symbol
        val parentUtilitySymbol = parentChain.utilityAsset.symbol

        val utilitySymbolSameWithParent = selfUtilitySymbol == parentUtilitySymbol

        return parents == BigInteger.ONE && interior  is MultiLocation.Interior.Here && utilitySymbolSameWithParent
    }

    private suspend fun addKnownLocation(chainAsset: Chain.Asset, location: MultiLocation) = knownChainAssetMultiLocationsMutex.withLock {
        knownChainAssetMultiLocations[chainAsset.fullId] = location
    }

    private suspend fun getKnownLocation(chainAsset: Chain.Asset) = knownChainAssetMultiLocationsMutex.withLock {
        knownChainAssetMultiLocations[chainAsset.fullId]
    }
}
