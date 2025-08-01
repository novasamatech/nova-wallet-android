package io.novafoundation.nova.feature_account_api.data.multisig

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_account_api.domain.multisig.CallHash
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

interface MultisigApprovalsRepository {
    suspend fun getApprovals(
        chain: Chain,
        accountIdKey: AccountIdKey,
        operationId: CallHash
    ): List<AccountIdKey>?
}
