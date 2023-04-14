package io.novafoundation.nova.web3names.data.caip19.identifiers

import java.math.BigInteger

enum class Caip2Namespace(val namespaceName: String) {
    EIP155("eip155"),
    POLKADOT("polkadot");

    companion object {

        fun find(namespaceName: String): Caip2Namespace? {
            return values().find { it.namespaceName == namespaceName }
        }
    }
}

sealed interface Caip2Identifier {
    val namespaceWitId: String

    class Eip155(val chainId: BigInteger) : Caip2Identifier {
        override val namespaceWitId: String = "${Caip2Namespace.EIP155.namespaceName}:$chainId"
    }

    class Polkadot(val genesisHash: String) : Caip2Identifier {
        override val namespaceWitId: String = "${Caip2Namespace.POLKADOT.namespaceName}:${genesisHash.take(32)}"
    }
}
