package io.novafoundation.nova.feature_cloud_backup_api.domain.model

import io.novafoundation.nova.common.data.secrets.v2.SecretStoreV2
import io.novafoundation.nova.common.utils.Identifiable
import io.novafoundation.nova.core.model.CryptoType
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

class CloudBackup(
    val publicData: PublicData,
    val privateData: PrivateData
) {

    class PublicData(
        val modifiedAt: Long,
        val wallets: List<WalletPublicInfo>
    )

    class WalletPublicInfo(
        val walletId: String,
        val substratePublicKey: ByteArray?,
        val substrateAccountId: ByteArray?,
        val substrateCryptoType: CryptoType?,
        val ethereumAddress: ByteArray?,
        val ethereumPublicKey: ByteArray?,
        val name: String,
        val type: Type,
        val chainAccounts: Set<ChainAccountInfo>
    ): Identifiable {

        override val identifier: String = walletId

        enum class Type {
            SECRETS, WATCH_ONLY, PARITY_SIGNER, LEDGER, POLKADOT_VAULT
        }

        class ChainAccountInfo(
            val chainId: ChainId,
            val publicKey: ByteArray?,
            val accountId: ByteArray,
            val cryptoType: CryptoType?,
        ) {

            override fun equals(other: Any?): Boolean {
                if (other !is ChainAccountInfo) return false

                return chainId == other.chainId &&
                    publicKey.contentEquals(other.publicKey) &&
                    accountId.contentEquals(other.accountId) &&
                    cryptoType == other.cryptoType
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

    class PrivateData(
        val wallets: List<WalletPrivateInfo>
    )

    class WalletPrivateInfo(
        val walletId: String,
        val entropy: ByteArray?,
        val substrate: SubstrateSecrets?,
        val ethereum: EthereumSecrets?,
        val chainAccounts: List<ChainAccountSecrets>,
        /**
         * Stores additional secrets like derivation path for ledger chain accounts
         * @see SecretStoreV2.getAdditionalMetaAccountSecret
         */
        val additional: Map<String, String>
    ) : Identifiable {

        override val identifier: String = walletId

        class ChainAccountSecrets(
            val accountId: ByteArray,
            val entropy: ByteArray?,
            val seed: ByteArray?,
            val keypair: KeyPairSecrets,
            val derivationPath: String?,
        )

        class SubstrateSecrets(
            val seed: ByteArray?,
            val keypair: KeyPairSecrets,
            val derivationPath: String?,
        )

        class EthereumSecrets(
            val keypair: KeyPairSecrets,
            val derivationPath: String?,
        )

        class KeyPairSecrets(
            val publicKey: ByteArray,
            val privateKey: ByteArray,
            val nonce: ByteArray?
        )
    }
}
