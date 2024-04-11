package io.novafoundation.nova.feature_account_impl.presentation.cloudBackup.restoreBackup

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.base.showError
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.mixin.actionAwaitable.ConfirmationDialogInfo
import io.novafoundation.nova.common.mixin.actionAwaitable.confirmingAction
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.addColor
import io.novafoundation.nova.common.utils.formatting.spannable.spannableFormatting
import io.novafoundation.nova.common.utils.toggle
import io.novafoundation.nova.common.view.bottomSheet.action.ActionBottomSheet
import io.novafoundation.nova.common.view.bottomSheet.action.ActionBottomSheetLauncher
import io.novafoundation.nova.common.view.bottomSheet.action.negative
import io.novafoundation.nova.common.view.bottomSheet.action.secondary
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.domain.cloudBackup.enterPassword.RestoreCloudBackupInteractor
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
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

    val _showPassword = MutableStateFlow(false)
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
        actionBottomSheetLauncher.launchBottomSheet(
            imageRes = R.drawable.ic_cloud_backup_error,
            title = resourceManager.getString(R.string.corrupted_backup_error_title),
            subtitle = with(resourceManager) {
                val highlightedPart = getString(R.string.corrupted_backup_error_subtitle_highlighted)
                    .addColor(getColor(R.color.text_primary))

                getString(R.string.corrupted_backup_error_subtitle).spannableFormatting(highlightedPart)
            },
            neutralButtonPreferences = ActionBottomSheet.ButtonPreferences.secondary(resourceManager.getString(R.string.common_cancel)),
            actionButtonPreferences = ActionBottomSheet.ButtonPreferences.negative(
                resourceManager.getString(R.string.cloud_backup_delete_button),
                ::confirmCloudBackupDelete
            )
        )
    }

    fun toggleShowPassword() {
        _showPassword.toggle()
    }

    fun forgotPasswordClicked() {
        actionBottomSheetLauncher.launchBottomSheet(
            imageRes = R.drawable.ic_cloud_backup_password,
            title = resourceManager.getString(R.string.restore_cloud_backup_delete_backup_title),
            subtitle = with(resourceManager) {
                val highlightedFirstPart = getString(R.string.restore_cloud_backup_delete_backup_description_highlighted_1)
                    .addColor(getColor(R.color.text_primary))

                val highlightedSecondPart = getString(R.string.restore_cloud_backup_delete_backup_description_highlighted_2)
                    .addColor(getColor(R.color.text_primary))

                getString(R.string.restore_cloud_backup_delete_backup_description).spannableFormatting(highlightedFirstPart, highlightedSecondPart)
            },
            neutralButtonPreferences = ActionBottomSheet.ButtonPreferences.secondary(resourceManager.getString(R.string.common_cancel)),
            actionButtonPreferences = ActionBottomSheet.ButtonPreferences.negative(
                resourceManager.getString(R.string.cloud_backup_delete_button),
                ::confirmCloudBackupDelete
            )
        )
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
            confirmationAwaitableAction.awaitAction(
                ConfirmationDialogInfo(
                    title = R.string.cloud_backup_delete_backup_confirmation_title,
                    message = R.string.cloud_backup_delete_backup_confirmation_message,
                    positiveButton = R.string.cloud_backup_delete_button,
                    negativeButton = R.string.common_cancel
                )
            )

            interactor.deleteCloudBackup()
                .onSuccess { router.back() }
                .onFailure { throwable ->
                    val titleAndMessage = mapDeleteBackupFailureToUi(resourceManager, throwable)
                    titleAndMessage?.let { showError(it) }
                }
        }
    }
}
