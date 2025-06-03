package io.novafoundation.nova.feature_deep_linking.presentation.handling.common

sealed class DeepLinkHandlingException : Exception() {

    sealed class ReferendumHandlingException : DeepLinkHandlingException() {

        object ReferendumIsNotSpecified : ReferendumHandlingException()

        object ChainIsNotFound : ReferendumHandlingException()

        object GovernanceTypeIsNotSpecified : ReferendumHandlingException()

        object GovernanceTypeIsNotSupported : ReferendumHandlingException()
    }

    sealed class DAppHandlingException : DeepLinkHandlingException() {

        object UrlIsInvalid : DAppHandlingException()

        class DomainIsNotMatched(val domain: String) : DAppHandlingException()
    }

    sealed class ImportMnemonicHandlingException : DeepLinkHandlingException() {

        object InvalidMnemonic : ImportMnemonicHandlingException()

        object InvalidCryptoType : ImportMnemonicHandlingException()

        object InvalidDerivationPath : ImportMnemonicHandlingException()
    }
}
