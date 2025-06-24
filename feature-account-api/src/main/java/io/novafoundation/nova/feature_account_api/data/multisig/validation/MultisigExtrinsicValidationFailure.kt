package io.novafoundation.nova.feature_account_api.data.multisig.validation

import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MultisigMetaAccount
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigInteger

sealed interface MultisigExtrinsicValidationFailure {

    class NotEnoughSignatoryBalance(
        val signatory: MetaAccount,
        val asset: Chain.Asset,
        val deposit: BigInteger?,
        val fee: BigInteger?,
        val balanceToAdd: BigInteger
    ): MultisigExtrinsicValidationFailure

    class OperationAlreadyExists(
        val multisigAccount: MultisigMetaAccount
    ) : MultisigExtrinsicValidationFailure
}
