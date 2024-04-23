package io.novafoundation.nova.feature_settings_impl.presentation.cloudBackup.settings

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.base.showError
import io.novafoundation.nova.common.list.toListWithHeaders
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.mixin.actionAwaitable.ConfirmationDialogInfo
import io.novafoundation.nova.common.mixin.actionAwaitable.confirmingAction
import io.novafoundation.nova.common.mixin.api.CustomDialogDisplayer
import io.novafoundation.nova.common.mixin.api.displayDialogOrNothing
import io.novafoundation.nova.common.navigation.awaitResponse
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.event
import io.novafoundation.nova.common.utils.flatMap
import io.novafoundation.nova.common.utils.progress.ProgressDialogMixin
import io.novafoundation.nova.common.utils.progress.startProgress
import io.novafoundation.nova.common.view.bottomSheet.action.ActionBottomSheetLauncher
import io.novafoundation.nova.common.view.input.selector.ListSelectorMixin
import io.novafoundation.nova.feature_account_api.presenatation.account.common.listing.MetaAccountTypePresentationMapper
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.cloudBackup.changePassword.ChangeBackupPasswordCommunicator
import io.novafoundation.nova.feature_account_api.presenatation.cloudBackup.changePassword.ChangeBackupPasswordRequester
import io.novafoundation.nova.feature_account_api.presenatation.cloudBackup.changePassword.RestoreBackupPasswordCommunicator
import io.novafoundation.nova.feature_account_api.presenatation.cloudBackup.changePassword.RestoreBackupPasswordRequester
import io.novafoundation.nova.feature_account_api.presenatation.cloudBackup.createPassword.SyncWalletsBackupPasswordCommunicator
import io.novafoundation.nova.feature_account_api.presenatation.cloudBackup.createPassword.SyncWalletsBackupPasswordRequester
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.CloudBackup
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.diff.CloudBackupDiff
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors.CannotApplyNonDestructiveDiff
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors.CloudBackupNotFound
import io.novafoundation.nova.feature_cloud_backup_api.presenter.action.launchDeleteBackupAction
import io.novafoundation.nova.feature_cloud_backup_api.presenter.confirmation.awaitDeleteBackupConfirmation
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors.FetchBackupError
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors.InvalidBackupPasswordError
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors.PasswordNotSaved
import io.novafoundation.nova.feature_cloud_backup_api.presenter.action.launchCloudBackupChangesAction
import io.novafoundation.nova.feature_cloud_backup_api.presenter.action.launchCorruptedBackupFoundAction
import io.novafoundation.nova.feature_cloud_backup_api.presenter.action.launchDeprecatedPasswordAction
import io.novafoundation.nova.feature_cloud_backup_api.presenter.confirmation.awaitBackupDestructiveChangesConfirmation
import io.novafoundation.nova.feature_cloud_backup_api.presenter.errorHandling.handlers.showCloudBackupUnknownError
import io.novafoundation.nova.feature_cloud_backup_api.presenter.errorHandling.mapCloudBackupSyncFailed
import io.novafoundation.nova.feature_cloud_backup_api.presenter.errorHandling.mapDeleteBackupFailureToUi
import io.novafoundation.nova.feature_cloud_backup_api.presenter.errorHandling.mapWriteBackupFailureToUi
import io.novafoundation.nova.feature_settings_impl.R
import io.novafoundation.nova.feature_settings_impl.SettingsRouter
import io.novafoundation.nova.feature_settings_impl.domain.CloudBackupSettingsInteractor
import io.novafoundation.nova.feature_settings_impl.domain.model.CloudBackupChangedAccount
import io.novafoundation.nova.feature_settings_impl.presentation.cloudBackup.backupDiff.CloudBackupDiffBottomSheet
import io.novafoundation.nova.feature_settings_impl.presentation.cloudBackup.backupDiff.adapter.CloudBackupDiffGroupRVItem
import io.novafoundation.nova.feature_settings_impl.presentation.cloudBackup.backupDiff.adapter.AccountDiffRVItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class BackupSettingsViewModel(
    private val resourceManager: ResourceManager,
    private val router: SettingsRouter,
    private val cloudBackupSettingsInteractor: CloudBackupSettingsInteractor,
    private val syncWalletsBackupPasswordCommunicator: SyncWalletsBackupPasswordCommunicator,
    private val changeBackupPasswordCommunicator: ChangeBackupPasswordCommunicator,
    private val restoreBackupPasswordCommunicator: RestoreBackupPasswordCommunicator,
    private val actionBottomSheetLauncher: ActionBottomSheetLauncher,
    private val accountTypePresentationMapper: MetaAccountTypePresentationMapper,
    private val addressIconGenerator: AddressIconGenerator,
    private val walletUiUseCase: WalletUiUseCase,
    val progressDialogMixin: ProgressDialogMixin,
    actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
    listSelectorMixinFactory: ListSelectorMixin.Factory,
    customDialogProvider: CustomDialogDisplayer.Presentation
) : BaseViewModel(),
    ActionBottomSheetLauncher by actionBottomSheetLauncher,
    CustomDialogDisplayer.Presentation by customDialogProvider {

    val negativeConfirmationAwaitableAction = actionAwaitableMixinFactory.confirmingAction<ConfirmationDialogInfo>()

    val neutralConfirmationAwaitableAction = actionAwaitableMixinFactory.confirmingAction<ConfirmationDialogInfo>()

    val listSelectorMixin = listSelectorMixinFactory.create(viewModelScope)

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

    private val _cloudBackupChangesLiveData = MutableLiveData<Event<CloudBackupDiffBottomSheet.Payload>>()
    val cloudBackupChangesLiveData = _cloudBackupChangesLiveData

    init {
        syncCloudBackupState()
        observeRequesterResults()

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

                syncCloudBackupOnSwitcher()
            }
        }
    }

    fun manualBackupClicked() {
        TODO()
    }

    fun cloudBackupManageClicked() {
        if (isSyncing.value) return

        when (syncedState.value) {
            BackupSyncOutcome.StorageAuthFailed -> return

            BackupSyncOutcome.UnknownPassword,
            BackupSyncOutcome.CorruptedBackup,
            BackupSyncOutcome.OtherStorageIssue -> {
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
        when (val value = syncedState.value) {
            BackupSyncOutcome.UnknownPassword -> showPasswordDeprecatedActionDialog()

            BackupSyncOutcome.CorruptedBackup -> showCorruptedBackupActionDialog()
            is BackupSyncOutcome.DestructiveDiff -> openCloudBackupDiffScreen(value.cloudBackupDiff)
            BackupSyncOutcome.StorageAuthFailed -> initSignInToCloud()

            BackupSyncOutcome.OtherStorageIssue,
            BackupSyncOutcome.UnknownError -> showCloudBackupUnknownError(resourceManager)

            BackupSyncOutcome.Ok -> {}
        }
    }

    fun applyBackupDestructiveChanges() {
        launch {
            neutralConfirmationAwaitableAction.awaitBackupDestructiveChangesConfirmation()

            cloudBackupSettingsInteractor.applyBackupAccountDiff()
                .onSuccess { syncCloudBackupState() }
                .onFailure { showError(mapWriteBackupFailureToUi(resourceManager, it)) }
        }
    }

    private fun openDestructiveDiffAction(diff: CloudBackupDiff) {
        actionBottomSheetLauncher.launchCloudBackupChangesAction(resourceManager) {
            openCloudBackupDiffScreen(diff)
        }
    }

    private fun openCloudBackupDiffScreen(diff: CloudBackupDiff) {
        launch {
            val sortedDiff = cloudBackupSettingsInteractor.prepareSortedLocalChangesFromDiff(diff)
            val cloudBackupChangesList = sortedDiff.toListWithHeaders(
                keyMapper = { type, _ -> accountTypePresentationMapper.mapTypeToChipLabel(type)?.let { CloudBackupDiffGroupRVItem(it) } },
                valueMapper = { mapMetaAccountDiffToUi(it) }
            )

            _cloudBackupChangesLiveData.value = CloudBackupDiffBottomSheet.Payload(cloudBackupChangesList).event()
        }
    }

    private fun Throwable.toEnableBackupSyncState(): BackupSyncOutcome {
        return when (this) {
            is PasswordNotSaved, is InvalidBackupPasswordError -> BackupSyncOutcome.UnknownPassword
            // not found backup is ok when we enable backup and when we start initial sync since we will create a new backup
            is CannotApplyNonDestructiveDiff -> BackupSyncOutcome.DestructiveDiff(cloudBackupDiff)
            is FetchBackupError.BackupNotFound -> BackupSyncOutcome.Ok
            is FetchBackupError.CorruptedBackup -> BackupSyncOutcome.CorruptedBackup
            is FetchBackupError.Other -> BackupSyncOutcome.UnknownError
            is FetchBackupError.AuthFailed -> BackupSyncOutcome.StorageAuthFailed
            else -> BackupSyncOutcome.UnknownError
        }
    }

    private fun syncCloudBackupState() = launch {
        if (cloudBackupSettingsInteractor.isSyncCloudBackupEnabled()) {
            runSyncWithProgress { result ->
                result.onFailure { throwable ->
                    if (throwable is CloudBackupNotFound) {
                        writeBackupToCloudAndSync()
                    }
                }
            }
        }
    }

    private suspend fun writeBackupToCloudAndSync() {
        cloudBackupSettingsInteractor.writeLocalBackupToCloud()
            .flatMap {
                cloudBackupSettingsInteractor.syncCloudBackup()
            }.handleSyncBackupResult()
    }

    private suspend fun syncCloudBackupOnSwitcher() = runSyncWithProgress { result ->
        result.onSuccess { cloudBackupSettingsInteractor.setCloudBackupSyncEnabled(true) }
            .onFailure { throwable ->

                when (throwable) {
                    is CloudBackupNotFound -> {
                        syncWalletsBackupPasswordCommunicator.awaitResponse(SyncWalletsBackupPasswordRequester.EmptyRequest)
                        // cloudBackupSyncEnabled is set by syncWalletsBackup flow
                        cloudBackupEnabled.value = cloudBackupSettingsInteractor.isSyncCloudBackupEnabled()
                        syncedState.value = BackupSyncOutcome.Ok
                    }

                    else -> {
                        cloudBackupSettingsInteractor.setCloudBackupSyncEnabled(true)
                    }
                }
            }
    }

    private suspend inline fun runSyncWithProgress(action: (Result<Unit>) -> Unit) {
        isSyncing.value = true

        val result = cloudBackupSettingsInteractor.syncCloudBackup()
            .handleSyncBackupResult()

        action(result)

        isSyncing.value = false
    }

    private suspend inline fun runActionAndSync(action: (Result<Unit>) -> Unit) {
        isSyncing.value = true

        val result = cloudBackupSettingsInteractor.syncCloudBackup()
            .handleSyncBackupResult()

        action(result)

        isSyncing.value = false
    }

    private fun Result<Unit>.handleSyncBackupResult(): Result<Unit> {
        return onSuccess { syncedState.value = BackupSyncOutcome.Ok; }
            .onFailure { throwable ->
                syncedState.value = throwable.toEnableBackupSyncState()
                handleBackupError(throwable)
            }
    }

    private fun handleBackupError(throwable: Throwable) {
        val payload = mapCloudBackupSyncFailed(
            resourceManager,
            throwable,
            onDestructiveBackupFound = ::openDestructiveDiffAction,
            onPasswordDeprecated = ::showPasswordDeprecatedActionDialog,
            onCorruptedBackup = ::showCorruptedBackupActionDialog,
            initSignIn = ::initSignInToCloud
        )

        displayDialogOrNothing(payload)
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
        changeBackupPasswordCommunicator.openRequest(ChangeBackupPasswordRequester.EmptyRequest)
    }

    private fun onDeleteBackupClicked() {
        actionBottomSheetLauncher.launchDeleteBackupAction(resourceManager, ::confirmCloudBackupDelete)
    }

    private fun observeRequesterResults() {
        changeBackupPasswordCommunicator.responseFlow.syncBackupOnEach()
        restoreBackupPasswordCommunicator.responseFlow.syncBackupOnEach()
        syncWalletsBackupPasswordCommunicator.responseFlow.syncBackupOnEach()
    }

    private fun Flow<Any>.syncBackupOnEach() {
        this.onEach {
            syncCloudBackupState()
        }
            .launchIn(this@BackupSettingsViewModel)
    }

    private fun showPasswordDeprecatedActionDialog() {
        actionBottomSheetLauncher.launchDeprecatedPasswordAction(resourceManager, ::openRestorePassword)
    }

    private fun showCorruptedBackupActionDialog() {
        actionBottomSheetLauncher.launchCorruptedBackupFoundAction(resourceManager, ::confirmCloudBackupDelete)
    }

    private fun openRestorePassword() {
        restoreBackupPasswordCommunicator.openRequest(RestoreBackupPasswordRequester.EmptyRequest)
    }

    private fun initSignInToCloud() {
        launch {
            cloudBackupSettingsInteractor.signInToCloud()
                .onSuccess { syncCloudBackupState() }
        }
    }

    private fun confirmCloudBackupDelete() {
        launch {
            negativeConfirmationAwaitableAction.awaitDeleteBackupConfirmation()

            progressDialogMixin.startProgress(R.string.deleting_backup_progress) {
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

    private suspend fun mapMetaAccountDiffToUi(changedAccount: CloudBackupChangedAccount): AccountDiffRVItem {
        return with(changedAccount) {
            val (stateText, stateColorRes, stateIconRes) = mapChangingTypeToUi(changingType)
            val walletSeed = walletUiUseCase.walletSeed(
                account.substrateAccountId,
                account.ethereumAddress,
                account.chainAccounts.map(CloudBackup.WalletPublicInfo.ChainAccountInfo::accountId)
            )

            AccountDiffRVItem(
                id = account.walletId,
                icon = addressIconGenerator.createAddressIcon(walletSeed, 32),
                title = account.name,
                state = stateText,
                stateColorRes = stateColorRes,
                stateIconRes = stateIconRes
            )
        }
    }

    private fun mapChangingTypeToUi(type: CloudBackupChangedAccount.ChangingType): Triple<String, Int, Int?> {
        return when (type) {
            CloudBackupChangedAccount.ChangingType.ADDED -> Triple(resourceManager.getString(R.string.state_new), R.color.text_secondary, null)
            CloudBackupChangedAccount.ChangingType.REMOVED -> Triple(
                resourceManager.getString(R.string.state_removed),
                R.color.text_negative,
                R.drawable.ic_red_cross
            )

            CloudBackupChangedAccount.ChangingType.CHANGED -> Triple(
                resourceManager.getString(R.string.state_changed),
                R.color.text_warning,
                R.drawable.ic_warning_filled
            )
        }
    }
}
