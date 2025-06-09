package io.novafoundation.nova.feature_account_impl.domain.account.model

import io.novafoundation.nova.core.model.CryptoType
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.SecretsMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.hasChainAccountIn
import io.novafoundation.nova.feature_account_api.domain.model.substrateFrom
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novasama.substrate_sdk_android.encrypt.MultiChainEncryption

class RealSecretsMetaAccount(
    id: Long,
    globallyUniqueId: String,
    substratePublicKey: ByteArray?,
    substrateCryptoType: CryptoType?,
    substrateAccountId: ByteArray?,
    ethereumAddress: ByteArray?,
    ethereumPublicKey: ByteArray?,
    isSelected: Boolean,
    name: String,
    status: LightMetaAccount.Status,
    chainAccounts: Map<ChainId, MetaAccount.ChainAccount>,
    parentMetaId: Long?
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
    type = LightMetaAccount.Type.SECRETS,
    status = status,
    chainAccounts = chainAccounts,
    parentMetaId = parentMetaId
), SecretsMetaAccount {

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
