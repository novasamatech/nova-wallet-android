package io.novafoundation.nova.feature_account_impl.presentation.cloudBackup.enterPassword.base

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.base.showError
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.mixin.actionAwaitable.ConfirmationDialogInfo
import io.novafoundation.nova.common.mixin.actionAwaitable.confirmingAction
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.toggle
import io.novafoundation.nova.common.view.bottomSheet.action.ActionBottomSheetLauncher
import io.novafoundation.nova.common.view.bottomSheet.action.ActionBottomSheetLauncherFactory
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.domain.cloudBackup.enterPassword.EnterCloudBackupInteractor
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_cloud_backup_api.presenter.action.launchBackupLostPasswordAction
import io.novafoundation.nova.feature_cloud_backup_api.presenter.confirmation.awaitDeleteBackupConfirmation
import io.novafoundation.nova.feature_cloud_backup_api.presenter.errorHandling.mapDeleteBackupFailureToUi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

abstract class EnterCloudBackupPasswordViewModel(
    internal val router: AccountRouter,
    internal val resourceManager: ResourceManager,
    internal val interactor: EnterCloudBackupInteractor,
    internal val actionBottomSheetLauncherFactory: ActionBottomSheetLauncherFactory,
    private val actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
) : BaseViewModel(), ActionBottomSheetLauncher by actionBottomSheetLauncherFactory.create() {

    val confirmationAwaitableAction = actionAwaitableMixinFactory.confirmingAction<ConfirmationDialogInfo>()

    val passwordFlow = MutableStateFlow("")

    val _showPassword = MutableStateFlow(false)
    val showPassword: Flow<Boolean> = _showPassword

    private val _restoreBackupInProgress = MutableStateFlow(false)
    private val restoreBackupInProgress: Flow<Boolean> = _restoreBackupInProgress

    val continueButtonState = combine(passwordFlow, restoreBackupInProgress) { password, backupInProgress ->
        when {
            backupInProgress -> DescriptiveButtonState.Loading
            password.isNotEmpty() -> DescriptiveButtonState.Enabled(resourceManager.getString(R.string.common_continue))
            else -> DescriptiveButtonState.Disabled(resourceManager.getString(R.string.common_enter_password))
        }
    }.shareInBackground()

    fun backClicked() {
        router.back()
    }

    fun continueClicked() {
        launch {
            _restoreBackupInProgress.value = true
            val password = passwordFlow.value
            continueInternal(password)

            _restoreBackupInProgress.value = false
        }
    }

    abstract suspend fun continueInternal(password: String)

    fun toggleShowPassword() {
        _showPassword.toggle()
    }

    fun forgotPasswordClicked() {
        launchBackupLostPasswordAction(resourceManager, ::confirmCloudBackupDelete)
    }

    internal fun confirmCloudBackupDelete() {
        launch {
            confirmationAwaitableAction.awaitDeleteBackupConfirmation(resourceManager)

            interactor.deleteCloudBackup()
                .onSuccess { router.back() }
                .onFailure { throwable ->
                    val titleAndMessage = mapDeleteBackupFailureToUi(resourceManager, throwable)
                    titleAndMessage?.let { showError(it) }
                }
        }
    }
}
