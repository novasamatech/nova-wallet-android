package io.novafoundation.nova.caip.caip2.matchers

import io.novafoundation.nova.caip.caip2.identifier.Caip2Identifier
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class Eip155Matcher(private val chain: Chain) : Caip2Matcher {

    override fun match(caip2Identifier: Caip2Identifier): Boolean {
        return caip2Identifier is Caip2Identifier.Eip155 &&
            caip2Identifier.chainId == chain.addressPrefix.toBigInteger()
    }
}
