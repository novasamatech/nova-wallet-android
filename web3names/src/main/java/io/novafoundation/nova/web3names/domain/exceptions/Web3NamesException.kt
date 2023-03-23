package io.novafoundation.nova.web3names.domain.exceptions

sealed class Web3NamesException : Exception() {
    class ParseWeb3NameException : Web3NamesException()

    class ChainProviderNotFoundException : Web3NamesException()

    class ValidAccountNotFoundException : Web3NamesException()
}
