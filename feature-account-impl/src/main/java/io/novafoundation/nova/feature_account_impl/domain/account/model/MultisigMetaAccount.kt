package io.novafoundation.nova.feature_account_impl.domain.account.model

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.address.format.AddressScheme
import io.novafoundation.nova.common.utils.compareTo
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MultisigAvailability
import io.novafoundation.nova.feature_account_api.domain.model.MultisigMetaAccount
import io.novafoundation.nova.feature_account_impl.data.multisig.MultisigRepository
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novasama.substrate_sdk_android.runtime.AccountId

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
    private val otherSignatoriesUnsorted: List<AccountIdKey>,
    override val threshold: Int,
    private val multisigRepository: MultisigRepository,
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

    override val otherSignatories by lazy(LazyThreadSafetyMode.PUBLICATION) {
        otherSignatoriesUnsorted.sortedWith { a, b -> a.value.compareTo(b.value, unsigned = true) }
    }

    override val availability: MultisigAvailability
        get() = if (chainAccounts.isEmpty()) {
            MultisigAvailability.Universal(addressScheme())
        } else {
            MultisigAvailability.SingleChain(chainAccounts.keys.first())
        }

    override suspend fun supportsAddingChainAccount(chain: Chain): Boolean {
        // User cannot manually add accounts to multisig meta account
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

    private fun isSupported(chain: Chain): Boolean {
        return multisigRepository.supportsMultisigSync(chain)
    }

    private fun addressScheme(): AddressScheme {
        return when {
            substrateAccountId != null -> AddressScheme.SUBSTRATE
            else -> AddressScheme.EVM
        }
    }
}
