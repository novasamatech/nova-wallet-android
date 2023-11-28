package io.novafoundation.nova.app.root.presentation.deepLinks.common

sealed class DeepLinkHandlingException : Exception() {

    sealed class ReferendumHandlingException : DeepLinkHandlingException() {

        object ReferendumIsNotSpecified : ReferendumHandlingException()

        object ChainIsNotFound : ReferendumHandlingException()

        object GovernanceTypeIsNotSepcified : ReferendumHandlingException()

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
