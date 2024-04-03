package io.novafoundation.nova.feature_account_impl.domain.common

import io.novafoundation.nova.core_db.model.chain.account.ChainAccountLocal
import io.novafoundation.nova.core_db.model.chain.account.MetaAccountLocal
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.CloudBackup
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.CloudBackupWallet

fun mapLocalAccountToCloudBackup(modificationTime: Long, metaAccount: MetaAccountLocal, chainAccounts: List<ChainAccountLocal>): CloudBackup {
    return CloudBackup(
        modificationTime,
        listOf(
            CloudBackupWallet(
                id = metaAccount.id,
                substratePublicKey = metaAccount.substratePublicKey,
                substrateCryptoType = metaAccount.substrateCryptoType,
                substrateAccountId = metaAccount.substrateAccountId,
                ethereumAddress = metaAccount.ethereumAddress,
                ethereumPublicKey = metaAccount.ethereumPublicKey,
                name = metaAccount.name,
                type = mapAccountTypeFromLocal(metaAccount.type),
                chainAccounts = chainAccounts.map { mapLocalChainAccountToCloudBackup(it) }
            )
        )
    )
}

fun mapLocalChainAccountToCloudBackup(account: ChainAccountLocal): CloudBackupWallet.ChainAccount {
    return CloudBackupWallet.ChainAccount(
        chainId = account.chainId,
        publicKey = account.publicKey,
        accountId = account.accountId,
        cryptoType = account.cryptoType
    )
}

private fun mapAccountTypeFromLocal(localType: MetaAccountLocal.Type): CloudBackupWallet.Type {
    return when (localType) {
        MetaAccountLocal.Type.SECRETS -> CloudBackupWallet.Type.SECRETS
        MetaAccountLocal.Type.WATCH_ONLY -> CloudBackupWallet.Type.WATCH_ONLY
        MetaAccountLocal.Type.PARITY_SIGNER -> CloudBackupWallet.Type.PARITY_SIGNER
        MetaAccountLocal.Type.LEDGER -> CloudBackupWallet.Type.LEDGER
        MetaAccountLocal.Type.POLKADOT_VAULT -> CloudBackupWallet.Type.POLKADOT_VAULT
        MetaAccountLocal.Type.PROXIED -> throw IllegalStateException("Proxied accounts are not supported to backup")
    }
}
