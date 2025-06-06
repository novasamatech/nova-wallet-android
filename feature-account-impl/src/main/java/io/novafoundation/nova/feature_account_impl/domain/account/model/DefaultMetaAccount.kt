package io.novafoundation.nova.feature_account_impl.domain.account.model

import io.novafoundation.nova.core.model.CryptoType
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.hasChainAccountIn
import io.novafoundation.nova.feature_account_api.domain.model.substrateFrom
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novasama.substrate_sdk_android.encrypt.MultiChainEncryption
import io.novasama.substrate_sdk_android.runtime.AccountId

open class DefaultMetaAccount(
    override val id: Long,
    override val globallyUniqueId: String,
    override val substratePublicKey: ByteArray?,
    override val substrateCryptoType: CryptoType?,
    override val substrateAccountId: ByteArray?,
    override val ethereumAddress: ByteArray?,
    override val ethereumPublicKey: ByteArray?,
    override val isSelected: Boolean,
    override val name: String,
    override val type: LightMetaAccount.Type,
    override val status: LightMetaAccount.Status,
    override val chainAccounts: Map<ChainId, MetaAccount.ChainAccount>,
    override val parentMetaId: Long?
) : MetaAccount {

    override suspend fun supportsAddingChainAccount(chain: Chain): Boolean {
        return true
    }

    override fun hasAccountIn(chain: Chain): Boolean {
        return when {
            hasChainAccountIn(chain.id) -> true
            chain.isEthereumBased -> ethereumAddress != null
            else -> substrateAccountId != null
        }
    }

    override fun accountIdIn(chain: Chain): AccountId? {
        return when {
            hasChainAccountIn(chain.id) -> chainAccounts.getValue(chain.id).accountId
            chain.isEthereumBased -> ethereumAddress
            else -> substrateAccountId
        }
    }

    override fun publicKeyIn(chain: Chain): ByteArray? {
        return when {
            hasChainAccountIn(chain.id) -> chainAccounts.getValue(chain.id).publicKey
            chain.isEthereumBased -> ethereumPublicKey
            else -> substratePublicKey
        }
    }

    override fun multiChainEncryptionIn(chain: Chain): MultiChainEncryption? {
        return when {
            hasChainAccountIn(chain.id) -> {
                val cryptoType = chainAccounts.getValue(chain.id).cryptoType ?: return null

                if (chain.isEthereumBased) {
                    MultiChainEncryption.Ethereum
                } else {
                    MultiChainEncryption.substrateFrom(cryptoType)
                }
            }

            chain.isEthereumBased -> MultiChainEncryption.Ethereum

            else -> substrateCryptoType?.let(MultiChainEncryption.Companion::substrateFrom)
        }
    }
}
