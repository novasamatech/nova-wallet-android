package io.novafoundation.nova.feature_account_impl.domain.common

import io.novafoundation.nova.common.data.secrets.v2.ChainAccountSecrets
import io.novafoundation.nova.common.data.secrets.v2.MetaAccountSecrets
import io.novafoundation.nova.common.data.secrets.v2.derivationPath
import io.novafoundation.nova.common.data.secrets.v2.ethereumDerivationPath
import io.novafoundation.nova.common.data.secrets.v2.ethereumKeypair
import io.novafoundation.nova.common.data.secrets.v2.keypair
import io.novafoundation.nova.common.data.secrets.v2.nonce
import io.novafoundation.nova.common.data.secrets.v2.privateKey
import io.novafoundation.nova.common.data.secrets.v2.substrateDerivationPath
import io.novafoundation.nova.common.data.secrets.v2.substrateKeypair
import io.novafoundation.nova.core_db.model.chain.account.ChainAccountLocal
import io.novafoundation.nova.core_db.model.chain.account.MetaAccountLocal
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.CloudBackup
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.CloudBackup.WalletPrivateInfo
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.CloudBackup.WalletPublicInfo
import io.novasama.substrate_sdk_android.scale.EncodableStruct

fun mapLocalAccountToCloudBackup(
    modificationTime: Long,
    metaAccount: MetaAccountLocal,
    chainAccounts: List<ChainAccountLocal>,
    baseSecrets: EncodableStruct<MetaAccountSecrets>,
    chainAccountSecrets: Map<String, EncodableStruct<ChainAccountSecrets>>,
    additionalSecrets: Map<String, String>
): CloudBackup {
    return CloudBackup(
        publicData = CloudBackup.PublicData(
            modificationTime,
            listOf(
                WalletPublicInfo(
                    walletId = metaAccount.globallyUniqueId,
                    substratePublicKey = metaAccount.substratePublicKey,
                    substrateAccountId = metaAccount.substrateAccountId,
                    substrateCryptoType = metaAccount.substrateCryptoType,
                    ethereumAddress = metaAccount.ethereumAddress,
                    ethereumPublicKey = metaAccount.ethereumPublicKey,
                    name = metaAccount.name,
                    type = mapAccountTypeFromLocal(metaAccount.type),
                    chainAccounts = chainAccounts.map { mapLocalChainAccountToPublicCloudBackup(it) }
                )
            )
        ),
        privateData = CloudBackup.PrivateData(
            wallets = listOf(
                WalletPrivateInfo(
                    walletId = metaAccount.globallyUniqueId,
                    entropy = baseSecrets.get(MetaAccountSecrets.Entropy),
                    substrate = getSubstrateSecrets(baseSecrets),
                    ethereum = getEthereumSecrets(baseSecrets),
                    chainAccounts = chainAccountSecrets.map { (chainId, secrets) -> mapLocalChainAccountToPrivateCloudBackup(chainId, secrets) },
                    additional = additionalSecrets
                )
            )
        )
    )
}

fun mapLocalChainAccountToPrivateCloudBackup(chainId: String, secrets: EncodableStruct<ChainAccountSecrets>): WalletPrivateInfo.ChainAccountSecrets {
    return WalletPrivateInfo.ChainAccountSecrets(
        chainId = chainId,
        entropy = secrets.get(ChainAccountSecrets.Entropy),
        seed = secrets.get(ChainAccountSecrets.Seed),
        keypair = WalletPrivateInfo.KeyPairSecrets(
            privateKey = secrets.keypair.privateKey,
            nonce = secrets.keypair.nonce
        ),
        derivationPath = secrets.derivationPath
    )
}

private fun getSubstrateSecrets(
    baseSecrets: EncodableStruct<MetaAccountSecrets>
): WalletPrivateInfo.SubstrateSecrets {
    return WalletPrivateInfo.SubstrateSecrets(
        seed = baseSecrets.get(MetaAccountSecrets.Seed),
        keypair = WalletPrivateInfo.KeyPairSecrets(
            privateKey = baseSecrets.substrateKeypair.privateKey,
            nonce = baseSecrets.substrateKeypair.nonce
        ),
        derivationPath = baseSecrets.substrateDerivationPath
    )
}

