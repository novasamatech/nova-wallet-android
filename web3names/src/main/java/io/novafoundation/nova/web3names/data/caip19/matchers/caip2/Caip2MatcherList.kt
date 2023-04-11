package io.novafoundation.nova.web3names.data.caip19.matchers.caip2

import io.novafoundation.nova.web3names.data.caip19.identifiers.Caip2Identifier

class Caip2MatcherList(private val matchers: List<Caip2Matcher>) : Caip2Matcher {

    override fun match(caip2Identifier: Caip2Identifier): Boolean {
        return matchers.any { it.match(caip2Identifier) }
    }
}
