package io.novafoundation.nova.caip.caip19.matchers.asset

import io.novafoundation.nova.caip.caip19.identifiers.AssetIdentifier

interface AssetMatcher {

    fun match(assetIdentifier: AssetIdentifier): Boolean
}
