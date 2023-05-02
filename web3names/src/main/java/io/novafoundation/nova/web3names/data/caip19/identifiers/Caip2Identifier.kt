package io.novafoundation.nova.web3names.data.caip19.identifiers

import java.math.BigInteger

const val EIP155 = "eip155"
const val POLKADOT = "polkadot"

sealed interface Caip2Identifier {
    val namespaceWitId: String

    class Eip155(val chainId: BigInteger) : Caip2Identifier {
        override val namespaceWitId: String = "$EIP155:$chainId"
    }

    class Polkadot(val genesisHash: String) : Caip2Identifier {
        override val namespaceWitId: String = "$POLKADOT:$genesisHash"
    }
}
