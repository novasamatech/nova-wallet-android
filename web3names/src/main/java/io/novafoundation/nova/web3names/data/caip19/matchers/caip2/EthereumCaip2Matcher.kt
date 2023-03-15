package io.novafoundation.nova.web3names.data.caip19.matchers.caip2

import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.web3names.data.caip19.identifiers.Caip2Identifier

class EthereumCaip2Matcher(private val chain: Chain) : Caip2Matcher {
    override fun match(caip2Identifier: Caip2Identifier): Boolean {
        return caip2Identifier is Caip2Identifier.Eip155 &&
            caip2Identifier.namespaceWitId == chain.id &&
            chain.addressPrefix.toBigInteger() == caip2Identifier.chainId
    }
}
