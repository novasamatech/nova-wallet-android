package io.novafoundation.nova.feature_account_impl.presentation.cloudBackup.restoreBackup

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.base.showError
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.mixin.actionAwaitable.ConfirmationDialogInfo
import io.novafoundation.nova.common.mixin.actionAwaitable.confirmingAction
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.toggle
import io.novafoundation.nova.common.view.bottomSheet.action.ActionBottomSheetLauncher
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.domain.cloudBackup.enterPassword.RestoreCloudBackupInteractor
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_cloud_backup_api.presenter.action.launchBackupLostPasswordAction
import io.novafoundation.nova.feature_cloud_backup_api.presenter.action.launchCorruptedBackupFoundAction
import io.novafoundation.nova.feature_cloud_backup_api.presenter.confirmation.awaitDeleteBackupConfirmation
import io.novafoundation.nova.feature_cloud_backup_api.presenter.errorHandling.mapDeleteBackupFailureToUi
import io.novafoundation.nova.feature_cloud_backup_api.presenter.errorHandling.mapRestoreBackupFailureToUi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class RestoreCloudBackupViewModel(
    private val router: AccountRouter,
    private val resourceManager: ResourceManager,
    private val interactor: RestoreCloudBackupInteractor,
    private val actionBottomSheetLauncher: ActionBottomSheetLauncher,
    private val accountInteractor: AccountInteractor,
    private val actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
) : BaseViewModel(), ActionBottomSheetLauncher by actionBottomSheetLauncher {

    val confirmationAwaitableAction = actionAwaitableMixinFactory.confirmingAction<ConfirmationDialogInfo>()

    val passwordFlow = MutableStateFlow("")

    private val _showPassword = MutableStateFlow(false)
    val showPassword: Flow<Boolean> = _showPassword

    private val _restoreBackupInProgress = MutableStateFlow(false)
    private val restoreBackupInProgress: Flow<Boolean> = _restoreBackupInProgress

    val continueButtonState = combine(passwordFlow, restoreBackupInProgress) { password, backupInProgress ->
        when {
            backupInProgress -> DescriptiveButtonState.Loading
            password.isNotEmpty() -> DescriptiveButtonState.Enabled(resourceManager.getString(R.string.common_continue))
            else -> DescriptiveButtonState.Disabled(resourceManager.getString(R.string.cloud_backup_enter_password))
        }
    }.shareInBackground()

    fun backClicked() {
        router.back()
    }

    fun continueClicked() {
        launch {
            _restoreBackupInProgress.value = true
            val password = passwordFlow.value
            interactor.restoreCloudBackup(password)
                .onSuccess {
                    continueBasedOnCodeStatus()
                }.onFailure { throwable ->
                    // TODO Antony: Handle CannotApplyNonDestructiveDiff
                    val titleAndMessage = mapRestoreBackupFailureToUi(
                        resourceManager,
                        throwable,
                        ::corruptedBackupFound
                    )
                    titleAndMessage?.let { showError(it) }
                }

            _restoreBackupInProgress.value = false
        }
    }

    private fun corruptedBackupFound() {
        actionBottomSheetLauncher.launchCorruptedBackupFoundAction(resourceManager, ::confirmCloudBackupDelete)
    }

    fun toggleShowPassword() {
        _showPassword.toggle()
    }

    fun forgotPasswordClicked() {
        actionBottomSheetLauncher.launchBackupLostPasswordAction(resourceManager, ::confirmCloudBackupDelete)
    }

    private suspend fun continueBasedOnCodeStatus() {
        if (accountInteractor.isCodeSet()) {
            router.openMain()
        } else {
            router.openCreatePincode()
        }
    }

    private fun confirmCloudBackupDelete() {
        launch {
            confirmationAwaitableAction.awaitDeleteBackupConfirmation()

            interactor.deleteCloudBackup()
                .onSuccess { router.back() }
                .onFailure { throwable ->
                    val titleAndMessage = mapDeleteBackupFailureToUi(resourceManager, throwable)
                    titleAndMessage?.let { showError(it) }
                }
        }
    }
}
