package io.novafoundation.nova.caip.caip2.identifier

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
        override val namespaceWitId: String = formatCaip2(Caip2Namespace.EIP155, chainId)
    }

    class Polkadot(val genesisHash: String) : Caip2Identifier {
        override val namespaceWitId: String = formatCaip2(Caip2Namespace.POLKADOT, genesisHash.take(32))
    }
}

private fun formatCaip2(namespace: Caip2Namespace, reference: Any): String {
    return "${namespace.namespaceName}:$reference"
}
