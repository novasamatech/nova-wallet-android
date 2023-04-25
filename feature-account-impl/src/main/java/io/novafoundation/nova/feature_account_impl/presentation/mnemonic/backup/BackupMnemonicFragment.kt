package io.novafoundation.nova.feature_account_impl.presentation.mnemonic.backup

import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.view.dialog.dialog
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.di.AccountFeatureComponent
import kotlinx.android.synthetic.main.fragment_backup_mnemonic.backupMnemonicContinue
import kotlinx.android.synthetic.main.fragment_backup_mnemonic.backupMnemonicPhrase
import kotlinx.android.synthetic.main.fragment_backup_mnemonic.backupMnemonicToolbar

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

        backupMnemonicContinue.setOnClickListener { viewModel.nextClicked() }
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
        viewModel.continueText.observe(backupMnemonicContinue::setText)

        viewModel.showMnemonicWarningDialog.observeEvent {
            showMnemonicWarning()
        }

        viewModel.mnemonicDisplay.observe {
            backupMnemonicPhrase.setMessage(it.orEmpty())
        }
    }

    private fun showMnemonicWarning() = dialog(ContextThemeWrapper(requireContext(), R.style.AccentNegativeAlertDialogTheme)) {
        setTitle(R.string.common_attention)
        setMessage(R.string.common_no_screenshot_message_v2_2_0)

        setPositiveButton(R.string.common_i_understand) { _, _ -> viewModel.warningAccepted() }
        setNegativeButton(R.string.common_cancel) { _, _ -> viewModel.warningDeclined() }
    }
}
