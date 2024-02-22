package io.novafoundation.nova.web3names.domain.exceptions

import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.extensions.requirePrefix

sealed class Web3NamesException(identifier: String) : Exception() {

    val web3Name: String = identifier.requirePrefix("w3n:")

    class ChainProviderNotFoundException(identifier: String) : Web3NamesException(identifier)

    class IntegrityCheckFailed(identifier: String) : Web3NamesException(identifier)

    class ValidAccountNotFoundException(identifier: String, val chainName: String) : Web3NamesException(identifier)

    class UnknownException(web3NameInput: String, val chainName: String) : Web3NamesException(web3NameInput)

    class UnsupportedAsset(identifier: String, val chainAsset: Chain.Asset) : Web3NamesException(identifier)
}
