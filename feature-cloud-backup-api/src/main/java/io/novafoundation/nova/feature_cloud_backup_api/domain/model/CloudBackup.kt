package io.novafoundation.nova.feature_cloud_backup_api.domain.model

import io.novafoundation.nova.common.utils.Identifiable
import io.novafoundation.nova.core.model.CryptoType
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

data class CloudBackup(
    val publicData: PublicData,
    val privateData: PrivateData
) {

    data class PublicData(
        val modifiedAt: Long,
        val wallets: List<WalletPublicInfo>
    )

    data class WalletPublicInfo(
        val walletId: String,
        val substratePublicKey: ByteArray?,
        val substrateAccountId: ByteArray?,
        val substrateCryptoType: CryptoType?,
        val ethereumAddress: ByteArray?,
        val ethereumPublicKey: ByteArray?,
        val name: String,
        val type: Type,
        val chainAccounts: Set<ChainAccountInfo>
    ) : Identifiable {

        override val identifier: String = walletId

        enum class Type {
            SECRETS, WATCH_ONLY, PARITY_SIGNER, LEDGER, LEDGER_GENERIC, POLKADOT_VAULT
        }

        data class ChainAccountInfo(
            val chainId: ChainId,
            val publicKey: ByteArray?,
            val accountId: ByteArray,
            val cryptoType: ChainAccountCryptoType?,
        ) {

            override fun equals(other: Any?): Boolean {
                if (other !is ChainAccountInfo) return false

                return chainId == other.chainId &&
                    publicKey.contentEquals(other.publicKey) &&
                    accountId.contentEquals(other.accountId) &&
                    cryptoType == other.cryptoType
            }

            override fun hashCode(): Int {
                var result = chainId.hashCode()
                result = 31 * result + (publicKey?.contentHashCode() ?: 0)
                result = 31 * result + accountId.contentHashCode()
                result = 31 * result + (cryptoType?.hashCode() ?: 0)
                return result
            }

            enum class ChainAccountCryptoType {
                SR25519, ED25519, ECDSA, ETHEREUM
            }
        }

        override fun equals(other: Any?): Boolean {
            if (other !is WalletPublicInfo) return false

            return walletId == other.walletId &&
                substratePublicKey.contentEquals(other.substratePublicKey) &&
                substrateAccountId.contentEquals(other.substrateAccountId) &&
                substrateCryptoType == other.substrateCryptoType &&
                ethereumAddress.contentEquals(other.ethereumAddress) &&
                ethereumPublicKey.contentEquals(other.ethereumPublicKey) &&
                name == other.name &&
                type == other.type &&
                chainAccounts == other.chainAccounts
        }

        override fun hashCode(): Int {
            var result = walletId.hashCode()
            result = 31 * result + (substratePublicKey?.contentHashCode() ?: 0)
            result = 31 * result + (substrateAccountId?.contentHashCode() ?: 0)
            result = 31 * result + (substrateCryptoType?.hashCode() ?: 0)
            result = 31 * result + (ethereumAddress?.contentHashCode() ?: 0)
            result = 31 * result + (ethereumPublicKey?.contentHashCode() ?: 0)
            result = 31 * result + name.hashCode()
            result = 31 * result + type.hashCode()
            result = 31 * result + chainAccounts.hashCode()
            result = 31 * result + identifier.hashCode()
            return result
        }
    }

    data class PrivateData(
        val wallets: List<WalletPrivateInfo>
    )

    data class WalletPrivateInfo(
        val walletId: String,
        val entropy: ByteArray?,
        val substrate: SubstrateSecrets?,
        val ethereum: EthereumSecrets?,
        val chainAccounts: List<ChainAccountSecrets>,
    ) : Identifiable {

        override val identifier: String = walletId

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as WalletPrivateInfo

            if (walletId != other.walletId) return false
            if (entropy != null) {
                if (other.entropy == null) return false
                if (!entropy.contentEquals(other.entropy)) return false
            } else if (other.entropy != null) return false
            if (substrate != other.substrate) return false
            if (ethereum != other.ethereum) return false
            if (chainAccounts != other.chainAccounts) return false
            return identifier == other.identifier
        }

        override fun hashCode(): Int {
            var result = walletId.hashCode()
            result = 31 * result + (entropy?.contentHashCode() ?: 0)
            result = 31 * result + (substrate?.hashCode() ?: 0)
            result = 31 * result + (ethereum?.hashCode() ?: 0)
            result = 31 * result + chainAccounts.hashCode()
            result = 31 * result + identifier.hashCode()
            return result
        }

        data class ChainAccountSecrets(
            val accountId: ByteArray,
            val entropy: ByteArray?,
            val seed: ByteArray?,
            val keypair: KeyPairSecrets?,
            val derivationPath: String?,
        ) {

            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as ChainAccountSecrets

                if (!accountId.contentEquals(other.accountId)) return false
                if (entropy != null) {
                    if (other.entropy == null) return false
                    if (!entropy.contentEquals(other.entropy)) return false
                } else if (other.entropy != null) return false
                if (seed != null) {
                    if (other.seed == null) return false
                    if (!seed.contentEquals(other.seed)) return false
                } else if (other.seed != null) return false
                if (keypair != other.keypair) return false
                return derivationPath == other.derivationPath
            }

            override fun hashCode(): Int {
                var result = accountId.contentHashCode()
                result = 31 * result + (entropy?.contentHashCode() ?: 0)
                result = 31 * result + (seed?.contentHashCode() ?: 0)
                result = 31 * result + (keypair?.hashCode() ?: 0)
                result = 31 * result + (derivationPath?.hashCode() ?: 0)
                return result
            }
        }

        data class SubstrateSecrets(
            val seed: ByteArray?,
            val keypair: KeyPairSecrets,
            val derivationPath: String?,
        ) {

            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as SubstrateSecrets

                if (seed != null) {
                    if (other.seed == null) return false
                    if (!seed.contentEquals(other.seed)) return false
                } else if (other.seed != null) return false
                if (keypair != other.keypair) return false
                return derivationPath == other.derivationPath
            }

            override fun hashCode(): Int {
                var result = seed?.contentHashCode() ?: 0
                result = 31 * result + keypair.hashCode()
                result = 31 * result + (derivationPath?.hashCode() ?: 0)
                return result
            }
        }

        data class EthereumSecrets(
            val keypair: KeyPairSecrets,
            val derivationPath: String?,
        )

        data class KeyPairSecrets(
            val publicKey: ByteArray,
            val privateKey: ByteArray,
            val nonce: ByteArray?
        ) {

            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as KeyPairSecrets

                if (!publicKey.contentEquals(other.publicKey)) return false
                if (!privateKey.contentEquals(other.privateKey)) return false
                if (nonce != null) {
                    if (other.nonce == null) return false
                    if (!nonce.contentEquals(other.nonce)) return false
                } else if (other.nonce != null) return false

                return true
            }

            override fun hashCode(): Int {
                var result = publicKey.contentHashCode()
                result = 31 * result + privateKey.contentHashCode()
                result = 31 * result + (nonce?.contentHashCode() ?: 0)
                return result
            }
        }
    }
}

fun CloudBackup.WalletPrivateInfo.isCompletelyEmpty(): Boolean {
    return entropy == null && substrate == null && ethereum == null && chainAccounts.isEmpty()
}
