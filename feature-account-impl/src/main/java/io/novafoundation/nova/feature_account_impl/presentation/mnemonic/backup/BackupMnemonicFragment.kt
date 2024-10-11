package io.novafoundation.nova.feature_account_impl.presentation.mnemonic.backup

import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import io.novafoundation.nova.feature_account_impl.di.AccountFeatureComponent

class BackupMnemonicFragment : BaseFragment<BackupMnemonicViewModel>() {

    companion object {

        private const val KEY_ADD_ACCOUNT_PAYLOAD = "BackupMnemonicFragment.payload"

        fun getBundle(payload: BackupMnemonicPayload): Bundle {
            return Bundle().apply {
                putParcelable(KEY_ADD_ACCOUNT_PAYLOAD, payload)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_backup_mnemonic, container, false)
    }

    override fun initViews() {
        backupMnemonicToolbar.setHomeButtonListener { viewModel.homeButtonClicked() }
        backupMnemonicToolbar.setRightActionClickListener { viewModel.optionsClicked() }

        buildConditions()

        backupMnemonicContinue.setOnClickListener { viewModel.nextClicked() }
    }

    private fun buildConditions() {
        backupMnemonicCondition1.setText(buildCondition(R.string.backup_secrets_warning_condition_1, R.string.backup_secrets_warning_condition_1_highlight))
        backupMnemonicCondition2.setText(buildCondition(R.string.backup_secrets_warning_condition_2, R.string.backup_secrets_warning_condition_2_highlight))
        backupMnemonicCondition3.setText(buildCondition(R.string.backup_secrets_warning_condition_3, R.string.backup_secrets_warning_condition_3_highlight))

        viewModel.conditionMixin.setupConditions(
            backupMnemonicCondition1,
            backupMnemonicCondition2,
            backupMnemonicCondition3
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
        viewModel.buttonState.observe(backupMnemonicContinue::setState)

        viewModel.showMnemonicWarningDialog.observeEvent {
            showMnemonicWarning()
        }

        viewModel.mnemonicDisplay.observe {
            backupMnemonicPassphrase.setWords(it)
        }
    }

    private fun showMnemonicWarning() = dialog(ContextThemeWrapper(requireContext(), R.style.AccentNegativeAlertDialogTheme)) {
        setTitle(R.string.backup_mnemonic_attention_title)
        setMessage(R.string.common_no_screenshot_message_v2_2_0)

        setPositiveButton(R.string.common_i_understand, null)
        setNegativeButton(R.string.common_cancel) { _, _ -> viewModel.warningDeclined() }
    }
}
