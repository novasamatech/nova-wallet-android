package io.novafoundation.nova.web3names.domain.caip19.matchers.asset

import io.novafoundation.nova.web3names.domain.caip19.identifiers.AssetIdentifier

class UnsupportedAssetMatcher() : AssetMatcher {

    override fun match(assetIdentifier: AssetIdentifier): Boolean {
        return false
    }
}
