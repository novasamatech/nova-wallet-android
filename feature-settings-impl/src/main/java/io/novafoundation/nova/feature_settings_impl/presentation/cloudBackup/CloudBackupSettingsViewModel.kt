package io.novafoundation.nova.feature_settings_impl.presentation.cloudBackup

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.base.showError
import io.novafoundation.nova.common.navigation.awaitResponse
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.presenatation.cloudBackup.createPassword.SyncWalletsBackupPasswordCommunicator
import io.novafoundation.nova.feature_account_api.presenatation.cloudBackup.createPassword.SyncWalletsBackupPasswordRequester
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
    private val errorState = MutableStateFlow<Throwable?>(null)

    val lastSync = cloudBackupSettingsInteractor.observeLastSyncedTime()
    val cloudBackupEnabled = MutableStateFlow(false)

    val cloudBackupStateModel: Flow<CloudBackupStateModel> = combine(
        cloudBackupEnabled,
        isSyncing,
        errorState,
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
                isSyncing.value = true
                cloudBackupEnabled.value = true
                syncBackupInternal(
                    onBackupNotFound = {
                        syncWalletsBackupPasswordCommunicator.awaitResponse(SyncWalletsBackupPasswordRequester.EmptyRequest)
                        cloudBackupEnabled.value = cloudBackupSettingsInteractor.isSyncCloudBackupEnabled()
                        errorState.value = null
                    },
                    onPasswordDeprecated = { cloudBackupSettingsInteractor.setCloudBackupSyncEnabled(true) }
                )
                    .onSuccess {
                        cloudBackupSettingsInteractor.setCloudBackupSyncEnabled(true)
                    }
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
                isSyncing.value = true
                syncBackupInternal(onBackupNotFound = { /* run create backup using existing password, make throwable state empty */ })
                isSyncing.value = false
            }
        }
    }

    private suspend fun syncBackupInternal(
        onBackupNotFound: (suspend () -> Unit)? = null,
        onPasswordDeprecated: (suspend () -> Unit)? = null
    ): Result<Unit> {
        isSyncing.value = true

        val result = cloudBackupSettingsInteractor.syncCloudBackup()
            .onSuccess { errorState.value = null }
            .onFailure { throwable ->
                errorState.value = throwable

                val titleAndMessage = mapCloudBackupSyncFailed(resourceManager, throwable, onBackupNotFound, onPasswordDeprecated)
                titleAndMessage?.let { showError(it) }
            }

        isSyncing.value = false
        return result
    }
}
