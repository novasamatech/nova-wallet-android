package io.novafoundation.nova.caip.caip19

import io.novafoundation.nova.caip.caip19.identifiers.AssetIdentifier
import io.novafoundation.nova.caip.caip19.identifiers.Caip19Identifier
import io.novafoundation.nova.caip.caip19.identifiers.ERC20
import io.novafoundation.nova.caip.caip19.identifiers.NotSupportedIdentifierException
import io.novafoundation.nova.caip.caip19.identifiers.SLIP44
import io.novafoundation.nova.caip.caip2.Caip2Parser
import io.novafoundation.nova.caip.common.toNamespaceAndReference

interface Caip19Parser {

    fun parseCaip19(caip19Identifier: String): Result<Caip19Identifier>
}

internal class RealCaip19Parser(
    private val caip2Parser: Caip2Parser,
) : Caip19Parser {

    override fun parseCaip19(caip19Identifier: String): Result<Caip19Identifier> = runCatching {
        val (chain, asset) = caip19Identifier.splitToNamespaces()

        Caip19Identifier(caip2Parser.parseCaip2(chain).getOrThrow(), parseAsset(asset))
    }

    private fun parseAsset(asset: String): AssetIdentifier {
        val (assetNamespace, assetIdentifier) = asset.toNamespaceAndReference()

        return when (assetNamespace) {
            SLIP44 -> AssetIdentifier.Slip44(assetIdentifier.toInt())
            ERC20 -> AssetIdentifier.Erc20(assetIdentifier)
            else -> throw NotSupportedIdentifierException()
        }
    }

    private fun String.splitToNamespaces(): Pair<String, String> {
        val (chainNamespace, tokenNamespace) = split("/")
        return Pair(chainNamespace, tokenNamespace)
    }
}
