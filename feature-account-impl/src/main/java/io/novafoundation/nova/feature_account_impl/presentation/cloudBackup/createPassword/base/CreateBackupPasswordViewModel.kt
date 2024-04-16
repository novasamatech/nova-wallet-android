package io.novafoundation.nova.feature_account_impl.presentation.cloudBackup.createPassword.base

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.addColor
import io.novafoundation.nova.common.utils.formatting.spannable.spannableFormatting
import io.novafoundation.nova.common.utils.toggle
import io.novafoundation.nova.common.view.bottomSheet.action.ActionBottomSheet
import io.novafoundation.nova.common.view.bottomSheet.action.ActionBottomSheetLauncher
import io.novafoundation.nova.common.view.bottomSheet.action.primary
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.domain.cloudBackup.createPassword.CreateCloudBackupPasswordInteractor
import io.novafoundation.nova.feature_account_impl.domain.cloudBackup.createPassword.model.PasswordErrors
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_cloud_backup_api.presenter.action.launchRememberPasswordWarning
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine

class PasswordInputState(
    val containsMinSymbols: Boolean,
    val hasLetters: Boolean,
    val hasNumbers: Boolean,
    val passwordsMatch: Boolean
) {

    val isRequirementsSatisfied = containsMinSymbols && hasLetters && hasNumbers && passwordsMatch
}

abstract class BackupCreatePasswordViewModel(
    protected val router: AccountRouter,
    protected val resourceManager: ResourceManager,
    protected val interactor: CreateCloudBackupPasswordInteractor,
    private val actionBottomSheetLauncher: ActionBottomSheetLauncher
) : BaseViewModel(), ActionBottomSheetLauncher by actionBottomSheetLauncher {

    val passwordFlow = MutableStateFlow("")
    val passwordConfirmFlow = MutableStateFlow("")

    val _showPasswords = MutableStateFlow(false)

    val showPasswords: Flow<Boolean> = _showPasswords

    val passwordStateFlow = combine(passwordFlow, passwordConfirmFlow) { password, confirm ->
        val passwordErrors = interactor.checkPasswords(password, confirm)
        PasswordInputState(
            containsMinSymbols = PasswordErrors.TOO_SHORT !in passwordErrors,
            hasLetters = PasswordErrors.NO_LETTERS !in passwordErrors,
            hasNumbers = PasswordErrors.NO_DIGITS !in passwordErrors,
            passwordsMatch = PasswordErrors.PASSWORDS_DO_NOT_MATCH !in passwordErrors
        )
    }.shareInBackground()

    protected val backupInProgress = MutableStateFlow(false)

    val continueButtonState = combine(passwordStateFlow, backupInProgress) { passwordState, backupInProgress ->
        when {
            backupInProgress && passwordState.isRequirementsSatisfied -> DescriptiveButtonState.Loading
            passwordState.isRequirementsSatisfied -> DescriptiveButtonState.Enabled(resourceManager.getString(R.string.common_continue))
            else -> DescriptiveButtonState.Disabled(resourceManager.getString(R.string.cloud_backup_enter_password))
        }
    }.shareInBackground()

    init {
        showPasswordWarningDialog()
    }

    abstract fun continueClicked()

    open fun backClicked() {
        router.back()
    }

    fun toggleShowPassword() {
        _showPasswords.toggle()
    }

    fun showProgress(show: Boolean) {
        backupInProgress.value = show
    }

    private fun showPasswordWarningDialog() {
        actionBottomSheetLauncher.launchRememberPasswordWarning(resourceManager)
    }
}
