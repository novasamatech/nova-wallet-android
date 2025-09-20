package io.novafoundation.nova.feature_account_impl.data.cloudBackup

import io.novafoundation.nova.common.address.AccountIdKey
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
import io.novafoundation.nova.core.model.CryptoType
import io.novafoundation.nova.core_db.dao.MetaAccountDao
import io.novafoundation.nova.core_db.model.chain.account.ChainAccountLocal
import io.novafoundation.nova.core_db.model.chain.account.JoinedMetaAccountInfo
import io.novafoundation.nova.core_db.model.chain.account.MetaAccountLocal
import io.novafoundation.nova.core_db.model.chain.account.RelationJoinedMetaAccountInfo
import io.novafoundation.nova.feature_account_api.data.cloudBackup.CLOUD_BACKUP_APPLY_SOURCE
import io.novafoundation.nova.feature_account_api.data.cloudBackup.LocalAccountsCloudBackupFacade
import io.novafoundation.nova.feature_account_api.data.events.MetaAccountChangesEventBus
import io.novafoundation.nova.feature_account_api.data.events.buildChangesEvent
import io.novafoundation.nova.feature_account_impl.data.mappers.AccountMappers
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.CloudBackup
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.CloudBackup.WalletPublicInfo.ChainAccountInfo.ChainAccountCryptoType
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.diff.CloudBackupDiff
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.diff.isEmpty
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.diff.isNotDestructive
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.isCompletelyEmpty
import io.novafoundation.nova.feature_ledger_api.data.repository.LedgerDerivationPath
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.ChainsById
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.chainsById
import io.novasama.substrate_sdk_android.encrypt.keypair.BaseKeypair
import io.novasama.substrate_sdk_android.encrypt.keypair.Keypair
import io.novasama.substrate_sdk_android.encrypt.keypair.substrate.Sr25519Keypair
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.scale.EncodableStruct

