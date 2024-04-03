package io.novafoundation.nova.feature_cloud_backup_api.domain.model

import io.novafoundation.nova.common.data.secrets.v2.SecretStoreV2
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
        val walletId: Long,
        val substratePublicKey: ByteArray?,
        val substrateAccountId: ByteArray?,
        val substrateCryptoType: CryptoType?,
        val ethereumAddress: ByteArray?,
        val ethereumPublicKey: ByteArray?,
        val name: String,
        val type: Type,
        val chainAccounts: List<ChainAccountInfo>
    ) {

        enum class Type {
            SECRETS, WATCH_ONLY, PARITY_SIGNER, LEDGER, POLKADOT_VAULT
        }

        class ChainAccountInfo(
            val chainId: ChainId,
            val publicKey: ByteArray?,
            val accountId: ByteArray,
            val cryptoType: CryptoType?,
        )
    }

    class PrivateData(
        val wallets: List<WalletPrivateInfo>
    )

    class WalletPrivateInfo(
        val walletId: Long,
        val entropy: ByteArray?,
        val substrate: SubstrateSecrets,
        val ethereum: EthereumSecrets,
        val chainAccounts: List<ChainAccountSecrets>,
        /**
         * Stores additional secrets like derivation path for ledger chain accounts
         * @see SecretStoreV2.getAdditionalMetaAccountSecret
         */
        val additional: Map<String, String>
    ) {

        class ChainAccountSecrets(
            val chainId: ChainId,
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
            val privateKey: ByteArray,
            val derivationPath: String?,
        )

        class KeyPairSecrets(
            val privateKey: ByteArray,
            val nonce: ByteArray?
        )
    }
}
