package io.novafoundation.nova.web3names.domain.exceptions

sealed class Web3NamesException : Exception() {
    class ChainProviderNotFoundException(val identifier: String) : Web3NamesException()

    class IntegrityCheckFailed: Web3NamesException()

    class ValidAccountNotFoundException(val identifier: String, val chainName: String) : Web3NamesException()

    class UnknownException(val chainName: String) : Web3NamesException()
}
