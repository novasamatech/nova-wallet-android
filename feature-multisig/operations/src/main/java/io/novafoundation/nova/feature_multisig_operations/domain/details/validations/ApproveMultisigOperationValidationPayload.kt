package io.novafoundation.nova.feature_multisig_operations.domain.details.validations

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.data.multisig.model.PendingMultisigOperation
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.requireAccountIdKeyIn
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.balances.model.ChainAssetBalance
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class ApproveMultisigOperationValidationPayload(
    val fee: Fee,
    val signatoryBalance: ChainAssetBalance,
    val signatory: MetaAccount,
    val operation: PendingMultisigOperation,
)

val ApproveMultisigOperationValidationPayload.chain: Chain
    get() = operation.chain

val ApproveMultisigOperationValidationPayload.signatoryAccountId: AccountIdKey
    get() = signatory.requireAccountIdKeyIn(chain)
