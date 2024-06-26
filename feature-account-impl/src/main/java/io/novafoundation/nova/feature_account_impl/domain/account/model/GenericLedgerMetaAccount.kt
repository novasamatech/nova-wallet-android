package io.novafoundation.nova.feature_account_impl.domain.account.model

import io.novafoundation.nova.core.model.CryptoType
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.ProxyAccount
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novasama.substrate_sdk_android.encrypt.MultiChainEncryption
import io.novasama.substrate_sdk_android.runtime.AccountId

class GenericLedgerMetaAccount(
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
    chainAccounts: Map<ChainId, MetaAccount.ChainAccount>,
    proxy: ProxyAccount?,
    private val supportedGenericLedgerChains: Set<ChainId>
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
    proxy = proxy
) {

    override suspend fun supportsAddingChainAccount(chain: Chain): Boolean {
        // Generic ledger provides account for every possible account
        return false
    }

    override fun hasAccountIn(chain: Chain): Boolean {
        return if (isSupported(chain)) {
            super.hasAccountIn(chain)
        } else {
            false
        }
    }

    override fun accountIdIn(chain: Chain): AccountId? {
        return if (isSupported(chain)) {
            super.accountIdIn(chain)
        } else {
            null
        }
    }

    override fun publicKeyIn(chain: Chain): ByteArray? {
        return if (isSupported(chain)) {
            super.publicKeyIn(chain)
        } else {
            null
        }
    }

    override fun multiChainEncryptionIn(chain: Chain): MultiChainEncryption? {
        return if (isSupported(chain)) {
            super.multiChainEncryptionIn(chain)
        } else {
            null
        }
    }

    private fun isSupported(chain: Chain) = chain.id in supportedGenericLedgerChains
}
