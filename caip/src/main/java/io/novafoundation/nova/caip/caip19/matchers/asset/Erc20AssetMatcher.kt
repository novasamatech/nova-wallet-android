package io.novafoundation.nova.caip.caip19.matchers.asset

import io.novafoundation.nova.caip.caip19.identifiers.AssetIdentifier
import io.novafoundation.nova.caip.caip19.identifiers.AssetIdentifier.Erc20

class Erc20AssetMatcher(private val contractAddress: String) : AssetMatcher {

    override fun match(assetIdentifier: AssetIdentifier): Boolean {
        return assetIdentifier is Erc20 && assetIdentifier.contractAddress == contractAddress
    }
}
