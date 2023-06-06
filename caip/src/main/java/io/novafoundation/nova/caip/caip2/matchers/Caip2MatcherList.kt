package io.novafoundation.nova.caip.caip2.matchers

import io.novafoundation.nova.caip.caip2.identifier.Caip2Identifier

class Caip2MatcherList(private val matchers: List<Caip2Matcher>) : Caip2Matcher {

    override fun match(caip2Identifier: Caip2Identifier): Boolean {
        return matchers.any { it.match(caip2Identifier) }
    }
}
