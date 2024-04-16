package io.novafoundation.nova.feature_settings_impl.presentation.cloudBackup

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.base.showError
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.mixin.actionAwaitable.ConfirmationDialogInfo
import io.novafoundation.nova.common.mixin.actionAwaitable.confirmingAction
import io.novafoundation.nova.common.navigation.awaitResponse
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.view.bottomSheet.action.ActionBottomSheetLauncher
import io.novafoundation.nova.common.view.input.selector.ListSelectorMixin
import io.novafoundation.nova.feature_account_api.presenatation.cloudBackup.createPassword.SyncWalletsBackupPasswordCommunicator
import io.novafoundation.nova.feature_account_api.presenatation.cloudBackup.createPassword.SyncWalletsBackupPasswordRequester
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors.CloudBackupAuthFailed
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors.CloudBackupNotFound
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors.CloudBackupWrongPassword
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors.CorruptedBackupError
import io.novafoundation.nova.feature_cloud_backup_api.presenter.action.launchDeleteBackupAction
import io.novafoundation.nova.feature_cloud_backup_api.presenter.confirmation.awaitDeleteBackupConfirmation
import io.novafoundation.nova.feature_cloud_backup_api.presenter.errorHandling.mapCloudBackupSyncFailed
import io.novafoundation.nova.feature_cloud_backup_api.presenter.errorHandling.mapDeleteBackupFailureToUi
import io.novafoundation.nova.feature_settings_impl.R
import io.novafoundation.nova.feature_settings_impl.SettingsRouter
import io.novafoundation.nova.feature_settings_impl.domain.CloudBackupSettingsInteractor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class BackupSettingsViewModel(
    private val resourceManager: ResourceManager,
    private val router: SettingsRouter,
    private val cloudBackupSettingsInteractor: CloudBackupSettingsInteractor,
    private val syncWalletsBackupPasswordCommunicator: SyncWalletsBackupPasswordCommunicator,
    private val actionBottomSheetLauncher: ActionBottomSheetLauncher,
    actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
    listSelectorMixinFactory: ListSelectorMixin.Factory
) : BaseViewModel(), ActionBottomSheetLauncher by actionBottomSheetLauncher {

    val confirmationAwaitableAction = actionAwaitableMixinFactory.confirmingAction<ConfirmationDialogInfo>()

    val listSelectorMixin = listSelectorMixinFactory.create(viewModelScope)

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
        val reason = errorState.value

        when (reason) {
            is CloudBackupAuthFailed -> return

            is CloudBackupWrongPassword,
            is CorruptedBackupError -> {
                listSelectorMixin.showSelector(
                    R.string.manage_cloud_backup,
                    listOf(manageBackupDeleteBackupItem())
                )
            }

            else -> {
                listSelectorMixin.showSelector(
                    R.string.manage_cloud_backup,
                    listOf(manageBackupChangePasswordItem(), manageBackupDeleteBackupItem())
                )
            }
        }
    }

    fun problemButtonClicked() {
        TODO()
    }

    private fun initSyncCloudBackupState() {
        launch {
            if (cloudBackupSettingsInteractor.isSyncCloudBackupEnabled()) {
                syncBackupInternal(onBackupNotFound = {
                    /* TODO: run create backup using existing password */
                })
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

                when (throwable) {
                    is CloudBackupNotFound -> onBackupNotFound?.invoke()
                    is CloudBackupWrongPassword -> onPasswordDeprecated?.invoke()
                }

                val titleAndMessage = mapCloudBackupSyncFailed(resourceManager, throwable)
                titleAndMessage?.let { showError(it) }
            }

        isSyncing.value = false
        return result
    }

    private fun manageBackupChangePasswordItem(): ListSelectorMixin.Item {
        return ListSelectorMixin.Item(
            R.drawable.ic_pin,
            R.color.icon_primary,
            R.string.common_change_password,
            R.color.text_primary,
            ::onChangePasswordClicked
        )
    }

    private fun manageBackupDeleteBackupItem(): ListSelectorMixin.Item {
        return ListSelectorMixin.Item(
            R.drawable.ic_delete,
            R.color.icon_negative,
            R.string.backup_settings_delete_backup,
            R.color.text_negative,
            ::onDeleteBackupClicked
        )
    }

    private fun onChangePasswordClicked() {
        TODO()
    }

    private fun onDeleteBackupClicked() {
        actionBottomSheetLauncher.launchDeleteBackupAction(resourceManager, ::confirmCloudBackupDelete)
    }

    private fun confirmCloudBackupDelete() {
        launch {
            confirmationAwaitableAction.awaitDeleteBackupConfirmation()

            cloudBackupSettingsInteractor.deleteCloudBackup()
                .onSuccess {
                    cloudBackupSettingsInteractor.setCloudBackupSyncEnabled(false)
                    cloudBackupEnabled.value = false
                }
                .onFailure { throwable ->
                    val titleAndMessage = mapDeleteBackupFailureToUi(resourceManager, throwable)
                    titleAndMessage?.let { showError(it) }
                }
        }
    }
}
