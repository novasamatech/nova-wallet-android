package io.novafoundation.nova.feature_account_impl.domain.account.model

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.address.format.AddressScheme
import io.novafoundation.nova.feature_account_api.domain.model.DerivativeMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccountAvailability
import io.novafoundation.nova.feature_account_impl.data.derivative.DerivativeAccountRepository
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novasama.substrate_sdk_android.runtime.AccountId

class RealDerivativeMetaAccount(
    id: Long,
    globallyUniqueId: String,
    substrateAccountId: ByteArray?,
    ethereumAddress: ByteArray?,
    ethereumPublicKey: ByteArray?,
    chainAccounts: Map<ChainId, MetaAccount.ChainAccount>,
    isSelected: Boolean,
    name: String,
    status: LightMetaAccount.Status,
    override val parentAccountId: AccountIdKey,
    override val parentMetaId: Long,
    override val index: Int,
    private val derivativeAccountRepository: DerivativeAccountRepository,
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
    type = LightMetaAccount.Type.DERIVATIVE,
    status = status,
    chainAccounts = chainAccounts,
    parentMetaId = parentMetaId
), DerivativeMetaAccount {

    override val availability: MetaAccountAvailability
        get() = if (chainAccounts.isEmpty()) {
            MetaAccountAvailability.Universal(addressScheme())
        } else {
            MetaAccountAvailability.SingleChain(chainAccounts.keys.first())
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
        return null
    }

    private fun isSupported(chain: Chain): Boolean {
        return derivativeAccountRepository.areDerivativeAccountsSupported(chain)
    }

    private fun addressScheme(): AddressScheme {
        return when {
            substrateAccountId != null -> AddressScheme.SUBSTRATE
            else -> AddressScheme.EVM
        }
    }
}
