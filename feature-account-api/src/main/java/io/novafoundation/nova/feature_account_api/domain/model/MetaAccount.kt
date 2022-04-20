
package io.novafoundation.nova.feature_account_api.domain.model

import io.novafoundation.nova.common.data.mappers.mapCryptoTypeToEncryption
import io.novafoundation.nova.common.utils.DEFAULT_PREFIX
import io.novafoundation.nova.core.model.CryptoType
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.ext.toEthereumAddress
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import jp.co.soramitsu.fearless_utils.encrypt.MultiChainEncryption
import jp.co.soramitsu.fearless_utils.ss58.SS58Encoder
import jp.co.soramitsu.fearless_utils.ss58.SS58Encoder.toAddress

class MetaAccountOrdering(
    val id: Long,
    val position: Int,
)

interface LightMetaAccount {
    val id: Long
    val substratePublicKey: ByteArray
    val substrateCryptoType: CryptoType
    val substrateAccountId: ByteArray
    val ethereumAddress: ByteArray?
    val ethereumPublicKey: ByteArray?
    val isSelected: Boolean
    val name: String
}

fun LightMetaAccount(
    id: Long,
    substratePublicKey: ByteArray,
    substrateCryptoType: CryptoType,
    substrateAccountId: ByteArray,
    ethereumAddress: ByteArray?,
    ethereumPublicKey: ByteArray?,
    isSelected: Boolean,
    name: String,
) = object : LightMetaAccount {
    override val id: Long = id
    override val substratePublicKey: ByteArray = substratePublicKey
    override val substrateCryptoType: CryptoType = substrateCryptoType
    override val substrateAccountId: ByteArray = substrateAccountId
    override val ethereumAddress: ByteArray? = ethereumAddress
    override val ethereumPublicKey: ByteArray? = ethereumPublicKey
    override val isSelected: Boolean = isSelected
    override val name: String = name
}

class MetaAccount(
    override val id: Long,
    val chainAccounts: Map<ChainId, ChainAccount>,
    override val substratePublicKey: ByteArray,
    override val substrateCryptoType: CryptoType,
    override val substrateAccountId: ByteArray,
    override val ethereumAddress: ByteArray?,
    override val ethereumPublicKey: ByteArray?,
    override val isSelected: Boolean,
    override val name: String,
) : LightMetaAccount {

    class ChainAccount(
        val metaId: Long,
        val chain: Chain,
        val publicKey: ByteArray,
        val accountId: ByteArray,
        val cryptoType: CryptoType,
    )
}

fun MetaAccount.hasAccountIn(chain: Chain) = when {
    hasChainAccountIn(chain.id) -> true
    chain.isEthereumBased -> ethereumPublicKey != null
    else -> true
}

fun MetaAccount.hasChainAccountIn(chainId: ChainId) = chainId in chainAccounts

fun MetaAccount.cryptoTypeIn(chain: Chain): CryptoType {
    return when {
        hasChainAccountIn(chain.id) -> chainAccounts.getValue(chain.id).cryptoType
        chain.isEthereumBased -> CryptoType.ECDSA
        else -> substrateCryptoType
    }
}

fun MetaAccount.addressIn(chain: Chain): String? {
    return when {
        hasChainAccountIn(chain.id) -> chain.addressOf(chainAccounts.getValue(chain.id).accountId)
        chain.isEthereumBased -> ethereumAddress?.let(chain::addressOf)
        else -> chain.addressOf(substrateAccountId)
    }
}

fun MetaAccount.mainEthereumAddress() = ethereumAddress?.toEthereumAddress()

fun MetaAccount.requireAddressIn(chain: Chain): String = addressIn(chain) ?: throw NoSuchElementException("No chain account found for $chain in $name")

val MetaAccount.defaultSubstrateAddress
    get() = substrateAccountId.toAddress(SS58Encoder.DEFAULT_PREFIX)

fun MetaAccount.accountIdIn(chain: Chain): ByteArray? {
    return when {
        hasChainAccountIn(chain.id) -> chainAccounts.getValue(chain.id).accountId
        chain.isEthereumBased -> ethereumAddress
        else -> substrateAccountId
    }
}

fun MetaAccount.publicKeyIn(chain: Chain): ByteArray? {
    return when {
        hasChainAccountIn(chain.id) -> chainAccounts.getValue(chain.id).publicKey
        chain.isEthereumBased -> ethereumPublicKey
        else -> substratePublicKey
    }
}

fun MetaAccount.multiChainEncryptionIn(chain: Chain): MultiChainEncryption {
    return when {
        chain.isEthereumBased -> MultiChainEncryption.Ethereum
        else -> {
            val cryptoType = when {
                hasChainAccountIn(chain.id) -> chainAccounts.getValue(chain.id).cryptoType
                else -> substrateCryptoType
            }

            val encryptionType = mapCryptoTypeToEncryption(cryptoType)

            MultiChainEncryption.Substrate(encryptionType)
        }
    }
}

/**
 * Returns [MultiChainEncryption] for given [accountId] inside this meta account
 * @throws NoSuchElementException in case no matching [accountId] found inside meta account
 */
fun MetaAccount.multiChainEncryptionFor(accountId: ByteArray): MultiChainEncryption {
    return when {
        ethereumPublicKey.contentEquals(accountId) -> MultiChainEncryption.Ethereum
        else -> {
            val cryptoType = when {
                substrateAccountId.contentEquals(accountId) -> substrateCryptoType
                else -> chainAccounts.values.first { it.accountId.contentEquals(accountId) }.cryptoType
            }

            val encryptionType = mapCryptoTypeToEncryption(cryptoType)

            MultiChainEncryption.Substrate(encryptionType)
        }
    }
}

fun MetaAccount.chainAccountFor(chainId: ChainId) = chainAccounts.getValue(chainId)
