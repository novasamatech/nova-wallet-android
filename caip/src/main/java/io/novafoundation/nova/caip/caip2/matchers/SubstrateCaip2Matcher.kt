package io.novafoundation.nova.caip.caip2.matchers

import io.novafoundation.nova.common.utils.removeHexPrefix
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.caip.caip2.identifier.Caip2Identifier

class SubstrateCaip2Matcher(private val chain: Chain) : Caip2Matcher {

    override fun match(caip2Identifier: Caip2Identifier): Boolean {
        return caip2Identifier is Caip2Identifier.Polkadot &&
            chain.id.removeHexPrefix().startsWith(caip2Identifier.genesisHash.removeHexPrefix())
    }
}
