package io.novafoundation.nova.feature_account_api.domain.model

import io.novafoundation.nova.common.data.mappers.mapCryptoTypeToEncryption
import io.novafoundation.nova.common.utils.DEFAULT_PREFIX
import io.novafoundation.nova.core.model.CryptoType
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.ext.toEthereumAddress
import io.novafoundation.nova.runtime.multiNetwork.ChainsById
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novasama.substrate_sdk_android.encrypt.MultiChainEncryption
import io.novasama.substrate_sdk_android.extensions.asEthereumPublicKey
import io.novasama.substrate_sdk_android.extensions.toAccountId
import io.novasama.substrate_sdk_android.ss58.SS58Encoder
import io.novasama.substrate_sdk_android.ss58.SS58Encoder.toAddress

class MetaAccountOrdering(
    val id: Long,
    val position: Int,
)

interface LightMetaAccount {
    val id: Long
    val substratePublicKey: ByteArray?
    val substrateCryptoType: CryptoType?
    val substrateAccountId: ByteArray?
    val ethereumAddress: ByteArray?
    val ethereumPublicKey: ByteArray?
    val isSelected: Boolean
    val name: String
    val type: Type
    val status: LightMetaAccount.Status

    enum class Type {
        SECRETS, WATCH_ONLY, PARITY_SIGNER, LEDGER, POLKADOT_VAULT, PROXIED
    }

    enum class Status {
        ACTIVE, DEACTIVATED
    }
}

fun LightMetaAccount(
    id: Long,
    substratePublicKey: ByteArray?,
    substrateCryptoType: CryptoType?,
    substrateAccountId: ByteArray?,
    ethereumAddress: ByteArray?,
    ethereumPublicKey: ByteArray?,
    isSelected: Boolean,
    name: String,
    type: LightMetaAccount.Type,
    status: LightMetaAccount.Status
) = object : LightMetaAccount {
    override val id: Long = id
    override val substratePublicKey: ByteArray? = substratePublicKey
    override val substrateCryptoType: CryptoType? = substrateCryptoType
    override val substrateAccountId: ByteArray? = substrateAccountId
    override val ethereumAddress: ByteArray? = ethereumAddress
    override val ethereumPublicKey: ByteArray? = ethereumPublicKey
    override val isSelected: Boolean = isSelected
    override val name: String = name
    override val type: LightMetaAccount.Type = type
    override val status: LightMetaAccount.Status = status
}

class MetaAccount(
    override val id: Long,
    val chainAccounts: Map<ChainId, ChainAccount>,
    val proxy: ProxyAccount?,
    override val substratePublicKey: ByteArray?,
    override val substrateCryptoType: CryptoType?,
    override val substrateAccountId: ByteArray?,
    override val ethereumAddress: ByteArray?,
    override val ethereumPublicKey: ByteArray?,
    override val isSelected: Boolean,
    override val name: String,
    override val type: LightMetaAccount.Type,
    override val status: LightMetaAccount.Status
) : LightMetaAccount {

    class ChainAccount(
        val metaId: Long,
        val chainId: ChainId,
        val publicKey: ByteArray?,
        val accountId: ByteArray,
        val cryptoType: CryptoType?,
    )
}

fun MetaAccount.hasAccountIn(chain: Chain) = when {
    hasChainAccountIn(chain.id) -> true
    chain.isEthereumBased -> ethereumAddress != null
    else -> substrateAccountId != null
}

fun MetaAccount.hasChainAccountIn(chainId: ChainId) = chainId in chainAccounts

fun MetaAccount.cryptoTypeIn(chain: Chain): CryptoType? {
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
        else -> substrateAccountId?.let { chain.addressOf(substrateAccountId) }
    }
}

fun MetaAccount.mainEthereumAddress() = ethereumAddress?.toEthereumAddress()

fun MetaAccount.requireAddressIn(chain: Chain): String = addressIn(chain) ?: throw NoSuchElementException("No chain account found for $chain in $name")

val MetaAccount.defaultSubstrateAddress: String?
    get() = substrateAccountId?.toDefaultSubstrateAddress()

fun ByteArray.toDefaultSubstrateAddress(): String {
    return toAddress(SS58Encoder.DEFAULT_PREFIX)
}

fun MetaAccount.accountIdIn(chain: Chain): ByteArray? {
    return when {
        hasChainAccountIn(chain.id) -> chainAccounts.getValue(chain.id).accountId
        chain.isEthereumBased -> ethereumAddress
        else -> substrateAccountId
    }
}

fun MetaAccount.requireAccountIdIn(chain: Chain): ByteArray {
    return requireNotNull(accountIdIn(chain))
}

fun MetaAccount.publicKeyIn(chain: Chain): ByteArray? {
    return when {
        hasChainAccountIn(chain.id) -> chainAccounts.getValue(chain.id).publicKey
        chain.isEthereumBased -> ethereumPublicKey
        else -> substratePublicKey
    }
}

fun MetaAccount.multiChainEncryptionIn(chain: Chain): MultiChainEncryption? {
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

fun MetaAccount.ethereumAccountId() = ethereumPublicKey?.asEthereumPublicKey()?.toAccountId()?.value

/**
@return [MultiChainEncryption] for given [accountId] inside this meta account or null in case it was not possible to determine result
 */
fun MetaAccount.multiChainEncryptionFor(accountId: ByteArray, chainsById: ChainsById): MultiChainEncryption? {
    return when {
        substrateAccountId.contentEquals(accountId) -> substrateCryptoType?.let(MultiChainEncryption.Companion::substrateFrom)
        ethereumAccountId().contentEquals(accountId) -> MultiChainEncryption.Ethereum
        else -> {
            val chainAccount = chainAccounts.values.firstOrNull { it.accountId.contentEquals(accountId) } ?: return null
            val cryptoType = chainAccount.cryptoType ?: return null
            val chain = chainsById[chainAccount.chainId] ?: return null

            if (chain.isEthereumBased) {
                MultiChainEncryption.Ethereum
            } else {
                MultiChainEncryption.substrateFrom(cryptoType)
            }
        }
    }
}

private fun MultiChainEncryption.Companion.substrateFrom(cryptoType: CryptoType): MultiChainEncryption.Substrate {
    return MultiChainEncryption.Substrate(mapCryptoTypeToEncryption(cryptoType))
}

fun MetaAccount.chainAccountFor(chainId: ChainId) = chainAccounts.getValue(chainId)

fun LightMetaAccount.Type.asPolkadotVaultVariantOrNull(): PolkadotVaultVariant? {
    return when (this) {
        LightMetaAccount.Type.PARITY_SIGNER -> PolkadotVaultVariant.PARITY_SIGNER
        LightMetaAccount.Type.POLKADOT_VAULT -> PolkadotVaultVariant.POLKADOT_VAULT
        else -> null
    }
}

fun LightMetaAccount.Type.asPolkadotVaultVariantOrThrow(): PolkadotVaultVariant {
    return requireNotNull(asPolkadotVaultVariantOrNull()) {
        "Not a Polkadot Vault compatible account type"
    }
}

fun LightMetaAccount.Type.requestedAccountPaysFees(): Boolean {
    return when (this) {
        LightMetaAccount.Type.SECRETS,
        LightMetaAccount.Type.WATCH_ONLY,
        LightMetaAccount.Type.PARITY_SIGNER,
        LightMetaAccount.Type.LEDGER,
        LightMetaAccount.Type.POLKADOT_VAULT -> true

        LightMetaAccount.Type.PROXIED -> false
    }
}
