package io.novafoundation.nova.feature_settings_impl.presentation.cloudBackup

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.base.showError
import io.novafoundation.nova.common.navigation.awaitResponse
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.presenatation.cloudBackup.createPassword.SyncWalletsBackupPasswordCommunicator
import io.novafoundation.nova.feature_account_api.presenatation.cloudBackup.createPassword.SyncWalletsBackupPasswordRequester
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors.CloudBackupAuthFailed
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors.CloudBackupNotFound
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors.CloudBackupWrongPassword
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors.FetchBackupError
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors.InvalidBackupPasswordError
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors.PasswordNotSaved
import io.novafoundation.nova.feature_cloud_backup_api.presenter.errorHandling.mapCloudBackupSyncFailed
import io.novafoundation.nova.feature_settings_impl.SettingsRouter
import io.novafoundation.nova.feature_settings_impl.domain.CloudBackupSettingsInteractor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class CloudBackupSettingsViewModel(
    private val resourceManager: ResourceManager,
    private val router: SettingsRouter,
    private val cloudBackupSettingsInteractor: CloudBackupSettingsInteractor,
    private val syncWalletsBackupPasswordCommunicator: SyncWalletsBackupPasswordCommunicator
) : BaseViewModel() {

    private val isSyncing = MutableStateFlow(false)

    private val syncedState = MutableStateFlow<BackupSyncOutcome>(BackupSyncOutcome.Ok)

    private val lastSync = cloudBackupSettingsInteractor.observeLastSyncedTime()

    val cloudBackupEnabled = MutableStateFlow(false)

    val cloudBackupStateModel: Flow<CloudBackupStateModel> = combine(
        cloudBackupEnabled,
        isSyncing,
        syncedState,
        lastSync
    ) { backupEnabled, syncingInProgress, state, lastSync ->
        mapCloudBackupStateModel(resourceManager, backupEnabled, syncingInProgress, state, lastSync)
    }

    init {
        initSyncCloudBackupState()

        launch {
            cloudBackupEnabled.value = cloudBackupSettingsInteractor.isSyncCloudBackupEnabled()
        }
    }

    fun backClicked() {
        router.back()
    }

    fun backupSwitcherClicked() {
        launch {
            if (cloudBackupSettingsInteractor.isSyncCloudBackupEnabled()) {
                cloudBackupEnabled.value = false
                cloudBackupSettingsInteractor.setCloudBackupSyncEnabled(false)
            } else {
                cloudBackupEnabled.value = true

                syncBackupInternal(
                    onSuccess = {
                        cloudBackupSettingsInteractor.setCloudBackupSyncEnabled(true)
                    },
                    onBackupNotFound = {
                        syncWalletsBackupPasswordCommunicator.awaitResponse(SyncWalletsBackupPasswordRequester.EmptyRequest)
                        // cloudBackupSyncEnabled is set by syncWalletsBackup flow
                        cloudBackupEnabled.value = cloudBackupSettingsInteractor.isSyncCloudBackupEnabled()
                        syncedState.value = BackupSyncOutcome.Ok
                    },
                    onUnknownPassword = {
                        cloudBackupSettingsInteractor.setCloudBackupSyncEnabled(true)
                    },
                    onOtherError = {
                        cloudBackupSettingsInteractor.setCloudBackupSyncEnabled(true)
                    }
                )
            }
        }
    }

    fun manualBackupClicked() {
        TODO()
    }

    fun cloudBackupManageClicked() {
        TODO()
    }

    fun problemButtonClicked() {
        TODO()
    }

    private fun initSyncCloudBackupState() {
        launch {
            if (cloudBackupSettingsInteractor.isSyncCloudBackupEnabled()) {
                syncBackupInternal(
                    onSuccess = {},
                    onBackupNotFound = {/* TODO: run create backup using existing password */ },
                    onUnknownPassword = {},
                    onOtherError = {}
                )
            }
        }
    }

    private fun Throwable.toEnableBackupSyncState(): BackupSyncOutcome {
        return when(this) {
            is PasswordNotSaved, is InvalidBackupPasswordError -> BackupSyncOutcome.UnknownPassword
            is FetchBackupError.BackupNotFound -> BackupSyncOutcome.Ok // not found backup is ok when we enable backup
            is FetchBackupError.CorruptedBackup -> BackupSyncOutcome.CorruptedBackup
            is FetchBackupError.Other -> BackupSyncOutcome.UnknownError
            is CloudBackupAuthFailed -> BackupSyncOutcome.StorageAuthFailed
            else ->  BackupSyncOutcome.UnknownError
        }
    }

    private suspend fun syncBackupInternal(
        onSuccess: suspend () -> Unit,
        onBackupNotFound: suspend () -> Unit,
        onUnknownPassword: suspend () -> Unit,
        onOtherError: suspend () -> Unit,
    ) {
        isSyncing.value = true

        cloudBackupSettingsInteractor.syncCloudBackup()
            .onSuccess { syncedState.value = BackupSyncOutcome.Ok; onSuccess() }
            .onFailure { throwable ->
                syncedState.value = throwable.toEnableBackupSyncState()

                // TODO Antony: handle `PasswordNotSaved`
                when (throwable) {
                    is CloudBackupNotFound -> onBackupNotFound.invoke()
                    is CloudBackupWrongPassword, is PasswordNotSaved -> onUnknownPassword.invoke()
                    else -> onOtherError.invoke()
                }

                val titleAndMessage = mapCloudBackupSyncFailed(resourceManager, throwable)
                titleAndMessage?.let { showError(it) }
            }

        isSyncing.value = false
    }
}

sealed class BackupSyncOutcome {

    object Ok: BackupSyncOutcome()

    object UnknownPassword : BackupSyncOutcome()

    object DestructiveDiff : BackupSyncOutcome()

    object StorageAuthFailed  : BackupSyncOutcome()

    object OtherStorageIssue:  BackupSyncOutcome()

    object CorruptedBackup : BackupSyncOutcome()

    object UnknownError : BackupSyncOutcome()
}


fun BackupSyncOutcome.isError(): Boolean {
    return this != BackupSyncOutcome.Ok
}
