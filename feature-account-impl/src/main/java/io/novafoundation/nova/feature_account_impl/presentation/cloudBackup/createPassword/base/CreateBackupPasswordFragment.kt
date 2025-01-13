package io.novafoundation.nova.feature_account_impl.presentation.cloudBackup.createPassword.base

import android.widget.TextView
import androidx.lifecycle.lifecycleScope

import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.bindTo
import io.novafoundation.nova.common.utils.setCompoundDrawableTintRes
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.common.utils.switchPasswordInputType
import io.novafoundation.nova.common.view.bottomSheet.action.observeActionBottomSheet
import io.novafoundation.nova.common.view.setState
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.databinding.FragmentCreateCloudBackupPasswordBinding

abstract class CreateBackupPasswordFragment<T : BackupCreatePasswordViewModel> : BaseFragment<T, FragmentCreateCloudBackupPasswordBinding>() {

    override fun createBinding() = FragmentCreateCloudBackupPasswordBinding.inflate(layoutInflater)

    abstract val titleRes: Int
    abstract val subtitleRes: Int

    override fun initViews() {
        binder.createCloudBackupPasswordToolbar.applyStatusBarInsets()
        binder.createCloudBackupPasswordToolbar.setHomeButtonListener { viewModel.backClicked() }

        binder.createBackupPasswordTitle.setText(titleRes)
        binder.createBackupPasswordSubtitle.setText(subtitleRes)

        binder.createCloudBackupPasswordContinue.setOnClickListener { viewModel.continueClicked() }
        binder.createCloudBackupPasswordInput.setEndIconOnClickListener { viewModel.toggleShowPassword() }
        binder.createCloudBackupPasswordConfirmInput.setEndIconOnClickListener { viewModel.toggleShowPassword() }
        binder.createCloudBackupPasswordContinue.prepareForProgress(viewLifecycleOwner)
    }

    override fun subscribe(viewModel: T) {
        observeActionBottomSheet(viewModel)

        binder.createCloudBackupPasswordInput.content.bindTo(viewModel.passwordFlow, lifecycleScope)
        binder.createCloudBackupPasswordConfirmInput.content.bindTo(viewModel.passwordConfirmFlow, lifecycleScope)

        viewModel.passwordStateFlow.observe { state ->
            binder.createCloudBackupPasswordMinChars.requirementState(state.containsMinSymbols)
            binder.createCloudBackupPasswordNumbers.requirementState(state.hasNumbers)
            binder.createCloudBackupPasswordLetters.requirementState(state.hasLetters)
            binder.createCloudBackupPasswordPasswordsMatch.requirementState(state.passwordsMatch)
        }

        viewModel.continueButtonState.observe { state ->
            binder.createCloudBackupPasswordContinue.setState(state)
        }

        viewModel.showPasswords.observe {
            binder.createCloudBackupPasswordInput.content.switchPasswordInputType(it)
            binder.createCloudBackupPasswordConfirmInput.content.switchPasswordInputType(it)
        }
    }

    private fun TextView.requirementState(isValid: Boolean) {
        if (isValid) {
            setTextColorRes(R.color.text_positive)
            setCompoundDrawableTintRes(R.color.icon_positive)
        } else {
            setTextColorRes(R.color.text_secondary)
            setCompoundDrawableTintRes(R.color.icon_secondary)
        }
    }
}
