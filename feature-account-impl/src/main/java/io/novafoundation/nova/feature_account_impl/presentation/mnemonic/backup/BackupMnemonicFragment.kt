package io.novafoundation.nova.feature_account_impl.presentation.mnemonic.backup

import android.os.Bundle
import android.view.ContextThemeWrapper
import by.kirich1409.viewbindingdelegate.viewBinding
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.condition.setupConditions
import io.novafoundation.nova.common.utils.colorSpan
import io.novafoundation.nova.common.utils.formatting.spannable.SpannableFormatter
import io.novafoundation.nova.common.utils.toSpannable
import io.novafoundation.nova.common.view.dialog.dialog
import io.novafoundation.nova.common.view.setState
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.databinding.FragmentBackupMnemonicBinding
import io.novafoundation.nova.feature_account_impl.di.AccountFeatureComponent

class BackupMnemonicFragment : BaseFragment<BackupMnemonicViewModel, FragmentBackupMnemonicBinding>() {

    companion object {

        private const val KEY_ADD_ACCOUNT_PAYLOAD = "BackupMnemonicFragment.payload"

        fun getBundle(payload: BackupMnemonicPayload): Bundle {
            return Bundle().apply {
                putParcelable(KEY_ADD_ACCOUNT_PAYLOAD, payload)
            }
        }
    }

    override val binder by viewBinding(FragmentBackupMnemonicBinding::bind)

    override fun initViews() {
        binder.backupMnemonicToolbar.setHomeButtonListener { viewModel.homeButtonClicked() }
        binder.backupMnemonicToolbar.setRightActionClickListener { viewModel.optionsClicked() }

        buildConditions()

        binder.backupMnemonicContinue.setOnClickListener { viewModel.nextClicked() }
    }

    private fun buildConditions() {
        binder.backupMnemonicCondition1.setText(
            buildCondition(R.string.backup_secrets_warning_condition_1, R.string.backup_secrets_warning_condition_1_highlight)
        )
        binder.backupMnemonicCondition2.setText(
            buildCondition(R.string.backup_secrets_warning_condition_2, R.string.backup_secrets_warning_condition_2_highlight)
        )
        binder.backupMnemonicCondition3.setText(
            buildCondition(R.string.backup_secrets_warning_condition_3, R.string.backup_secrets_warning_condition_3_highlight)
        )

        viewModel.conditionMixin.setupConditions(
            binder.backupMnemonicCondition1,
            binder.backupMnemonicCondition2,
            binder.backupMnemonicCondition3
        )
    }

    private fun buildCondition(termBaseResId: Int, termHighlightResId: Int): CharSequence {
        return SpannableFormatter.format(
            getString(termBaseResId),
            getString(termHighlightResId)
                .toSpannable(colorSpan(requireContext().getColor(R.color.text_primary)))
        )
    }

    override fun inject() {
        FeatureUtils.getFeature<AccountFeatureComponent>(context!!, AccountFeatureApi::class.java)
            .backupMnemonicComponentFactory()
            .create(
                fragment = this,
                payload = argument(KEY_ADD_ACCOUNT_PAYLOAD)
            )
            .inject(this)
    }

    override fun subscribe(viewModel: BackupMnemonicViewModel) {
        viewModel.buttonState.observe(binder.backupMnemonicContinue::setState)

        viewModel.showMnemonicWarningDialog.observeEvent {
            showMnemonicWarning()
        }

        viewModel.mnemonicDisplay.observe {
            binder.backupMnemonicPassphrase.setWords(it)
        }
    }

    private fun showMnemonicWarning() = dialog(ContextThemeWrapper(requireContext(), R.style.AccentNegativeAlertDialogTheme)) {
        setTitle(R.string.backup_mnemonic_attention_title)
        setMessage(R.string.common_no_screenshot_message_v2_2_0)

        setPositiveButton(R.string.common_i_understand, null)
        setNegativeButton(R.string.common_cancel) { _, _ -> viewModel.warningDeclined() }
    }
}
