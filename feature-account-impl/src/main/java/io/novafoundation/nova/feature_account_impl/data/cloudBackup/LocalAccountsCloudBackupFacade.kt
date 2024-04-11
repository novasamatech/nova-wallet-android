package io.novafoundation.nova.feature_account_impl.data.cloudBackup

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.address.get
import io.novafoundation.nova.common.address.intoKey
import io.novafoundation.nova.common.data.secrets.v2.ChainAccountSecrets
import io.novafoundation.nova.common.data.secrets.v2.KeyPairSchema
import io.novafoundation.nova.common.data.secrets.v2.MetaAccountSecrets
import io.novafoundation.nova.common.data.secrets.v2.SecretStoreV2
import io.novafoundation.nova.common.data.secrets.v2.derivationPath
import io.novafoundation.nova.common.data.secrets.v2.entropy
import io.novafoundation.nova.common.data.secrets.v2.ethereumDerivationPath
import io.novafoundation.nova.common.data.secrets.v2.ethereumKeypair
import io.novafoundation.nova.common.data.secrets.v2.keypair
import io.novafoundation.nova.common.data.secrets.v2.nonce
import io.novafoundation.nova.common.data.secrets.v2.privateKey
import io.novafoundation.nova.common.data.secrets.v2.publicKey
import io.novafoundation.nova.common.data.secrets.v2.seed
import io.novafoundation.nova.common.data.secrets.v2.substrateDerivationPath
import io.novafoundation.nova.common.data.secrets.v2.substrateKeypair
import io.novafoundation.nova.common.utils.filterNotNull
import io.novafoundation.nova.common.utils.findById
import io.novafoundation.nova.common.utils.mapToSet
import io.novafoundation.nova.core_db.dao.MetaAccountDao
import io.novafoundation.nova.core_db.model.chain.account.ChainAccountLocal
import io.novafoundation.nova.core_db.model.chain.account.JoinedMetaAccountInfo
import io.novafoundation.nova.core_db.model.chain.account.MetaAccountLocal
import io.novafoundation.nova.core_db.model.chain.account.RelationJoinedMetaAccountInfo
import io.novafoundation.nova.feature_account_api.data.events.MetaAccountChangesEventBus
import io.novafoundation.nova.feature_account_api.data.events.MetaAccountChangesEventBus.Event.AccountAdded
import io.novafoundation.nova.feature_account_api.data.events.MetaAccountChangesEventBus.Event.AccountNameChanged
import io.novafoundation.nova.feature_account_api.data.events.MetaAccountChangesEventBus.Event.AccountRemoved
import io.novafoundation.nova.feature_account_api.data.events.MetaAccountChangesEventBus.Event.AccountStructureChanged
import io.novafoundation.nova.feature_account_api.data.events.buildChangesEvent
import io.novafoundation.nova.feature_account_impl.data.mappers.mapMetaAccountTypeFromLocal
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.CloudBackup
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.CloudBackup.WalletPrivateInfo
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.diff.CloudBackupDiff
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.diff.isEmpty
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.diff.localVsCloudDiff
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.diff.strategy.BackupDiffStrategyFactory
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors.CannotApplyNonDestructiveDiff
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.isCompletelyEmpty
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novasama.substrate_sdk_android.encrypt.keypair.BaseKeypair
import io.novasama.substrate_sdk_android.encrypt.keypair.Keypair
import io.novasama.substrate_sdk_android.encrypt.keypair.substrate.Sr25519Keypair
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.scale.EncodableStruct

interface LocalAccountsCloudBackupFacade {

    /**
     * Constructs full backup instance, including sensitive information
     * Should only be used when full backup instance is needed, for example when writing backup to cloud. Otherwise use [publicBackupInfoFromLocalSnapshot]
     *
     * Important note: Should only be called as the result of direct user interaction!
     * We don't want to exposure secrets to RAM until user explicitly directs app to do so
     */
    suspend fun fullBackupInfoFromLocalSnapshot(): CloudBackup

    /**
     * Constructs partial backup instance, including only the public information (addresses, metadata e.t.c)
     *
     * Can be used without direct user interaction (e.g. in background) to compare backup states between local and remote sources
     */
    suspend fun publicBackupInfoFromLocalSnapshot(): CloudBackup.PublicData

