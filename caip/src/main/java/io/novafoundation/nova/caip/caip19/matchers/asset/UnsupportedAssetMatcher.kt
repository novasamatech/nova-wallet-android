package io.novafoundation.nova.caip.caip19.matchers.asset

import io.novafoundation.nova.caip.caip19.identifiers.AssetIdentifier

class UnsupportedAssetMatcher : AssetMatcher {

    override fun match(assetIdentifier: AssetIdentifier): Boolean {
        return false
    }
}
