package io.novafoundation.nova.feature_account_api.data.multisig.validation

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MultisigMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.requireAccountIdIn
import io.novafoundation.nova.feature_account_api.domain.model.requireAccountIdKeyIn
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericCall

class MultisigExtrinsicValidationPayload(
    val multisig: MultisigMetaAccount,
    val signatory: MetaAccount,
    val chain: Chain,
    val signatoryFeePaymentMode: SignatoryFeePaymentMode,
    // Call that is passed to as_multi. Might be both the actual call (in case multisig is a the root signer) or be wrapped by some other signer
    val callInsideAsMulti: GenericCall.Instance,
)

sealed class SignatoryFeePaymentMode {

    data object PaysSubmissionFee : SignatoryFeePaymentMode()

    data object NothingToPay : SignatoryFeePaymentMode()
}

fun MultisigExtrinsicValidationPayload.signatoryAccountId(): AccountId {
    return signatory.requireAccountIdIn(chain)
}

fun MultisigExtrinsicValidationPayload.multisigAccountId(): AccountIdKey {
    return multisig.requireAccountIdKeyIn(chain)
}
