package io.novafoundation.nova.web3names.data.caip19.matchers.caip2

import io.novafoundation.nova.web3names.data.caip19.identifiers.Caip2Identifier

interface Caip2Matcher {

    fun match(caip2Identifier: Caip2Identifier): Boolean
}
