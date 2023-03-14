package io.novafoundation.nova.web3names.domain.caip19.matchers.caip2

import io.novafoundation.nova.runtime.ext.genesisHash
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.web3names.domain.caip19.identifiers.Caip2Identifier

class SubstrateCaip2Matcher(private val chain: Chain) : Caip2Matcher {

    override fun match(caip2Identifier: Caip2Identifier): Boolean {
        return caip2Identifier is Caip2Identifier.Polkadot &&
            caip2Identifier.genesisHash == chain.genesisHash
    }
}
