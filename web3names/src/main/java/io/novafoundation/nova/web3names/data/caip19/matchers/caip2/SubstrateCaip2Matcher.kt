package io.novafoundation.nova.web3names.data.caip19.matchers.caip2

import io.novafoundation.nova.common.utils.removeHexPrefix
import io.novafoundation.nova.runtime.ext.genesisHash
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.web3names.data.caip19.identifiers.Caip2Identifier

class SubstrateCaip2Matcher(private val chain: Chain) : Caip2Matcher {

    override fun match(caip2Identifier: Caip2Identifier): Boolean {
        return caip2Identifier is Caip2Identifier.Polkadot &&
            chain.id.removeHexPrefix().startsWith(caip2Identifier.genesisHash.removeHexPrefix())
    }
}
