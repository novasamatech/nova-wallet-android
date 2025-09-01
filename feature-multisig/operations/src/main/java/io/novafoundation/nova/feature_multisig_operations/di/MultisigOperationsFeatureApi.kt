package io.novafoundation.nova.feature_multisig_operations.di

import io.novafoundation.nova.feature_multisig_operations.presentation.details.deeplink.MultisigOperationDeepLinkConfigurator
import io.novafoundation.nova.feature_multisig_operations.di.deeplink.MultisigDeepLinks
import io.novafoundation.nova.feature_multisig_operations.presentation.callFormatting.MultisigCallFormatter

interface MultisigOperationsFeatureApi {

    val multisigDeepLinks: MultisigDeepLinks

    val multisigOperationDeepLinkConfigurator: MultisigOperationDeepLinkConfigurator

    val multisigCallFormatter: MultisigCallFormatter
}
