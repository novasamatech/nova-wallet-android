package io.novafoundation.nova.feature_cloud_backup_api.domain.model

import io.novafoundation.nova.core.model.CryptoType
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

class CloudBackup(
    val modifiedAt: Long,
    val wallets: List<CloudBackupWallet>
)

class CloudBackupWallet(
    val substratePublicKey: ByteArray?,
    val substrateCryptoType: CryptoType?,
    val substrateAccountId: ByteArray?,
    val ethereumAddress: ByteArray?,
    val ethereumPublicKey: ByteArray?,
    val name: String,
    val type: Type,
    val chainAccounts: List<ChainAccount>
) {

    class ChainAccount(
        val metaId: Long,
        val chainId: ChainId,
        val publicKey: ByteArray?,
        val accountId: ByteArray,
        val cryptoType: CryptoType?,
    )

    enum class Type {
        SECRETS, WATCH_ONLY, PARITY_SIGNER, LEDGER, POLKADOT_VAULT
    }
}
