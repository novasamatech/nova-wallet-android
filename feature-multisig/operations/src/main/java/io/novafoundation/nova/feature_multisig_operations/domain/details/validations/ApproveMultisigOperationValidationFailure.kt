package io.novafoundation.nova.feature_multisig_operations.domain.details.validations

import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal

sealed class ApproveMultisigOperationValidationFailure {

    class NotEnoughBalanceToPayFees(
        val signatory: MetaAccount,
        val chainAsset: Chain.Asset,
        val minimumNeeded: BigDecimal,
        val available: BigDecimal
    ) : ApproveMultisigOperationValidationFailure()
}
