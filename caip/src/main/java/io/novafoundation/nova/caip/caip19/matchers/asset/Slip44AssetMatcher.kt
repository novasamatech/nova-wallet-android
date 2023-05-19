package io.novafoundation.nova.caip.caip19.matchers.asset

import io.novafoundation.nova.caip.caip19.identifiers.AssetIdentifier

class Slip44AssetMatcher(
    private val assetSlip44CoinCode: Int,
) : AssetMatcher {

    override fun match(assetIdentifier: AssetIdentifier): Boolean {
        return assetIdentifier is AssetIdentifier.Slip44 &&
            assetSlip44CoinCode == assetIdentifier.slip44CoinCode
    }
}
