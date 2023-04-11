package io.novafoundation.nova.web3names.domain.exceptions

import jp.co.soramitsu.fearless_utils.extensions.requirePrefix

sealed class Web3NamesException(identifier: String) : Exception() {

    val web3Name: String = identifier.requirePrefix("w3n:")

    class ChainProviderNotFoundException(identifier: String) : Web3NamesException(identifier)

    class IntegrityCheckFailed(identifier: String) : Web3NamesException(identifier)

    class ValidAccountNotFoundException(identifier: String, val chainName: String) : Web3NamesException(identifier)

    class UnknownException(web3NameInput: String, val chainName: String) : Web3NamesException(web3NameInput)
}
