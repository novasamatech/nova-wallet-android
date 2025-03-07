package io.novafoundation.nova.feature_account_api.domain.model

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.data.mappers.mapCryptoTypeToEncryption
import io.novafoundation.nova.common.data.mappers.mapEncryptionToCryptoType
import io.novafoundation.nova.common.utils.DEFAULT_PREFIX
import io.novafoundation.nova.core.model.CryptoType
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.ext.toEthereumAddress
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novasama.substrate_sdk_android.encrypt.MultiChainEncryption
import io.novasama.substrate_sdk_android.extensions.asEthereumPublicKey
import io.novasama.substrate_sdk_android.extensions.toAccountId
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.ss58.SS58Encoder
import io.novasama.substrate_sdk_android.ss58.SS58Encoder.toAddress

class MetaAccountOrdering(
    val id: Long,
    val position: Int,
)

interface LightMetaAccount {

    val id: Long

    /**
     * In contrast to [id] which should only be unique **locally**, [globallyUniqueId] should be unique **globally**,
     * meaning it should be unique across all application instances. This is useful to compare meta accounts from different application instances
     */
    val globallyUniqueId: String

    val substratePublicKey: ByteArray?
    val substrateCryptoType: CryptoType?
    val substrateAccountId: ByteArray?
    val ethereumAddress: ByteArray?
    val ethereumPublicKey: ByteArray?
    val isSelected: Boolean
    val name: String
    val type: Type
    val status: Status

    enum class Type {
        SECRETS,
        WATCH_ONLY,
        PARITY_SIGNER,
        LEDGER_LEGACY,
        LEDGER,
        POLKADOT_VAULT,
        PROXIED
    }

    enum class Status {
        ACTIVE, DEACTIVATED
    }
}

fun LightMetaAccount(
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
) = object : LightMetaAccount {
    override val id: Long = id
    override val globallyUniqueId: String = globallyUniqueId
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

interface MetaAccount : LightMetaAccount {

    // TODO we can now subclass MetaAccount and have this field only in subclass
    val proxy: ProxyAccount?

    val chainAccounts: Map<ChainId, ChainAccount>

    class ChainAccount(
        val metaId: Long,
        val chainId: ChainId,
        val publicKey: ByteArray?,
        val accountId: ByteArray,
        val cryptoType: CryptoType?,
    )

    suspend fun supportsAddingChainAccount(chain: Chain): Boolean

    fun hasAccountIn(chain: Chain): Boolean

    fun accountIdIn(chain: Chain): AccountId?

    fun publicKeyIn(chain: Chain): ByteArray?

    fun multiChainEncryptionIn(chain: Chain): MultiChainEncryption?
}

fun MetaAccount.hasChainAccountIn(chainId: ChainId) = chainId in chainAccounts

fun MetaAccount.addressIn(chain: Chain): String? {
    return accountIdIn(chain)?.let(chain::addressOf)
}

fun MetaAccount.accountIdKeyIn(chain: Chain): AccountIdKey? {
    return accountIdIn(chain)?.let(::AccountIdKey)
}

fun MetaAccount.mainEthereumAddress() = ethereumAddress?.toEthereumAddress()

fun MetaAccount.requireAddressIn(chain: Chain): String = addressIn(chain) ?: throw NoSuchElementException("No chain account found for ${chain.name} in $name")

val MetaAccount.defaultSubstrateAddress: String?
    get() = substrateAccountId?.toDefaultSubstrateAddress()

fun ByteArray.toDefaultSubstrateAddress(): String {
    return toAddress(SS58Encoder.DEFAULT_PREFIX)
}

fun MetaAccount.substrateMultiChainEncryption(): MultiChainEncryption? {
    return substrateCryptoType?.let(MultiChainEncryption.Companion::substrateFrom)
}

fun MetaAccount.requireAccountIdIn(chain: Chain): ByteArray {
    return requireNotNull(accountIdIn(chain))
}

fun MetaAccount.cryptoTypeIn(chain: Chain): CryptoType? {
    return multiChainEncryptionIn(chain)?.toCryptoType()
}

private fun MultiChainEncryption.toCryptoType(): CryptoType {
    return when (this) {
        is MultiChainEncryption.Substrate -> mapEncryptionToCryptoType(encryptionType)
        MultiChainEncryption.Ethereum -> CryptoType.ECDSA
    }
}

fun MultiChainEncryption.Companion.substrateFrom(cryptoType: CryptoType): MultiChainEncryption.Substrate {
    return MultiChainEncryption.Substrate(mapCryptoTypeToEncryption(cryptoType))
}

fun MetaAccount.ethereumAccountId() = ethereumPublicKey?.asEthereumPublicKey()?.toAccountId()?.value

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
        LightMetaAccount.Type.LEDGER_LEGACY,
        LightMetaAccount.Type.LEDGER,
        LightMetaAccount.Type.POLKADOT_VAULT -> true

        LightMetaAccount.Type.PROXIED -> false
    }
}

val LightMetaAccount.Type.isProxied: Boolean
    get() = this == LightMetaAccount.Type.PROXIED
