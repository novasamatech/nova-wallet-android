package io.novafoundation.feature_cloud_backup_test

import io.novafoundation.nova.core.model.CryptoType
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.CloudBackup
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.CloudBackup.WalletPrivateInfo
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.CloudBackup.WalletPrivateInfo.ChainAccountSecrets
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.CloudBackup.WalletPrivateInfo.EthereumSecrets
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.CloudBackup.WalletPrivateInfo.KeyPairSecrets
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.CloudBackup.WalletPrivateInfo.SubstrateSecrets
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.CloudBackup.WalletPublicInfo
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.CloudBackup.WalletPublicInfo.ChainAccountInfo
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.CloudBackup.WalletPublicInfo.ChainAccountInfo.ChainAccountCryptoType
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.isCompletelyEmpty
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novasama.substrate_sdk_android.runtime.AccountId

const val TEST_MODIFIED_AT = 0L

@DslMarker
annotation class CloudBackupBuildDsl

@CloudBackupBuildDsl
fun buildTestCloudBackup(builder: CloudBackupBuilder.() -> Unit): CloudBackup {
    return CloudBackupBuilder().apply(builder).build()
}

@CloudBackupBuildDsl
class CloudBackupBuilder {

    private var privateData: CloudBackup.PrivateData? = null
    private var publicData: CloudBackup.PublicData? = null

    fun publicData(
        builder: CloudBackupPublicDataBuilder.() -> Unit
    ) {
        publicData = CloudBackupPublicDataBuilder().apply(builder).build()
    }

    fun privateData(
        builder: CloudBackupPrivateDataBuilder.() -> Unit
    ) {
        privateData = CloudBackupPrivateDataBuilder().apply(builder).build()
    }

    fun build(): CloudBackup {
        return CloudBackup(
            publicData = requireNotNull(publicData),
            privateData = requireNotNull(privateData)
        )
    }
}

@CloudBackupBuildDsl
class CloudBackupPublicDataBuilder {

    private var _modifiedAt: Long = TEST_MODIFIED_AT

    private val wallets = mutableListOf<WalletPublicInfo>()

    fun modifiedAt(value: Long) {
        _modifiedAt = value
    }

    fun wallet(walletId: String, builder: WalletPublicInfoBuilder.() -> Unit): WalletPublicInfo {
        val element = WalletPublicInfoBuilder(walletId).apply(builder).build()
        wallets.add(element)
        return element
    }

    fun build(): CloudBackup.PublicData {
        return CloudBackup.PublicData(_modifiedAt, wallets)
    }
}

@CloudBackupBuildDsl
class CloudBackupPrivateDataBuilder {

    private val wallets = mutableListOf<WalletPrivateInfo>()

    fun wallet(walletId: String, builder: WalletPrivateInfoBuilder.() -> Unit) {
        val privateInfo = WalletPrivateInfoBuilder(walletId).apply(builder).build()

        if (!privateInfo.isCompletelyEmpty()) {
            wallets.add(privateInfo)
        }
    }

    fun build(): CloudBackup.PrivateData {
        return CloudBackup.PrivateData(wallets)
    }
}

@CloudBackupBuildDsl
class WalletPrivateInfoBuilder(
    private val walletId: String,
) {

    private var _entropy: ByteArray? = null

    private var substrate: SubstrateSecrets? = null
    private var ethereum: EthereumSecrets? = null

    private val chainAccounts = mutableListOf<ChainAccountSecrets>()

    fun entropy(value: ByteArray) {
        _entropy = value
    }

    fun substrate(builder: BackupSubstrateSecretsBuilder.() -> Unit) {
        substrate = BackupSubstrateSecretsBuilder().apply(builder).build()
    }

    fun ethereum(builder: BackupEthereumSecretsBuilder.() -> Unit) {
        ethereum = BackupEthereumSecretsBuilder().apply(builder).build()
    }

    fun chainAccount(accountId: AccountId, builder: (BackupChainAccountSecretsBuilder.() -> Unit)? = null) {
        val element = BackupChainAccountSecretsBuilder(accountId).apply { builder?.invoke(this) }.build()
        chainAccounts.add(element)
    }

    fun build(): WalletPrivateInfo {
        return WalletPrivateInfo(
            entropy = _entropy,
            walletId = walletId,
            substrate = substrate,
            ethereum = ethereum,
            chainAccounts = chainAccounts,
        )
    }
}

@CloudBackupBuildDsl
class BackupSubstrateSecretsBuilder {

    private var _keypair: KeyPairSecrets? = null
    private var _seed: ByteArray? = null
    private var _derivationPath: String? = null