class RealLocalAccountsCloudBackupFacade(
    private val secretsStoreV2: SecretStoreV2,
    private val accountDao: MetaAccountDao,
    private val cloudBackupAccountsModificationsTracker: CloudBackupAccountsModificationsTracker,
    private val metaAccountChangedEvents: MetaAccountChangesEventBus,
    private val chainRegistry: ChainRegistry,
    private val accountMappers: AccountMappers,
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

    override suspend fun constructCloudBackupForFirstWallet(
        metaAccount: MetaAccountLocal,
        baseSecrets: EncodableStruct<MetaAccountSecrets>,
    ): CloudBackup {
        val wrappedMetaAccount = listOf(
            RelationJoinedMetaAccountInfo(
                metaAccount = metaAccount,
                chainAccounts = emptyList(),
                proxyAccountLocal = null
            )
        )

        val walletPrivateInfo = CloudBackup.WalletPrivateInfo(
            walletId = metaAccount.globallyUniqueId,
            entropy = baseSecrets.entropy,
            substrate = baseSecrets.getSubstrateBackupSecrets(),
            ethereum = baseSecrets.getEthereumBackupSecrets(),
            chainAccounts = emptyList(),
        )

        return CloudBackup(
            publicData = wrappedMetaAccount.toBackupPublicData(modifiedAt = System.currentTimeMillis()),
            privateData = CloudBackup.PrivateData(
                wallets = listOf(walletPrivateInfo)
            )
        )
    }

    override suspend fun canPerformNonDestructiveApply(diff: CloudBackupDiff): Boolean {
        return diff.localChanges.isNotDestructive()
    }

    override suspend fun applyBackupDiff(diff: CloudBackupDiff, cloudVersion: CloudBackup) {
        val localChangesToApply = diff.localChanges
        if (localChangesToApply.isEmpty()) return

        val metaAccountsByUuid = getAllBackupableAccounts().associateBy { it.metaAccount.globallyUniqueId }

        val changesEvent = buildChangesEvent {
            accountDao.runInTransaction {
                addAll(applyLocalRemoval(localChangesToApply.removed, metaAccountsByUuid))
                addAll(applyLocalAddition(localChangesToApply.added, cloudVersion))
                addAll(
                    applyLocalModification(
                        localChangesToApply.modified,
                        cloudVersion,
                        metaAccountsByUuid
                    )
                )
            }
        }

        changesEvent?.let { metaAccountChangedEvents.notify(it, source = CLOUD_BACKUP_APPLY_SOURCE) }
    }

    private suspend fun applyLocalRemoval(
        toRemove: List<CloudBackup.WalletPublicInfo>,
        metaAccountsByUUid: Map<String, JoinedMetaAccountInfo>
    ): List<MetaAccountChangesEventBus.Event.AccountRemoved> {
        if (toRemove.isEmpty()) return emptyList()

        val localIds = toRemove.mapNotNull { metaAccountsByUUid[it.walletId]?.metaAccount?.id }
        val allAffectedMetaAccounts = accountDao.delete(localIds)

        // Clear meta account secrets
        toRemove.forEach {
            val localWallet = metaAccountsByUUid[it.walletId] ?: return@forEach
            val chainAccountIds = localWallet.chainAccounts.map(ChainAccountLocal::accountId)

            secretsStoreV2.clearMetaAccountSecrets(localWallet.metaAccount.id, chainAccountIds)
        }

        // Return changes
        return allAffectedMetaAccounts.map {
            MetaAccountChangesEventBus.Event.AccountRemoved(
                metaId = it.id,
                metaAccountType = accountMappers.mapMetaAccountTypeFromLocal(it.type)
            )
        }
    }

    private suspend fun applyLocalAddition(
        toAdd: List<CloudBackup.WalletPublicInfo>,
        cloudBackup: CloudBackup
    ): List<MetaAccountChangesEventBus.Event.AccountAdded> {
        return toAdd.map { publicWalletInfo ->
            val metaAccountLocal = publicWalletInfo.toMetaAccountLocal(
                accountPosition = accountDao.nextAccountPosition(),
                localIdOverwrite = null,
                isSelected = false
            )
            val metaId = accountDao.insertMetaAccount(metaAccountLocal)

            val chainAccountsLocal = publicWalletInfo.getChainAccountsLocal(metaId)
            if (chainAccountsLocal.isNotEmpty()) {
                accountDao.insertChainAccounts(chainAccountsLocal)
            }

            val metaAccountSecrets = cloudBackup.getMetaAccountSecrets(publicWalletInfo.walletId)
            metaAccountSecrets?.let {
                secretsStoreV2.putMetaAccountSecrets(metaId, metaAccountSecrets)
            }

            val chainAccountsSecrets = cloudBackup.getAllChainAccountSecrets(publicWalletInfo)
            chainAccountsSecrets.forEach { (accountId, secrets) ->
                secretsStoreV2.putChainAccountSecrets(metaId, accountId.value, secrets)
            }

            val additional = cloudBackup.getAllAdditionalSecrets(publicWalletInfo)
            additional.forEach { (secretName, secretValue) ->
                secretsStoreV2.putAdditionalMetaAccountSecret(metaId, secretName, secretValue)
            }

            MetaAccountChangesEventBus.Event.AccountAdded(
                metaId = metaId,
                metaAccountType = accountMappers.mapMetaAccountTypeFromLocal(metaAccountLocal.type)
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

            // Delete all chain account secrets associated with currently processed meta account
            val previousChainAccountIds = oldMetaAccountJoinInfo.chainAccounts.map { it.accountId }
            secretsStoreV2.clearChainAccountsSecrets(metaId, previousChainAccountIds)

            // Insert all chain account secrets from backup
            val chainAccountsSecrets = cloudVersion.getAllChainAccountSecrets(publicWalletInfo)
            chainAccountsSecrets.forEach { (accountId, secrets) ->
                secretsStoreV2.putChainAccountSecrets(metaId, accountId.value, secrets)
            }

            val additional = cloudVersion.getAllAdditionalSecrets(publicWalletInfo)
            additional.forEach { (secretName, secretValue) ->
                secretsStoreV2.putAdditionalMetaAccountSecret(metaId, secretName, secretValue)
            }

            val metaAccountType = accountMappers.mapMetaAccountTypeFromLocal(oldMetaAccountLocal.type)

            result.add(
                MetaAccountChangesEventBus.Event.AccountStructureChanged(
                    metaId = metaId,
                    metaAccountType = metaAccountType
                )
            )
            result.add(
                MetaAccountChangesEventBus.Event.AccountNameChanged(
                    metaId = metaId,
                    metaAccountType = metaAccountType
                )
            )
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
                val chainAccountSecrets = chainAccountsSecretsByAccountId[chainAccountPublicInfo.accountId.intoKey()]

                chainAccountSecrets?.toLocalSecrets()
            }
        ).filterNotNull()
    }

    private fun CloudBackup.getAllAdditionalSecrets(walletPublicInfo: CloudBackup.WalletPublicInfo): Map<String, String> {
        val privateInfo = privateData.wallets.findById(walletPublicInfo.walletId) ?: return emptyMap()
        val chainAccountsSecretsByAccountId = privateInfo.chainAccounts.associateBy { it.accountId.intoKey() }

        fun getAllLegacyLedgerAdditionalSecrets(): Map<String, String> {
            return walletPublicInfo.chainAccounts.mapNotNull { publicInfo ->
                val derivationPath = chainAccountsSecretsByAccountId[publicInfo.accountId.intoKey()]?.derivationPath ?: return@mapNotNull null
                val secretName = LedgerDerivationPath.legacyDerivationPathSecretKey(publicInfo.chainId)

                secretName to derivationPath
            }.toMap()
        }

        fun getAllGenericLedgerAdditionalSecrets(): Map<String, String> {
            val genericDerivationPath = privateInfo.substrate!!.derivationPath!!
            val secretName = LedgerDerivationPath.genericDerivationPathSecretKey()

            return mapOf(secretName to genericDerivationPath)
        }

        return when (walletPublicInfo.type) {
            CloudBackup.WalletPublicInfo.Type.LEDGER -> getAllLegacyLedgerAdditionalSecrets()
            CloudBackup.WalletPublicInfo.Type.LEDGER_GENERIC -> getAllGenericLedgerAdditionalSecrets()

            CloudBackup.WalletPublicInfo.Type.SECRETS,
            CloudBackup.WalletPublicInfo.Type.WATCH_ONLY,
            CloudBackup.WalletPublicInfo.Type.PARITY_SIGNER,
            CloudBackup.WalletPublicInfo.Type.POLKADOT_VAULT -> emptyMap()
        }
    }

    private suspend fun getAllBackupableAccounts(): List<JoinedMetaAccountInfo> {
        return accountDao.getMetaAccountsByStatus(MetaAccountLocal.Status.ACTIVE)
            .filter { accountMappers.mapMetaAccountTypeFromLocal(it.metaAccount.type).isBackupable() }
    }

    private suspend fun preparePrivateBackupData(metaAccounts: List<JoinedMetaAccountInfo>): CloudBackup.PrivateData {
        return CloudBackup.PrivateData(
            wallets = metaAccounts
                .map { prepareWalletPrivateInfo(it) }
                .filterNot { it.isCompletelyEmpty() }
        )
    }

    private suspend fun prepareWalletPrivateInfo(joinedMetaAccountInfo: JoinedMetaAccountInfo): CloudBackup.WalletPrivateInfo {
        val metaId = joinedMetaAccountInfo.metaAccount.id
        val baseSecrets = secretsStoreV2.getMetaAccountSecrets(metaId)

        val chainAccountsFromChainSecrets = joinedMetaAccountInfo.chainAccounts
            .mapToSet { it.accountId.intoKey() } // multiple chain accounts might refer to the same account id - remove duplicates
            .mapNotNull { prepareChainAccountPrivateInfo(metaId, it.value) }

        val chainAccountFromAdditionalSecrets = prepareChainAccountsFromAdditionalSecrets(joinedMetaAccountInfo)

        return CloudBackup.WalletPrivateInfo(
            walletId = joinedMetaAccountInfo.metaAccount.globallyUniqueId,
            entropy = baseSecrets?.entropy,
            substrate = prepareSubstrateBackupSecrets(baseSecrets, joinedMetaAccountInfo),
            ethereum = baseSecrets.getEthereumBackupSecrets(),
            chainAccounts = chainAccountsFromChainSecrets + chainAccountFromAdditionalSecrets,
        )
    }

    private suspend fun prepareSubstrateBackupSecrets(
        baseSecrets: EncodableStruct<MetaAccountSecrets>?,
        metaAccountLocal: JoinedMetaAccountInfo
    ): CloudBackup.WalletPrivateInfo.SubstrateSecrets? {
        return when (metaAccountLocal.metaAccount.type) {
            MetaAccountLocal.Type.LEDGER_GENERIC -> prepareGenericLedgerSubstrateBackupSecrets(metaAccountLocal)

            MetaAccountLocal.Type.LEDGER,
            MetaAccountLocal.Type.SECRETS,
            MetaAccountLocal.Type.WATCH_ONLY,
            MetaAccountLocal.Type.PARITY_SIGNER,
            MetaAccountLocal.Type.POLKADOT_VAULT,
            MetaAccountLocal.Type.PROXIED,
            MetaAccountLocal.Type.MULTISIG,
            MetaAccountLocal.Type.DERIVATIVE -> baseSecrets.getSubstrateBackupSecrets()
        }
    }

    private suspend fun prepareGenericLedgerSubstrateBackupSecrets(metaAccountLocal: JoinedMetaAccountInfo): CloudBackup.WalletPrivateInfo.SubstrateSecrets {
        val ledgerDerivationPathKey = LedgerDerivationPath.genericDerivationPathSecretKey()
        val ledgerDerivationPath = secretsStoreV2.getAdditionalMetaAccountSecret(metaAccountLocal.metaAccount.id, ledgerDerivationPathKey)

        return CloudBackup.WalletPrivateInfo.SubstrateSecrets(
            seed = null,
            keypair = null,
            derivationPath = ledgerDerivationPath
        )
    }

    private suspend fun prepareChainAccountsFromAdditionalSecrets(
        metaAccountLocal: JoinedMetaAccountInfo
    ): List<CloudBackup.WalletPrivateInfo.ChainAccountSecrets> {
        return when (metaAccountLocal.metaAccount.type) {
            MetaAccountLocal.Type.LEDGER -> prepareLegacyLedgerChainAccountSecrets(metaAccountLocal)

            MetaAccountLocal.Type.LEDGER_GENERIC,
            MetaAccountLocal.Type.SECRETS,
            MetaAccountLocal.Type.WATCH_ONLY,
            MetaAccountLocal.Type.PARITY_SIGNER,
            MetaAccountLocal.Type.POLKADOT_VAULT,
            MetaAccountLocal.Type.PROXIED,
            MetaAccountLocal.Type.MULTISIG,
            MetaAccountLocal.Type.DERIVATIVE -> emptyList()
        }
    }

    private suspend fun prepareLegacyLedgerChainAccountSecrets(
        ledgerAccountLocal: JoinedMetaAccountInfo
    ): List<CloudBackup.WalletPrivateInfo.ChainAccountSecrets> {
        return ledgerAccountLocal.chainAccounts.map { chainAccountLocal ->
            val ledgerDerivationPathKey = LedgerDerivationPath.legacyDerivationPathSecretKey(chainAccountLocal.chainId)
            val ledgerDerivationPath = secretsStoreV2.getAdditionalMetaAccountSecret(ledgerAccountLocal.metaAccount.id, ledgerDerivationPathKey)

            CloudBackup.WalletPrivateInfo.ChainAccountSecrets(
                accountId = chainAccountLocal.accountId,
                entropy = null,
                seed = null,
                keypair = null,
                derivationPath = ledgerDerivationPath
            )
        }
    }

    private suspend fun prepareChainAccountPrivateInfo(metaAccount: Long, chainAccountId: AccountId): CloudBackup.WalletPrivateInfo.ChainAccountSecrets? {
        val secrets = secretsStoreV2.getChainAccountSecrets(metaAccount, chainAccountId) ?: return null

        return secrets.toBackupSecrets(chainAccountId)
    }

    private fun EncodableStruct<ChainAccountSecrets>.toBackupSecrets(chainAccountId: ByteArray): CloudBackup.WalletPrivateInfo.ChainAccountSecrets {
        return CloudBackup.WalletPrivateInfo.ChainAccountSecrets(
            accountId = chainAccountId,
            entropy = entropy,
            seed = seed,
            keypair = keypair.toBackupKeypairSecrets(),
            derivationPath = derivationPath
        )
    }

    private fun CloudBackup.WalletPrivateInfo.ChainAccountSecrets.toLocalSecrets(): EncodableStruct<ChainAccountSecrets>? {
        return ChainAccountSecrets(
            entropy = entropy,
            seed = seed,
            derivationPath = derivationPath,
            keyPair = keypair?.toLocalKeyPair() ?: return null
        )
    }

    private fun CloudBackup.WalletPrivateInfo.KeyPairSecrets.toLocalKeyPair(): Keypair {
        val nonce = nonce

        return if (nonce != null) {
            Sr25519Keypair(privateKey = privateKey, publicKey = publicKey, nonce = nonce)
        } else {
            BaseKeypair(privateKey = privateKey, publicKey = publicKey)
        }
    }

    private fun CloudBackup.WalletPrivateInfo.getLocalMetaAccountSecrets(): EncodableStruct<MetaAccountSecrets>? {
        return MetaAccountSecrets(
            entropy = entropy,
            substrateSeed = substrate?.seed,
            // Keypair is optional in backup since Ledger backup has base substrate derivation path but doesn't have keypair
            // MetaAccountSecrets, however, require substrateKeyPair to be non-null, so we return null here in case of null keypair
            // Which is a expected behavior in case of Ledger secrets
            substrateKeyPair = substrate?.keypair?.toLocalKeyPair() ?: return null,
            substrateDerivationPath = substrate?.derivationPath,
            ethereumKeypair = ethereum?.keypair?.toLocalKeyPair(),
            ethereumDerivationPath = ethereum?.derivationPath
        )
    }

    private fun EncodableStruct<MetaAccountSecrets>?.getEthereumBackupSecrets(): CloudBackup.WalletPrivateInfo.EthereumSecrets? {
        if (this == null) return null

        return CloudBackup.WalletPrivateInfo.EthereumSecrets(
            keypair = ethereumKeypair?.toBackupKeypairSecrets() ?: return null,
            derivationPath = ethereumDerivationPath
        )
    }

    private fun EncodableStruct<MetaAccountSecrets>?.getSubstrateBackupSecrets(): CloudBackup.WalletPrivateInfo.SubstrateSecrets? {
        if (this == null) return null

        return CloudBackup.WalletPrivateInfo.SubstrateSecrets(
            seed = seed,
            keypair = substrateKeypair.toBackupKeypairSecrets(),
            derivationPath = substrateDerivationPath
        )
    }

    private fun EncodableStruct<KeyPairSchema>.toBackupKeypairSecrets(): CloudBackup.WalletPrivateInfo.KeyPairSecrets {
        return CloudBackup.WalletPrivateInfo.KeyPairSecrets(
            publicKey = publicKey,
            privateKey = privateKey,
            nonce = nonce
        )
    }

    private suspend fun List<JoinedMetaAccountInfo>.toBackupPublicData(
        modifiedAt: Long = cloudBackupAccountsModificationsTracker.getAccountsLastModifiedAt()
    ): CloudBackup.PublicData {
        val chainsById = chainRegistry.chainsById()

        return CloudBackup.PublicData(
            modifiedAt = modifiedAt,
            wallets = mapNotNull { it.toBackupPublicInfo(chainsById) },
        )
    }

    private fun JoinedMetaAccountInfo.toBackupPublicInfo(
        chainsById: ChainsById,
    ): CloudBackup.WalletPublicInfo? {
        return CloudBackup.WalletPublicInfo(
            walletId = metaAccount.globallyUniqueId,
            substratePublicKey = metaAccount.substratePublicKey,
            substrateAccountId = metaAccount.substrateAccountId,
            substrateCryptoType = metaAccount.substrateCryptoType,
            ethereumAddress = metaAccount.ethereumAddress,
            ethereumPublicKey = metaAccount.ethereumPublicKey,
            name = metaAccount.name,
            type = metaAccount.type.toBackupWalletType() ?: return null,
            chainAccounts = chainAccounts.mapToSet { chainAccount -> chainAccount.toBackupPublicChainAccount(chainsById) }
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
            status = MetaAccountLocal.Status.ACTIVE,
            typeExtras = null
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
                cryptoType = it.cryptoType?.toCryptoType()
            )
        }
    }

    private fun ChainAccountLocal.toBackupPublicChainAccount(chainsById: ChainsById): CloudBackup.WalletPublicInfo.ChainAccountInfo {
        return CloudBackup.WalletPublicInfo.ChainAccountInfo(
            chainId = chainId,
            publicKey = publicKey,
            accountId = accountId,
            cryptoType = cryptoType?.toBackupChainAccountCryptoType(chainsById, chainId)
        )
    }

    private fun MetaAccountLocal.Type.toBackupWalletType(): CloudBackup.WalletPublicInfo.Type? {
        return when (this) {
            MetaAccountLocal.Type.SECRETS -> CloudBackup.WalletPublicInfo.Type.SECRETS
            MetaAccountLocal.Type.WATCH_ONLY -> CloudBackup.WalletPublicInfo.Type.WATCH_ONLY
            MetaAccountLocal.Type.PARITY_SIGNER -> CloudBackup.WalletPublicInfo.Type.PARITY_SIGNER
            MetaAccountLocal.Type.LEDGER -> CloudBackup.WalletPublicInfo.Type.LEDGER
            MetaAccountLocal.Type.LEDGER_GENERIC -> CloudBackup.WalletPublicInfo.Type.LEDGER_GENERIC
            MetaAccountLocal.Type.POLKADOT_VAULT -> CloudBackup.WalletPublicInfo.Type.POLKADOT_VAULT

            MetaAccountLocal.Type.PROXIED,
            MetaAccountLocal.Type.MULTISIG,
            MetaAccountLocal.Type.DERIVATIVE -> null
        }
    }

    private fun CloudBackup.WalletPublicInfo.Type.toLocalWalletType(): MetaAccountLocal.Type {
        return when (this) {
            CloudBackup.WalletPublicInfo.Type.SECRETS -> MetaAccountLocal.Type.SECRETS
            CloudBackup.WalletPublicInfo.Type.WATCH_ONLY -> MetaAccountLocal.Type.WATCH_ONLY
            CloudBackup.WalletPublicInfo.Type.PARITY_SIGNER -> MetaAccountLocal.Type.PARITY_SIGNER
            CloudBackup.WalletPublicInfo.Type.LEDGER -> MetaAccountLocal.Type.LEDGER
            CloudBackup.WalletPublicInfo.Type.LEDGER_GENERIC -> MetaAccountLocal.Type.LEDGER_GENERIC
            CloudBackup.WalletPublicInfo.Type.POLKADOT_VAULT -> MetaAccountLocal.Type.POLKADOT_VAULT
        }
    }

    private fun ChainAccountCryptoType.toCryptoType(): CryptoType {
        return when (this) {
            ChainAccountCryptoType.SR25519 -> CryptoType.SR25519
            ChainAccountCryptoType.ED25519 -> CryptoType.ED25519
            ChainAccountCryptoType.ECDSA, ChainAccountCryptoType.ETHEREUM -> CryptoType.ECDSA
        }
    }

    private fun CryptoType.toBackupChainAccountCryptoType(chainsById: ChainsById, chainId: ChainId): ChainAccountCryptoType? {
        val isEvm = chainsById.isEVM(chainId) ?: return null

        if (isEvm) return ChainAccountCryptoType.ETHEREUM

        return when (this) {
            CryptoType.SR25519 -> ChainAccountCryptoType.SR25519
            CryptoType.ED25519 -> ChainAccountCryptoType.ED25519
            CryptoType.ECDSA -> ChainAccountCryptoType.ECDSA
        }
    }

    private fun ChainsById.isEVM(chainId: ChainId): Boolean? {
        return get(chainId)?.isEthereumBased
    }
}