private fun getEthereumSecrets(
    baseSecrets: EncodableStruct<MetaAccountSecrets>
): WalletPrivateInfo.EthereumSecrets? {
    return baseSecrets.ethereumKeypair?.privateKey?.let {
        WalletPrivateInfo.EthereumSecrets(
            privateKey = it,
            derivationPath = baseSecrets.ethereumDerivationPath
        )
    }
}

fun mapLocalChainAccountToPublicCloudBackup(account: ChainAccountLocal): WalletPublicInfo.ChainAccountInfo {
    return WalletPublicInfo.ChainAccountInfo(
        chainId = account.chainId,
        publicKey = account.publicKey,
        accountId = account.accountId,
        cryptoType = account.cryptoType
    )
}

private fun mapAccountTypeFromLocal(localType: MetaAccountLocal.Type): WalletPublicInfo.Type {
    return when (localType) {
        MetaAccountLocal.Type.SECRETS -> WalletPublicInfo.Type.SECRETS
        MetaAccountLocal.Type.WATCH_ONLY -> WalletPublicInfo.Type.WATCH_ONLY
        MetaAccountLocal.Type.PARITY_SIGNER -> WalletPublicInfo.Type.PARITY_SIGNER
        MetaAccountLocal.Type.LEDGER -> WalletPublicInfo.Type.LEDGER
        MetaAccountLocal.Type.POLKADOT_VAULT -> WalletPublicInfo.Type.POLKADOT_VAULT
        MetaAccountLocal.Type.PROXIED -> throw IllegalStateException("Proxied accounts are not supported to backup")
    }
}

suspend fun mapLocalAccountFromCloudBackup(wallet: WalletPublicInfo, metaAccountSortPosition: suspend () -> Int): MetaAccountLocal {
    return MetaAccountLocal(
        substratePublicKey = wallet.substratePublicKey,
        substrateCryptoType = wallet.substrateCryptoType,
        substrateAccountId = wallet.substrateAccountId,
        ethereumAddress = wallet.ethereumAddress,
        ethereumPublicKey = wallet.ethereumPublicKey,
        name = wallet.name,
        position = metaAccountSortPosition(),
        isSelected = false,
        parentMetaId = null,
        status = MetaAccountLocal.Status.ACTIVE,
        type = mapAccountTypeToLocal(wallet.type),
        globallyUniqueId = wallet.walletId
    )
}

fun mapLocalChainAccountsFromCloudBackup(metaId: Long, account: WalletPublicInfo): List<ChainAccountLocal> {
    return account.chainAccounts.map {
        mapLocalChainAccountFromCloudBackup(metaId, it)
    }
}

fun mapLocalChainAccountFromCloudBackup(metaId: Long, account: WalletPublicInfo.ChainAccountInfo): ChainAccountLocal {
    return ChainAccountLocal(
        metaId = metaId,
        chainId = account.chainId,
        publicKey = account.publicKey,
        accountId = account.accountId,
        cryptoType = account.cryptoType
    )
}

private fun mapAccountTypeToLocal(localType: WalletPublicInfo.Type): MetaAccountLocal.Type {
    return when (localType) {
        WalletPublicInfo.Type.SECRETS -> MetaAccountLocal.Type.SECRETS
        WalletPublicInfo.Type.WATCH_ONLY -> MetaAccountLocal.Type.WATCH_ONLY
        WalletPublicInfo.Type.PARITY_SIGNER -> MetaAccountLocal.Type.PARITY_SIGNER
        WalletPublicInfo.Type.LEDGER -> MetaAccountLocal.Type.LEDGER
        WalletPublicInfo.Type.POLKADOT_VAULT -> MetaAccountLocal.Type.POLKADOT_VAULT
    }
}