    /**
     * Creates a backup from external input. Useful for creating initial backup
     */
    suspend fun createCloudBackupFromInput(
        modificationTime: Long,
        metaAccount: MetaAccountLocal,
        chainAccounts: List<ChainAccountLocal>,
        baseSecrets: EncodableStruct<MetaAccountSecrets>,
        chainAccountSecrets: Map<String, EncodableStruct<ChainAccountSecrets>>,
        additionalSecrets: Map<String, String>
    ): CloudBackup

    /**
     * Check if it is possible to apply given [diff] to local state in non-destructive manner
     * In other words, whether it is possible to apply backup without notifying the user
     */
    suspend fun canPerformNonDestructiveApply(diff: CloudBackupDiff): Boolean

    /**
     * Applies cloud version of the backup to the local state.
     * This is a destructive action as may overwrite or delete secrets stored in the app
     *
     * Important note: Should only be called as the result of direct user interaction!
     */
    suspend fun applyBackupDiff(diff: CloudBackupDiff, cloudVersion: CloudBackup)
}

/**
 * Attempts to apply cloud backup version to current local application state in non-destructive manner
 * Will do nothing if it is not possible to apply changes in non-destructive manner
 *
 * @return whether the attempt succeeded
 */
suspend fun LocalAccountsCloudBackupFacade.applyNonDestructiveCloudVersionOrThrow(
    cloudVersion: CloudBackup,
    diffStrategy: BackupDiffStrategyFactory
): CloudBackupDiff {
    val localSnapshot = publicBackupInfoFromLocalSnapshot()
    val diff = localSnapshot.localVsCloudDiff(cloudVersion.publicData, diffStrategy)

    return if (canPerformNonDestructiveApply(diff)) {
        applyBackupDiff(diff, cloudVersion)

        diff
    } else {
        throw CannotApplyNonDestructiveDiff()
    }
}

