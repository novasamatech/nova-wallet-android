package io.novafoundation.nova.feature_account_impl.presentation.cloudBackup.createPassword.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

abstract class CreateBackupPasswordFragment<T : BackupCreatePasswordViewModel> : BaseFragment<T>() {

    abstract val titleRes: Int
    abstract val subtitleRes: Int

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_create_cloud_backup_password, container, false)
    }

    override fun initViews() {
        createCloudBackupPasswordToolbar.applyStatusBarInsets()
        createCloudBackupPasswordToolbar.setHomeButtonListener { viewModel.backClicked() }

        createBackupPasswordTitle.setText(titleRes)
        createBackupPasswordSubtitle.setText(subtitleRes)

        createCloudBackupPasswordContinue.setOnClickListener { viewModel.continueClicked() }
        createCloudBackupPasswordInput.setEndIconOnClickListener { viewModel.toggleShowPassword() }
        createCloudBackupPasswordConfirmInput.setEndIconOnClickListener { viewModel.toggleShowPassword() }
        createCloudBackupPasswordContinue.prepareForProgress(viewLifecycleOwner)
    }

    override fun subscribe(viewModel: T) {
        observeActionBottomSheet(viewModel)

        createCloudBackupPasswordInput.content.bindTo(viewModel.passwordFlow, lifecycleScope)
        createCloudBackupPasswordConfirmInput.content.bindTo(viewModel.passwordConfirmFlow, lifecycleScope)

        viewModel.passwordStateFlow.observe { state ->
            createCloudBackupPasswordMinChars.requirementState(state.containsMinSymbols)
            createCloudBackupPasswordNumbers.requirementState(state.hasNumbers)
            createCloudBackupPasswordLetters.requirementState(state.hasLetters)
            createCloudBackupPasswordPasswordsMatch.requirementState(state.passwordsMatch)
        }

        viewModel.continueButtonState.observe { state ->
            createCloudBackupPasswordContinue.setState(state)
        }

        viewModel.showPasswords.observe {
            createCloudBackupPasswordInput.content.switchPasswordInputType(it)
            createCloudBackupPasswordConfirmInput.content.switchPasswordInputType(it)
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
