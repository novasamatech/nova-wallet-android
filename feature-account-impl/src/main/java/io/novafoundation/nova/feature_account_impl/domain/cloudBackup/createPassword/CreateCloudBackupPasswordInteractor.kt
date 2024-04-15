package io.novafoundation.nova.feature_account_impl.domain.cloudBackup.createPassword

import io.novafoundation.nova.core_db.dao.MetaAccountDao
import io.novafoundation.nova.feature_account_api.domain.account.common.EncryptionDefaults
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.data.cloudBackup.LocalAccountsCloudBackupFacade
import io.novafoundation.nova.feature_account_api.data.cloudBackup.applyNonDestructiveCloudVersionOrThrow
import io.novafoundation.nova.feature_account_impl.data.repository.datasource.SecretsMetaAccountLocalFactory
import io.novafoundation.nova.feature_account_impl.data.secrets.AccountSecretsFactory
import io.novafoundation.nova.feature_account_impl.domain.cloudBackup.createPassword.model.PasswordErrors
import io.novafoundation.nova.feature_cloud_backup_api.domain.CloudBackupService
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.WriteBackupRequest
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.diff.strategy.BackupDiffStrategy
import java.util.*

private const val MIN_PASSWORD_SYMBOLS = 8

interface CreateCloudBackupPasswordInteractor {

    fun checkPasswords(password: String, confirmPassword: String): List<PasswordErrors>

    suspend fun createAndBackupAccount(accountName: String, password: String): Result<Unit>

    suspend fun syncWalletsBackup(password: String): Result<Unit>
}

class RealCreateCloudBackupPasswordInteractor(
    private val cloudBackupService: CloudBackupService,
    private val accountRepository: AccountRepository,
    private val encryptionDefaults: EncryptionDefaults,
    private val accountSecretsFactory: AccountSecretsFactory,
    private val secretsMetaAccountLocalFactory: SecretsMetaAccountLocalFactory,
    private val metaAccountDao: MetaAccountDao,
    private val localAccountsCloudBackupFacade: LocalAccountsCloudBackupFacade,
) : CreateCloudBackupPasswordInteractor {

    override fun checkPasswords(password: String, confirmPassword: String): List<PasswordErrors> {
        return buildList {
            if (password.length < MIN_PASSWORD_SYMBOLS) add(PasswordErrors.TOO_SHORT)
            if (!password.any { it.isLetter() }) add(PasswordErrors.NO_LETTERS)
            if (!password.any { it.isDigit() }) add(PasswordErrors.NO_DIGITS)
            if (password != confirmPassword || password.isEmpty()) add(PasswordErrors.PASSWORDS_DO_NOT_MATCH)
        }
    }

    override suspend fun createAndBackupAccount(accountName: String, password: String): Result<Unit> {
        val (secrets, substrateCryptoType) = accountSecretsFactory.metaAccountSecrets(
            substrateDerivationPath = encryptionDefaults.substrateDerivationPath,
            ethereumDerivationPath = encryptionDefaults.ethereumDerivationPath,
            accountSource = AccountSecretsFactory.AccountSource.Mnemonic(
                cryptoType = encryptionDefaults.substrateCryptoType,
                mnemonic = accountRepository.generateMnemonic().words
            )
        )

        val metaAccountLocal = secretsMetaAccountLocalFactory.create(
            name = accountName,
            substrateCryptoType = substrateCryptoType,
            secrets = secrets,
            accountSortPosition = metaAccountDao.nextAccountPosition()
        )

        val cloudBackup = localAccountsCloudBackupFacade.createCloudBackupFromInput(
            modificationTime = System.currentTimeMillis(),
            metaAccount = metaAccountLocal,
            chainAccounts = emptyList(),
            baseSecrets = secrets,
            chainAccountSecrets = emptyMap(),
            additionalSecrets = emptyMap()
        )

        return cloudBackupService.writeBackupToCloud(WriteBackupRequest(cloudBackup, password)).mapCatching {
            localAccountsCloudBackupFacade.applyNonDestructiveCloudVersionOrThrow(cloudBackup, BackupDiffStrategy.overwriteLocal())
            cloudBackupService.setLastSyncedTime(Date())

            val firstSelectedMetaAccount = accountRepository.getActiveMetaAccounts().first()
            accountRepository.selectMetaAccount(firstSelectedMetaAccount.id)
        }
    }

    override suspend fun syncWalletsBackup(password: String): Result<Unit> {
        val cloudBackup = localAccountsCloudBackupFacade.fullBackupInfoFromLocalSnapshot()
        return cloudBackupService.writeBackupToCloud(WriteBackupRequest(cloudBackup, password)).mapCatching {
            localAccountsCloudBackupFacade.applyNonDestructiveCloudVersionOrThrow(cloudBackup, BackupDiffStrategy.overwriteLocal())
            cloudBackupService.setLastSyncedTime(Date())
        }
    }
}