    fun seed(value: ByteArray) {
        _seed = value
    }

    fun derivationPath(value: String?) {
        _derivationPath = value
    }

    fun keypair(keypair: KeyPairSecrets) {
        _keypair = keypair
    }

    fun build(): SubstrateSecrets {
        return SubstrateSecrets(_seed, _keypair, _derivationPath)
    }
}

@CloudBackupBuildDsl
class BackupEthereumSecretsBuilder {

    private var _keypair: KeyPairSecrets? = null
    private var _derivationPath: String? = null

    fun derivationPath(value: String?) {
        _derivationPath = value
    }

    fun keypair(keypair: KeyPairSecrets) {
        _keypair = keypair
    }

    fun build(): EthereumSecrets {
        return EthereumSecrets(requireNotNull(_keypair), _derivationPath)
    }
}

@CloudBackupBuildDsl
class BackupChainAccountSecretsBuilder(private val accountId: AccountId) {

    private var _entropy: ByteArray? = null

    private var _keypair: KeyPairSecrets? = null
    private var _seed: ByteArray? = null
    private var _derivationPath: String? = null

    fun entropy(value: ByteArray) {
        _entropy = value
    }

    fun seed(value: ByteArray) {
        _seed = value
    }

    fun derivationPath(value: String?) {
        _derivationPath = value
    }

    fun keypair(keypair: KeyPairSecrets) {
        _keypair = keypair
    }

    fun keypairFromIndex(index: Int) {
        _keypair = KeyPairSecrets(
            privateKey = ByteArray(32) { index.toByte() },
            publicKey = ByteArray(32) { index.toByte() },
            nonce = ByteArray(32) { index.toByte() }
        )
    }

    fun build(): ChainAccountSecrets {
        return ChainAccountSecrets(accountId, _entropy, _seed, _keypair, _derivationPath)
    }
}

@CloudBackupBuildDsl
class WalletPublicInfoBuilder(
    private val walletId: String,
) {

    private val chainAccounts = mutableListOf<ChainAccountInfo>()

    private var _substratePublicKey: ByteArray? = null
    private var _substrateCryptoType: CryptoType? = null
    private var _substrateAccountId: ByteArray? = null
    private var _ethereumPublicKey: ByteArray? = null
    private var _ethereumAddress: ByteArray? = null
    private var _name: String = ""
    private var _isSelected: Boolean = false
    private var _type: WalletPublicInfo.Type = WalletPublicInfo.Type.SECRETS

    fun chainAccount(chainId: ChainId, builder: WalletChainAccountInfoBuilder.() -> Unit) {
        val chainAccountLocal = WalletChainAccountInfoBuilder(chainId).apply(builder).build()
        chainAccounts.add(chainAccountLocal)
    }

    fun substratePublicKey(value: ByteArray?) {
        _substratePublicKey = value
    }

    fun substrateCryptoType(value: CryptoType?) {
        _substrateCryptoType = value
    }

    fun substrateAccountId(value: ByteArray?) {
        _substrateAccountId = value
    }

    fun ethereumPublicKey(value: ByteArray?) {
        _ethereumPublicKey = value
    }

    fun ethereumAddress(value: ByteArray?) {
        _ethereumAddress = value
    }

    fun name(value: String) {
        _name = value
    }

    fun isSelected(value: Boolean) {
        _isSelected = value
    }

    fun type(value: WalletPublicInfo.Type) {
        _type = value
    }

    fun build(): WalletPublicInfo {
        return WalletPublicInfo(
            walletId = walletId,
            substratePublicKey = _substratePublicKey,
            substrateAccountId = _substrateAccountId,
            substrateCryptoType = _substrateCryptoType,
            ethereumAddress = _ethereumAddress,
            ethereumPublicKey = _ethereumPublicKey,
            name = _name,
            type = _type,
            chainAccounts = chainAccounts.toSet()
        )
    }
}

@CloudBackupBuildDsl
class WalletChainAccountInfoBuilder(
    private val chainId: ChainId,
) {

    private var _publicKey: ByteArray? = null
    private var _accountId: ByteArray = ByteArray(32)
    private var _cryptoType: ChainAccountCryptoType? = null

    fun publicKey(value: ByteArray) {
        _publicKey = value
    }

    fun accountId(value: ByteArray) {
        _accountId = value
    }

    fun cryptoType(cryptoType: ChainAccountCryptoType) {
        _cryptoType = cryptoType
    }

    fun build(): ChainAccountInfo {
        return ChainAccountInfo(chainId, _publicKey, _accountId, _cryptoType)
    }
}
