package io.novafoundation.nova.feature_swap_impl.data.assetExchange.assetConversion.locationConverter

import io.novafoundation.nova.feature_wallet_api.domain.model.MultiLocation
import io.novafoundation.nova.feature_wallet_api.domain.model.isHere
import io.novafoundation.nova.runtime.ext.isUtilityAsset
import io.novafoundation.nova.runtime.ext.relaychainAsNative
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigInteger

class NativeAssetLocationConverter(
    private val chain: Chain,
) : MultiLocationConverter {

    override suspend fun toMultiLocation(chainAsset: Chain.Asset): MultiLocation? {
        return if (chainAsset.chainId == chain.id && chainAsset.isUtilityAsset) {
            MultiLocation(
                parents = chain.expectedParentsInNativeInterior(),
                interior = MultiLocation.Interior.Here
            )
        } else {
            null
        }
    }

    override suspend fun toChainAsset(multiLocation: MultiLocation): Chain.Asset? {
        return if (chain.isNativeMultiLocation(multiLocation)) {
            chain.utilityAsset
        } else {
            null
        }
    }

    private fun Chain.expectedParentsInNativeInterior() : BigInteger {
        return if (additional.relaychainAsNative()) {
            BigInteger.ONE
        } else {
            BigInteger.ZERO
        }
    }

    private fun Chain.isNativeMultiLocation(multiLocation: MultiLocation): Boolean {
        return multiLocation.interior.isHere() && multiLocation.parents == expectedParentsInNativeInterior()
    }
}
