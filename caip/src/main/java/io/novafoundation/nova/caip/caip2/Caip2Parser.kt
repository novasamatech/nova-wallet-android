package io.novafoundation.nova.caip.caip2

import io.novafoundation.nova.caip.caip19.identifiers.NotSupportedIdentifierException
import io.novafoundation.nova.caip.caip2.identifier.Caip2Identifier
import io.novafoundation.nova.caip.caip2.identifier.Caip2Namespace
import io.novafoundation.nova.caip.common.toNamespaceAndReference

interface Caip2Parser {

    fun parseCaip2(caip2Identifier: String): Result<Caip2Identifier>
}

fun Caip2Parser.isValidCaip2(caip2Identifier: String): Boolean = parseCaip2(caip2Identifier).isSuccess

fun Caip2Parser.parseCaip2OrThrow(caip2Identifier: String): Caip2Identifier = parseCaip2(caip2Identifier).getOrThrow()

internal class RealCaip2Parser : Caip2Parser {

    override fun parseCaip2(caip2Identifier: String): Result<Caip2Identifier> = runCatching {
        val (chainNamespace, chainIdentifier) = caip2Identifier.toNamespaceAndReference()

        when (chainNamespace) {
            Caip2Namespace.EIP155.namespaceName -> Caip2Identifier.Eip155(chainIdentifier.toBigInteger())
            Caip2Namespace.POLKADOT.namespaceName -> Caip2Identifier.Polkadot(chainIdentifier)
            else -> throw NotSupportedIdentifierException()
        }
    }
}
