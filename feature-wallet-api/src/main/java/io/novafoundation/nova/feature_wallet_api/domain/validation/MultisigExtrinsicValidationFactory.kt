package io.novafoundation.nova.feature_wallet_api.domain.validation

import io.novafoundation.nova.feature_account_api.data.multisig.validation.MultisigExtrinsicValidationBuilder

interface MultisigExtrinsicValidationFactory {

    context(MultisigExtrinsicValidationBuilder)
    fun multisigSignatoryHasEnoughBalance()

    context(MultisigExtrinsicValidationBuilder)
    fun noPendingMultisigWithSameCallData()
}
