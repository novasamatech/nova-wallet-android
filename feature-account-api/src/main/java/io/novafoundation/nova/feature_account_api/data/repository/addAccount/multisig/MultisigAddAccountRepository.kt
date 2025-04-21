package io.novafoundation.nova.feature_account_api.data.repository.addAccount.multisig

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_account_api.data.repository.addAccount.AddAccountRepository
import io.novafoundation.nova.feature_account_api.data.repository.addAccount.multisig.MultisigAddAccountRepository.Payload
import io.novafoundation.nova.feature_account_api.domain.account.identity.Identity
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

interface MultisigAddAccountRepository : AddAccountRepository<Payload> {

    class Payload(
        val accounts: List<AccountPayload>
    )

    class AccountPayload(
        val chain: Chain,
        val multisigAccountId: AccountIdKey,
        val otherSignatories: List<AccountIdKey>,
        val threshold: Int,
        val signatoryMetaId: Long,
        val signatoryAccountId: AccountIdKey,
        val identity: Identity?
    )
}
