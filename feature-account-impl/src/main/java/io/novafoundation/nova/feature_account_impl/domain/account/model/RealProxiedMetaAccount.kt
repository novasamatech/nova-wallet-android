package io.novafoundation.nova.feature_account_impl.domain.account.model

import io.novafoundation.nova.core.model.CryptoType
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.ProxiedMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.ProxyAccount
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

internal class RealProxiedMetaAccount(
    id: Long,
    globallyUniqueId: String,
    substratePublicKey: ByteArray?,
    substrateCryptoType: CryptoType?,
    substrateAccountId: ByteArray?,
    ethereumAddress: ByteArray?,
    ethereumPublicKey: ByteArray?,
    isSelected: Boolean,
    name: String,
    type: LightMetaAccount.Type,
    status: LightMetaAccount.Status,
    override val proxy: ProxyAccount,
    chainAccounts: Map<ChainId, MetaAccount.ChainAccount>
) : DefaultMetaAccount(
    id = id,
    globallyUniqueId = globallyUniqueId,
    substratePublicKey = substratePublicKey,
    substrateCryptoType = substrateCryptoType,
    substrateAccountId = substrateAccountId,
    ethereumAddress = ethereumAddress,
    ethereumPublicKey = ethereumPublicKey,
    isSelected = isSelected,
    name = name,
    type = type,
    status = status,
    chainAccounts = chainAccounts,
),
    ProxiedMetaAccount {

    override suspend fun supportsAddingChainAccount(chain: Chain): Boolean {
        // User cannot manually add accounts to proxy meta account
        return false
    }
}
