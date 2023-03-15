package io.novafoundation.nova.web3names.data.caip19

import io.novafoundation.nova.web3names.data.caip19.identifiers.AssetIdentifier
import io.novafoundation.nova.web3names.data.caip19.identifiers.Caip19Identifier
import io.novafoundation.nova.web3names.data.caip19.identifiers.Caip2Identifier
import io.novafoundation.nova.web3names.data.caip19.identifiers.EIP155
import io.novafoundation.nova.web3names.data.caip19.identifiers.ERC20
import io.novafoundation.nova.web3names.data.caip19.identifiers.NotSupportedIdentifierException
import io.novafoundation.nova.web3names.data.caip19.identifiers.POLKADOT
import io.novafoundation.nova.web3names.data.caip19.identifiers.SLIP44

class Caip19Parser {

    fun parseCaip19(raw: String): Result<Caip19Identifier> {
        val (chain, asset) = raw.splitToNamespaces()

        return runCatching {
            Caip19Identifier(parserChain(chain), parseAsset(asset))
        }
    }

    private fun parserChain(chain: String): Caip2Identifier {
        val (chainNamespace, chainIdentifier) = chain.toNamespaceAndReference()

        return when (chainNamespace) {
            EIP155 -> Caip2Identifier.Eip155(chainIdentifier.toBigInteger())
            POLKADOT -> Caip2Identifier.Polkadot(chainIdentifier)
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
}
