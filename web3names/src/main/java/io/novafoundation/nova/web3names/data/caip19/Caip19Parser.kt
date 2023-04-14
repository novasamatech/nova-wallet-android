package io.novafoundation.nova.web3names.data.caip19

import io.novafoundation.nova.web3names.data.caip19.identifiers.AssetIdentifier
import io.novafoundation.nova.web3names.data.caip19.identifiers.Caip19Identifier
import io.novafoundation.nova.web3names.data.caip19.identifiers.Caip2Identifier
import io.novafoundation.nova.web3names.data.caip19.identifiers.Caip2Namespace
import io.novafoundation.nova.web3names.data.caip19.identifiers.ERC20
import io.novafoundation.nova.web3names.data.caip19.identifiers.NotSupportedIdentifierException
import io.novafoundation.nova.web3names.data.caip19.identifiers.SLIP44

class Caip19Parser {

    fun parseCaip19(raw: String): Result<Caip19Identifier> {
        val (chain, asset) = raw.splitToNamespaces()

        return runCatching {
            Caip19Identifier(parserCaip2(chain), parseAsset(asset))
        }
    }

    fun parserCaip2(chain: String): Caip2Identifier {
        val (chainNamespace, chainIdentifier) = chain.toNamespaceAndReference()

        return when (chainNamespace) {
            Caip2Namespace.EIP155.namespaceName -> Caip2Identifier.Eip155(chainIdentifier.toBigInteger())
            Caip2Namespace.POLKADOT.namespaceName -> Caip2Identifier.Polkadot(chainIdentifier)
            else -> throw NotSupportedIdentifierException()
        }
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

    private fun String.toNamespaceAndReference(): Pair<String, String> {
        val (namespaceName, namespaceReference) = split(":")
        return Pair(namespaceName, namespaceReference)
    }
}
