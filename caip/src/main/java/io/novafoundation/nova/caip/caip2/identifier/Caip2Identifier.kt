package io.novafoundation.nova.caip.caip2.identifier

import io.novafoundation.nova.common.utils.removeHexPrefix
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

sealed class Caip2Identifier {

    abstract val namespaceWitId: String

    abstract val namespace: Caip2Namespace

    override operator fun equals(other: Any?): Boolean = other is Caip2Identifier && namespaceWitId == other.namespaceWitId

    override fun hashCode(): Int = namespaceWitId.hashCode()

    class Eip155(val chainId: BigInteger) : Caip2Identifier() {

        override val namespace = Caip2Namespace.EIP155

        override val namespaceWitId: String = formatCaip2(Caip2Namespace.EIP155, chainId)
    }

    class Polkadot(val genesisHash: String) : Caip2Identifier() {
        override val namespace = Caip2Namespace.POLKADOT

        override val namespaceWitId: String = formatCaip2(Caip2Namespace.POLKADOT, genesisHash.removeHexPrefix().take(32))
    }
}

private fun formatCaip2(namespace: Caip2Namespace, reference: Any): String {
    return "${namespace.namespaceName}:$reference"
}
