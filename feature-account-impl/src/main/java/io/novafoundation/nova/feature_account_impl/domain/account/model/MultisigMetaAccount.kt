package io.novafoundation.nova.feature_account_impl.domain.account.model

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MultisigMetaAccount
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class RealMultisigMetaAccount(
    id: Long,
    globallyUniqueId: String,
    substrateAccountId: ByteArray?,
    ethereumAddress: ByteArray?,
    ethereumPublicKey: ByteArray?,
    isSelected: Boolean,
    name: String,
    status: LightMetaAccount.Status,
    override val signatoryMetaId: Long,
    override val signatoryAccountId: AccountIdKey,
    override val otherSignatories: List<AccountIdKey>,
    override val threshold: Int
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
    chainAccounts = emptyMap(),
), MultisigMetaAccount {

    override suspend fun supportsAddingChainAccount(chain: Chain): Boolean {
        // User cannot manually add accounts to multisig meta account
        return false
    }
}
