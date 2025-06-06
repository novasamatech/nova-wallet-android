package io.novafoundation.nova.feature_account_impl.domain.account.model

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MultisigAvailability
import io.novafoundation.nova.feature_account_api.domain.model.MultisigMetaAccount
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

class RealMultisigMetaAccount(
    id: Long,
    globallyUniqueId: String,
    substrateAccountId: ByteArray?,
    ethereumAddress: ByteArray?,
    ethereumPublicKey: ByteArray?,
    chainAccounts: Map<ChainId, MetaAccount.ChainAccount>,
    isSelected: Boolean,
    name: String,
    status: LightMetaAccount.Status,
    override val signatoryMetaId: Long,
    override val signatoryAccountId: AccountIdKey,
    override val otherSignatories: List<AccountIdKey>,
    override val threshold: Int,
    parentMetaId: Long?
) : DefaultMetaAccount(
    id = id,
    globallyUniqueId = globallyUniqueId,
    substratePublicKey = null,
    substrateCryptoType = null,
    substrateAccountId = substrateAccountId,
    ethereumAddress = ethereumAddress,
    ethereumPublicKey = ethereumPublicKey,
    isSelected = isSelected,
    name = name,
    type = LightMetaAccount.Type.MULTISIG,
    status = status,
    chainAccounts = chainAccounts,
    parentMetaId = parentMetaId
),
    MultisigMetaAccount {

    override val availability: MultisigAvailability
        get() = if (chainAccounts.isEmpty()) {
            MultisigAvailability.Universal
        } else {
            MultisigAvailability.SingleChain(chainAccounts.keys.first())
        }

    override suspend fun supportsAddingChainAccount(chain: Chain): Boolean {
        // User cannot manually add accounts to multisig meta account
        return false
    }
}
