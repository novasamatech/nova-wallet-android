package io.novafoundation.nova.app.root.presentation.deepLinks.common

import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.presentation.deepLinks.common.DeepLinkHandlingException.DAppHandlingException
import io.novafoundation.nova.app.root.presentation.deepLinks.common.DeepLinkHandlingException.ImportMnemonicHandlingException
import io.novafoundation.nova.app.root.presentation.deepLinks.common.DeepLinkHandlingException.ReferendumHandlingException
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.resources.ResourceManager

fun formatDeepLinkHandlingException(resourceManager: ResourceManager, exception: DeepLinkHandlingException): String {
    return when (exception) {

        is ReferendumHandlingException -> handleReferendumException(resourceManager, exception)

        is DAppHandlingException -> handleDAppException(resourceManager, exception)

        is ImportMnemonicHandlingException -> handleImportMnemonicException(resourceManager, exception)
    }
}

private fun handleReferendumException(resourceManager: ResourceManager, exception: ReferendumHandlingException): String {
    return when (exception) {
        ReferendumHandlingException.ReferendumIsNotSpecified -> resourceManager.getString(R.string.referendim_details_not_found_title)

        ReferendumHandlingException.ChainIsNotFound -> resourceManager.getString(R.string.deep_linking_chain_id_is_not_found)

        ReferendumHandlingException.GovernanceTypeIsNotSepcified -> resourceManager.getString(R.string.deep_linking_governance_type_is_not_specified)

        ReferendumHandlingException.GovernanceTypeIsNotSupported -> resourceManager.getString(R.string.deep_linking_governance_type_is_not_supported)
    }
}

fun handleDAppException(resourceManager: ResourceManager, exception: DAppHandlingException): String {
    return when (exception) {
        DAppHandlingException.UrlIsInvalid -> resourceManager.getString(R.string.deep_linking_url_is_invalid)

        is DAppHandlingException.DomainIsNotMatched -> resourceManager.getString(R.string.deep_linking_domain_is_not_matched, exception.domain)
    }
}

fun handleImportMnemonicException(resourceManager: ResourceManager, exception: ImportMnemonicHandlingException): String {
    return when (exception) {
        ImportMnemonicHandlingException.InvalidMnemonic -> resourceManager.getString(R.string.deep_linking_invalid_mnemonic)

        ImportMnemonicHandlingException.InvalidCryptoType -> resourceManager.getString(R.string.deep_linking_invalid_crypto_type)

        ImportMnemonicHandlingException.InvalidDerivationPath -> resourceManager.getString(R.string.deep_linking_invalid_derivation_path)
    }
}
