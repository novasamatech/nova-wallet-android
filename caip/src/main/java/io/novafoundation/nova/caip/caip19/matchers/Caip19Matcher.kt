package io.novafoundation.nova.caip.caip19.matchers

import io.novafoundation.nova.caip.caip19.identifiers.Caip19Identifier
import io.novafoundation.nova.caip.caip19.matchers.asset.AssetMatcher
import io.novafoundation.nova.caip.caip2.matchers.Caip2Matcher

class Caip19Matcher(
    private val caip2Matcher: Caip2Matcher,
    private val assetMatcher: AssetMatcher
) {

    fun match(caip19Identifier: Caip19Identifier): Boolean {
        return caip2Matcher.match(caip19Identifier.caip2Identifier) &&
            assetMatcher.match(caip19Identifier.assetIdentifier)
    }
}
