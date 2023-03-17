package io.novafoundation.nova.web3names.domain.caip19.matchers.asset

import io.novafoundation.nova.web3names.domain.caip19.identifiers.AssetIdentifier

interface AssetMatcher {

    fun match(assetIdentifier: AssetIdentifier): Boolean
}
