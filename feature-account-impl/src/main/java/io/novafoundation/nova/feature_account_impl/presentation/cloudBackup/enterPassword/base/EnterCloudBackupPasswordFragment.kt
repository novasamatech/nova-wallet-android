package io.novafoundation.nova.feature_account_impl.presentation.cloudBackup.enterPassword.base

import androidx.lifecycle.lifecycleScope

import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.mixin.actionAwaitable.setupConfirmationDialog
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.bindTo
import io.novafoundation.nova.common.utils.switchPasswordInputType
import io.novafoundation.nova.common.view.bottomSheet.action.observeActionBottomSheet
import io.novafoundation.nova.common.view.setState
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.databinding.FragmentRestoreCloudBackupBinding

abstract class EnterCloudBackupPasswordFragment<T : EnterCloudBackupPasswordViewModel> : BaseFragment<T, FragmentRestoreCloudBackupBinding>() {

    override fun createBinding() = FragmentRestoreCloudBackupBinding.inflate(layoutInflater)

    abstract val titleRes: Int
    abstract val subtitleRes: Int

    override fun initViews() {
        binder.restoreCloudBackupToolbar.applyStatusBarInsets()
        binder.restoreCloudBackupToolbar.setHomeButtonListener { viewModel.backClicked() }

        binder.enterBackupPasswordTitle.setText(titleRes)
        binder.enterBackupPasswordSubtitle.setText(subtitleRes)

        binder.restoreCloudBackupContinueBtn.prepareForProgress(viewLifecycleOwner)
        binder.restoreCloudBackupContinueBtn.setOnClickListener { viewModel.continueClicked() }
        binder.restoreCloudBackupInput.setEndIconOnClickListener { viewModel.toggleShowPassword() }
        binder.restoreCloudBackupForgotPassword.setOnClickListener { viewModel.forgotPasswordClicked() }
    }

    override fun subscribe(viewModel: T) {
        observeActionBottomSheet(viewModel)
        setupConfirmationDialog(R.style.AccentNegativeAlertDialogTheme_Reversed, viewModel.confirmationAwaitableAction)

        binder.restoreCloudBackupInput.content.bindTo(viewModel.passwordFlow, lifecycleScope)

        viewModel.continueButtonState.observe { state ->
            binder.restoreCloudBackupContinueBtn.setState(state)
        }

        viewModel.showPassword.observe {
            binder.restoreCloudBackupInput.content.switchPasswordInputType(it)
        }
    }
}
