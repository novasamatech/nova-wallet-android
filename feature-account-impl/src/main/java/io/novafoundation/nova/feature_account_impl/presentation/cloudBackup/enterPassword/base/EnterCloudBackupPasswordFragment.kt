package io.novafoundation.nova.feature_account_impl.presentation.cloudBackup.enterPassword.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.mixin.actionAwaitable.setupConfirmationDialog
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.bindTo
import io.novafoundation.nova.common.utils.switchPasswordInputType
import io.novafoundation.nova.common.view.bottomSheet.action.observeActionBottomSheet
import io.novafoundation.nova.common.view.setState
import io.novafoundation.nova.feature_account_impl.R

abstract class EnterCloudBackupPasswordFragment<T : EnterCloudBackupPasswordViewModel> : BaseFragment<T, FragmentRestoreCloudBackupBinding>() {

    override val binder by viewBinding(FragmentRestoreCloudBackupBinding::bind)

    abstract val titleRes: Int
    abstract val subtitleRes: Int

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_restore_cloud_backup, container, false)
    }

    override fun initViews() {
        restoreCloudBackupToolbar.applyStatusBarInsets()
        restoreCloudBackupToolbar.setHomeButtonListener { viewModel.backClicked() }

        enterBackupPasswordTitle.setText(titleRes)
        enterBackupPasswordSubtitle.setText(subtitleRes)

        restoreCloudBackupContinueBtn.prepareForProgress(viewLifecycleOwner)
        restoreCloudBackupContinueBtn.setOnClickListener { viewModel.continueClicked() }
        restoreCloudBackupInput.setEndIconOnClickListener { viewModel.toggleShowPassword() }
        restoreCloudBackupForgotPassword.setOnClickListener { viewModel.forgotPasswordClicked() }
    }

    override fun subscribe(viewModel: T) {
        observeActionBottomSheet(viewModel)
        setupConfirmationDialog(R.style.AccentNegativeAlertDialogTheme_Reversed, viewModel.confirmationAwaitableAction)

        restoreCloudBackupInput.content.bindTo(viewModel.passwordFlow, lifecycleScope)

        viewModel.continueButtonState.observe { state ->
            restoreCloudBackupContinueBtn.setState(state)
        }

        viewModel.showPassword.observe {
            restoreCloudBackupInput.content.switchPasswordInputType(it)
        }
    }
}