class RealLocalAccountsCloudBackupFacade(
    private val secretsStoreV2: SecretStoreV2,
    private val accountDao: MetaAccountDao,
    private val cloudBackupAccountsModificationsTracker: CloudBackupAccountsModificationsTracker,
    private val metaAccountChangedEvents: MetaAccountChangesEventBus,
) : LocalAccountsCloudBackupFacade {

    override suspend fun fullBackupInfoFromLocalSnapshot(): CloudBackup {
        val allBackupableAccounts = getAllBackupableAccounts()

        return CloudBackup(
            publicData = allBackupableAccounts.toBackupPublicData(),
            privateData = preparePrivateBackupData(allBackupableAccounts)
        )
    }

    override suspend fun publicBackupInfoFromLocalSnapshot(): CloudBackup.PublicData {
        val allBackupableAccounts = getAllBackupableAccounts()

        return allBackupableAccounts.toBackupPublicData()
    }

    override suspend fun createCloudBackupFromInput(
        modificationTime: Long,
        metaAccount: MetaAccountLocal,
        chainAccounts: List<ChainAccountLocal>,
        baseSecrets: EncodableStruct<MetaAccountSecrets>,
        chainAccountSecrets: Map<ChainId, EncodableStruct<ChainAccountSecrets>>,
        additionalSecrets: Map<String, String>
    ): CloudBackup {
        val wrappedMetaAccount = listOf(RelationJoinedMetaAccountInfo(metaAccount, chainAccounts, null))

        val backupChainAccounts = chainAccounts.mapNotNull { chainAccountLocal ->
            chainAccountSecrets[chainAccountLocal.chainId]?.toBackupSecrets(chainAccountLocal.accountId)
        }
        val walletPrivateInfo = WalletPrivateInfo(
            walletId = metaAccount.globallyUniqueId,
            entropy = baseSecrets.entropy,
            substrate = baseSecrets.getSubstrateBackupSecrets(),
            ethereum = baseSecrets.getEthereumBackupSecrets(),
            chainAccounts = backupChainAccounts,
            additional = additionalSecrets
        )

        return CloudBackup(
            publicData = wrappedMetaAccount.toBackupPublicData(modifiedAt = modificationTime),
            privateData = CloudBackup.PrivateData(
                wallets = listOf(walletPrivateInfo)
            )
        )
    }

    override suspend fun canPerformNonDestructiveApply(diff: CloudBackupDiff): Boolean {
        return diff.localChanges.modified.isEmpty() && diff.localChanges.removed.isEmpty()
    }

    override suspend fun applyBackupDiff(diff: CloudBackupDiff, cloudVersion: CloudBackup) {
        val localChangesToApply = diff.localChanges
        if (localChangesToApply.isEmpty()) return

        val metaAccountsByUuid = getAllBackupableAccounts().associateBy { it.metaAccount.globallyUniqueId }

        val changesEvent = buildChangesEvent {
            accountDao.withTransaction {
                addAll(applyLocalRemoval(localChangesToApply.removed, metaAccountsByUuid))
                addAll(applyLocalAddition(localChangesToApply.added, cloudVersion))
                addAll(applyLocalModification(localChangesToApply.modified, cloudVersion, metaAccountsByUuid))
            }
        }

        changesEvent?.let { metaAccountChangedEvents.notify(it) }
    }

    private suspend fun applyLocalRemoval(
        toRemove: List<CloudBackup.WalletPublicInfo>,
        metaAccountsByUUid: Map<String, JoinedMetaAccountInfo>
    ): List<AccountRemoved> {
        if (toRemove.isEmpty()) return emptyList()

        val localIds = toRemove.mapNotNull { metaAccountsByUUid[it.walletId]?.metaAccount?.id }
        accountDao.delete(localIds)

        return toRemove.mapNotNull {
            val localWallet = metaAccountsByUUid[it.walletId] ?: return@mapNotNull null
            val chainAccountIds = localWallet.chainAccounts.map(ChainAccountLocal::accountId)

            secretsStoreV2.clearSecrets(localWallet.metaAccount.id, chainAccountIds)

            AccountRemoved(
                metaId = localWallet.metaAccount.id,
                metaAccountType = mapMetaAccountTypeFromLocal(localWallet.metaAccount.type)
            )
        }
    }

    private suspend fun applyLocalAddition(
        toAdd: List<CloudBackup.WalletPublicInfo>,
        cloudBackup: CloudBackup
    ): List<AccountAdded> {
        return toAdd.map { publicWalletInfo ->
            val metaAccountLocal = publicWalletInfo.toMetaAccountLocal(
                accountPosition = accountDao.nextAccountPosition(),
                localIdOverwrite = null,
                isSelected = false
            )
            val metaId = accountDao.insertMetaAccount(metaAccountLocal)

            val chainAccountsLocal = publicWalletInfo.getChainAccountsLocal(metaId)
            accountDao.insertChainAccounts(chainAccountsLocal)

            val metaAccountSecrets = cloudBackup.getMetaAccountSecrets(publicWalletInfo.walletId)
            metaAccountSecrets?.let {
                secretsStoreV2.putMetaAccountSecrets(metaId, metaAccountSecrets)
            }

            val chainAccountsSecrets = cloudBackup.getAllChainAccountSecrets(publicWalletInfo)
            chainAccountsSecrets.forEach { (accountId, secrets) ->
                secretsStoreV2.putChainAccountSecrets(metaId, accountId.value, secrets)
            }

            AccountAdded(
                metaId = metaId,
                metaAccountType = mapMetaAccountTypeFromLocal(metaAccountLocal.type)
            )
        }
    }

    /**
     * Modification of each meta account is done in the following steps:
     *
     * Insert updated MetaAccountLocal
     *
     * Delete all previous ChainAccountLocal associated with currently processed meta account
     * Insert all ChainAccountLocal from backup
     *
     * Update meta account secrets
     * Delete all chain account secrets  associated with currently processed meta account
     * Insert all chain account secrets from backup
     */
    private suspend fun applyLocalModification(
        toModify: List<CloudBackup.WalletPublicInfo>,
        cloudVersion: CloudBackup,
        localMetaAccountsByUUid: Map<String, JoinedMetaAccountInfo>
    ): List<MetaAccountChangesEventBus.Event> {
        // There seems to be some bug in Kotlin compiler which prevents us to use `return flatMap` here:
        // Some internal assertion in compiler fails with error "cannot cal suspend function without continuation"
        // The closest issue I have found: https://youtrack.jetbrains.com/issue/KT-48319/JVM-IR-AssertionError-FUN-caused-by-suspend-lambda-inside-anonymous-function
        val result = mutableListOf<MetaAccountChangesEventBus.Event>()

        toModify.onEach { publicWalletInfo ->
            val oldMetaAccountJoinInfo = localMetaAccountsByUUid[publicWalletInfo.walletId] ?: return@onEach
            val oldMetaAccountLocal = oldMetaAccountJoinInfo.metaAccount
            val metaId = oldMetaAccountLocal.id

            // Insert updated MetaAccountLocal
            val updatedMetaAccountLocal = publicWalletInfo.toMetaAccountLocal(
                accountPosition = oldMetaAccountLocal.position,
                localIdOverwrite = metaId,
                isSelected = oldMetaAccountLocal.isSelected
            )
            accountDao.updateMetaAccount(updatedMetaAccountLocal)

            // Delete all previous ChainAccountLocal associated with currently processed meta account
            if (oldMetaAccountJoinInfo.chainAccounts.isNotEmpty()) {
                accountDao.deleteChainAccounts(oldMetaAccountJoinInfo.chainAccounts)
            }

            // Insert all ChainAccountLocal from backup
            val updatedChainAccountsLocal = publicWalletInfo.getChainAccountsLocal(metaId)
            if (updatedChainAccountsLocal.isNotEmpty()) {
                accountDao.insertChainAccounts(updatedChainAccountsLocal)
            }

            // Update meta account secrets
            val metaAccountSecrets = cloudVersion.getMetaAccountSecrets(publicWalletInfo.walletId)
            metaAccountSecrets?.let {
                secretsStoreV2.putMetaAccountSecrets(metaId, metaAccountSecrets)
            }

            // Delete all chain account secrets  associated with currently processed meta account
            val previousChainAccountIds = oldMetaAccountJoinInfo.chainAccounts.map { it.accountId }
            secretsStoreV2.clearSecrets(metaId, previousChainAccountIds)

            // Insert all chain account secrets from backup
            val chainAccountsSecrets = cloudVersion.getAllChainAccountSecrets(publicWalletInfo)
            chainAccountsSecrets.forEach { (accountId, secrets) ->
                secretsStoreV2.putChainAccountSecrets(metaId, accountId.value, secrets)
            }

            val metaAccountType = mapMetaAccountTypeFromLocal(oldMetaAccountLocal.type)

            result.add(AccountStructureChanged(metaId = metaId, metaAccountType = metaAccountType))
            result.add(AccountNameChanged(metaId = metaId, metaAccountType = metaAccountType))
        }

        return result
    }

    private fun CloudBackup.getMetaAccountSecrets(uuid: String): EncodableStruct<MetaAccountSecrets>? {
        return privateData.wallets.findById(uuid)?.getLocalMetaAccountSecrets()
    }

    private fun CloudBackup.getAllChainAccountSecrets(walletPublicInfo: CloudBackup.WalletPublicInfo): Map<AccountIdKey, EncodableStruct<ChainAccountSecrets>> {
        val privateInfo = privateData.wallets.findById(walletPublicInfo.walletId) ?: return emptyMap()

        val chainAccountsSecretsByAccountId = privateInfo.chainAccounts.associateBy { it.accountId.intoKey() }

        return walletPublicInfo.chainAccounts.associateBy(
            keySelector = { it.accountId.intoKey() },
            valueTransform = { chainAccountPublicInfo ->
                val chainAccountSecrets = chainAccountsSecretsByAccountId[chainAccountPublicInfo.accountId]

                chainAccountSecrets?.toLocalSecrets()
            }
        ).filterNotNull()
    }

    private suspend fun getAllBackupableAccounts(): List<JoinedMetaAccountInfo> {
        return accountDao.getMetaAccountsByStatus(MetaAccountLocal.Status.ACTIVE)
            .filter { it.metaAccount.type.isBackupable() }
    }

    private suspend fun preparePrivateBackupData(metaAccounts: List<JoinedMetaAccountInfo>): CloudBackup.PrivateData {
        return CloudBackup.PrivateData(
            wallets = metaAccounts
                .map { prepareWalletPrivateInfo(it) }
                .filterNot { it.isCompletelyEmpty() }
        )
    }

    private suspend fun prepareWalletPrivateInfo(joinedMetaAccountInfo: JoinedMetaAccountInfo): WalletPrivateInfo {
        val metaId = joinedMetaAccountInfo.metaAccount.id
        val baseSecrets = secretsStoreV2.getMetaAccountSecrets(metaId)

        return WalletPrivateInfo(
            walletId = joinedMetaAccountInfo.metaAccount.globallyUniqueId,
            entropy = baseSecrets?.entropy,
            substrate = baseSecrets.getSubstrateBackupSecrets(),
            ethereum = baseSecrets.getEthereumBackupSecrets(),
            chainAccounts = joinedMetaAccountInfo.chainAccounts
                .mapToSet { it.accountId.intoKey() } // multiple chain accounts might refer to the same account id - remove duplicates
                .mapNotNull { prepareChainAccountPrivateInfo(metaId, it.value) },
            additional = prepareAdditional(joinedMetaAccountInfo.metaAccount)
        )
    }

    private suspend fun prepareAdditional(metaAccountLocal: MetaAccountLocal): Map<String, String> {
        return secretsStoreV2.allKnownAdditionalSecrets(metaAccountLocal.id)
    }

    private suspend fun prepareChainAccountPrivateInfo(metaAccount: Long, chainAccountId: AccountId): WalletPrivateInfo.ChainAccountSecrets? {
        val secrets = secretsStoreV2.getChainAccountSecrets(metaAccount, chainAccountId) ?: return null

        return secrets.toBackupSecrets(chainAccountId)
    }

    private fun EncodableStruct<ChainAccountSecrets>.toBackupSecrets(chainAccountId: ByteArray): WalletPrivateInfo.ChainAccountSecrets {
        return WalletPrivateInfo.ChainAccountSecrets(
            accountId = chainAccountId,
            entropy = entropy,
            seed = seed,
            keypair = keypair.toBackupKeypairSecrets(),
            derivationPath = derivationPath
        )
    }

    private fun WalletPrivateInfo.ChainAccountSecrets.toLocalSecrets(): EncodableStruct<ChainAccountSecrets> {
        return ChainAccountSecrets(
            entropy = entropy,
            seed = seed,
            derivationPath = derivationPath,
            keyPair = keypair.toLocalKeyPair()
        )
    }

    private fun WalletPrivateInfo.KeyPairSecrets.toLocalKeyPair(): Keypair {
        val nonce = nonce

        return if (nonce != null) {
            Sr25519Keypair(privateKey = privateKey, publicKey = publicKey, nonce = nonce)
        } else {
            BaseKeypair(privateKey = privateKey, publicKey = publicKey)
        }
    }

    private fun WalletPrivateInfo.getLocalMetaAccountSecrets(): EncodableStruct<MetaAccountSecrets>? {
        return MetaAccountSecrets(
            entropy = entropy,
            seed = substrate?.seed,
            substrateKeyPair = substrate?.keypair?.toLocalKeyPair() ?: return null,
            substrateDerivationPath = substrate?.derivationPath,
            ethereumKeypair = ethereum?.keypair?.toLocalKeyPair(),
            ethereumDerivationPath = ethereum?.derivationPath
        )
    }

    private fun EncodableStruct<MetaAccountSecrets>?.getEthereumBackupSecrets(): WalletPrivateInfo.EthereumSecrets? {
        if (this == null) return null

        return WalletPrivateInfo.EthereumSecrets(
            keypair = ethereumKeypair?.toBackupKeypairSecrets() ?: return null,
            derivationPath = ethereumDerivationPath
        )
    }

    private fun EncodableStruct<MetaAccountSecrets>?.getSubstrateBackupSecrets(): WalletPrivateInfo.SubstrateSecrets? {
        if (this == null) return null

        return WalletPrivateInfo.SubstrateSecrets(
            seed = seed,
            keypair = substrateKeypair.toBackupKeypairSecrets(),
            derivationPath = substrateDerivationPath
        )
    }

    private fun EncodableStruct<KeyPairSchema>.toBackupKeypairSecrets(): WalletPrivateInfo.KeyPairSecrets {
        return WalletPrivateInfo.KeyPairSecrets(
            publicKey = publicKey,
            privateKey = privateKey,
            nonce = nonce
        )
    }

    private fun List<JoinedMetaAccountInfo>.toBackupPublicData(
        modifiedAt: Long = cloudBackupAccountsModificationsTracker.getAccountsLastModifiedAt()
    ): CloudBackup.PublicData {
        return CloudBackup.PublicData(
            modifiedAt = modifiedAt,
            wallets = mapNotNull { it.toBackupPublicInfo() },
        )
    }

    private fun JoinedMetaAccountInfo.toBackupPublicInfo(): CloudBackup.WalletPublicInfo? {
        return CloudBackup.WalletPublicInfo(
            walletId = metaAccount.globallyUniqueId,
            substratePublicKey = metaAccount.substratePublicKey,
            substrateAccountId = metaAccount.substrateAccountId,
            substrateCryptoType = metaAccount.substrateCryptoType,
            ethereumAddress = metaAccount.ethereumAddress,
            ethereumPublicKey = metaAccount.ethereumPublicKey,
            name = metaAccount.name,
            type = metaAccount.type.toBackupWalletType() ?: return null,
            chainAccounts = chainAccounts.mapToSet { chainAccount -> chainAccount.toBackupPublicChainAccount() }
        )
    }

    private fun CloudBackup.WalletPublicInfo.toMetaAccountLocal(
        accountPosition: Int,
        localIdOverwrite: Long?,
        isSelected: Boolean
    ): MetaAccountLocal {
        return MetaAccountLocal(
            substratePublicKey = substratePublicKey,
            substrateAccountId = substrateAccountId,
            substrateCryptoType = substrateCryptoType,
            ethereumAddress = ethereumAddress,
            ethereumPublicKey = ethereumPublicKey,
            name = name,
            type = type.toLocalWalletType(),
            globallyUniqueId = walletId,
            parentMetaId = null,
            isSelected = isSelected,
            position = accountPosition,
            status = MetaAccountLocal.Status.ACTIVE
        ).also {
            if (localIdOverwrite != null) {
                it.id = localIdOverwrite
            }
        }
    }

    private fun CloudBackup.WalletPublicInfo.getChainAccountsLocal(metaId: Long): List<ChainAccountLocal> {
        return chainAccounts.map {
            ChainAccountLocal(
                metaId = metaId,
                chainId = it.chainId,
                publicKey = it.publicKey,
                accountId = it.accountId,
                cryptoType = it.cryptoType
            )
        }
    }

    private fun ChainAccountLocal.toBackupPublicChainAccount(): CloudBackup.WalletPublicInfo.ChainAccountInfo {
        return CloudBackup.WalletPublicInfo.ChainAccountInfo(
            chainId = chainId,
            publicKey = publicKey,
            accountId = accountId,
            cryptoType = cryptoType
        )
    }

    private fun MetaAccountLocal.Type.toBackupWalletType(): CloudBackup.WalletPublicInfo.Type? {
        return when (this) {
            MetaAccountLocal.Type.SECRETS -> CloudBackup.WalletPublicInfo.Type.SECRETS
            MetaAccountLocal.Type.WATCH_ONLY -> CloudBackup.WalletPublicInfo.Type.WATCH_ONLY
            MetaAccountLocal.Type.PARITY_SIGNER -> CloudBackup.WalletPublicInfo.Type.PARITY_SIGNER
            MetaAccountLocal.Type.LEDGER -> CloudBackup.WalletPublicInfo.Type.LEDGER
            MetaAccountLocal.Type.POLKADOT_VAULT -> CloudBackup.WalletPublicInfo.Type.POLKADOT_VAULT
            MetaAccountLocal.Type.PROXIED -> null
        }
    }

    private fun CloudBackup.WalletPublicInfo.Type.toLocalWalletType(): MetaAccountLocal.Type {
        return when (this) {
            CloudBackup.WalletPublicInfo.Type.SECRETS -> MetaAccountLocal.Type.SECRETS
            CloudBackup.WalletPublicInfo.Type.WATCH_ONLY -> MetaAccountLocal.Type.WATCH_ONLY
            CloudBackup.WalletPublicInfo.Type.PARITY_SIGNER -> MetaAccountLocal.Type.PARITY_SIGNER
            CloudBackup.WalletPublicInfo.Type.LEDGER -> MetaAccountLocal.Type.LEDGER
            CloudBackup.WalletPublicInfo.Type.POLKADOT_VAULT -> MetaAccountLocal.Type.POLKADOT_VAULT
        }
    }
}
