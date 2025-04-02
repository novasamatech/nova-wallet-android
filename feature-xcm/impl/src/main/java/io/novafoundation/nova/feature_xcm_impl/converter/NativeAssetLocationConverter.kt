package io.novafoundation.nova.feature_xcm_impl.converter

import io.novafoundation.nova.feature_xcm_api.converter.MultiLocationConverter
import io.novafoundation.nova.feature_xcm_api.multiLocation.MultiLocation
import io.novafoundation.nova.feature_xcm_api.multiLocation.RelativeMultiLocation
import io.novafoundation.nova.feature_xcm_api.multiLocation.isHere
import io.novafoundation.nova.runtime.ext.isUtilityAsset
import io.novafoundation.nova.runtime.ext.relaychainAsNative
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

internal class NativeAssetLocationConverter(
    private val chain: Chain,
) : MultiLocationConverter {

    override suspend fun toMultiLocation(chainAsset: Chain.Asset): RelativeMultiLocation? {
        return if (chainAsset.chainId == chain.id && chainAsset.isUtilityAsset) {
            RelativeMultiLocation(
                parents = chain.expectedParentsInNativeInterior(),
                interior = MultiLocation.Interior.Here
            )
        } else {
            null
        }
    }

    override suspend fun toChainAsset(multiLocation: RelativeMultiLocation): Chain.Asset? {
        return if (chain.isNativeMultiLocation(multiLocation)) {
            chain.utilityAsset
        } else {
            null
        }
    }

    private fun Chain.expectedParentsInNativeInterior(): Int {
        return if (additional.relaychainAsNative()) 1 else 0
    }

    private fun Chain.isNativeMultiLocation(multiLocation: RelativeMultiLocation): Boolean {
        return multiLocation.interior.isHere() && multiLocation.parents == expectedParentsInNativeInterior()
    }
}
