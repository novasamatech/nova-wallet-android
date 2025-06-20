package io.novafoundation.nova.feature_account_impl.domain.account.model

import io.novafoundation.nova.core.model.CryptoType
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
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
    parentMetaId: Long?,
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
    parentMetaId = parentMetaId
) {

    override suspend fun supportsAddingChainAccount(chain: Chain): Boolean {
        // While Generic Ledger now provides support for both Substrate and EVM, initial version only supported Substrate
        // So user might have a missing EVM account and we should allow them to add it
        return isSupported(chain)
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

    private fun isSupported(chain: Chain) = chain.id in supportedGenericLedgerChains
}
