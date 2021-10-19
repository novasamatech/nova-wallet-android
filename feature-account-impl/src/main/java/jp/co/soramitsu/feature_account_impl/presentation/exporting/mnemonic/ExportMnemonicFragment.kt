package jp.co.soramitsu.feature_account_impl.presentation.exporting.mnemonic

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import jp.co.soramitsu.common.di.FeatureUtils
import jp.co.soramitsu.feature_account_api.di.AccountFeatureApi
import jp.co.soramitsu.feature_account_impl.R
import jp.co.soramitsu.feature_account_impl.di.AccountFeatureComponent
import jp.co.soramitsu.feature_account_impl.presentation.exporting.ExportFragment
import jp.co.soramitsu.feature_account_impl.presentation.exporting.ExportPayload
import jp.co.soramitsu.feature_account_impl.presentation.view.advanced.AdvancedBlockView.FieldState
import kotlinx.android.synthetic.main.fragment_export_mnemonic.exportMnemonicAdvanced
import kotlinx.android.synthetic.main.fragment_export_mnemonic.exportMnemonicConfirm
import kotlinx.android.synthetic.main.fragment_export_mnemonic.exportMnemonicExport
import kotlinx.android.synthetic.main.fragment_export_mnemonic.exportMnemonicToolbar
import kotlinx.android.synthetic.main.fragment_export_mnemonic.exportMnemonicType
import kotlinx.android.synthetic.main.fragment_export_mnemonic.exportMnemonicViewer

private const val PAYLOAD_KEY = "PAYLOAD_KEY"

class ExportMnemonicFragment : ExportFragment<ExportMnemonicViewModel>() {

    companion object {
        fun getBundle(exportPayload: ExportPayload): Bundle {
            return Bundle().apply {
                putParcelable(PAYLOAD_KEY, exportPayload)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_export_mnemonic, container, false)
    }

    override fun initViews() {
        exportMnemonicToolbar.setHomeButtonListener { viewModel.back() }

        configureAdvancedBlock()

        exportMnemonicConfirm.setOnClickListener { viewModel.openConfirmMnemonic() }

        exportMnemonicExport.setOnClickListener { viewModel.exportClicked() }
    }

    private fun configureAdvancedBlock() {
        with(exportMnemonicAdvanced) {
            configure(FieldState.DISABLED)
        }
    }

    override fun inject() {
        FeatureUtils.getFeature<AccountFeatureComponent>(requireContext(), AccountFeatureApi::class.java)
            .exportMnemonicFactory()
            .create(this, argument(PAYLOAD_KEY))
            .inject(this)
    }

    override fun subscribe(viewModel: ExportMnemonicViewModel) {
        super.subscribe(viewModel)

        exportMnemonicType.setMessage(viewModel.exportSource.nameRes)

        viewModel.mnemonicWordsFlow.observe {
            exportMnemonicViewer.submitList(it)
        }

        viewModel.exportingSecretFlow.observe {
            val derivationPath = it.derivationPath

            val state = if (derivationPath.isNullOrBlank()) FieldState.HIDDEN else FieldState.DISABLED

            with(exportMnemonicAdvanced) {
                configure(derivationPathField, state)

                setDerivationPath(derivationPath)
            }
        }

        viewModel.cryptoTypeFlow.observe {
            exportMnemonicAdvanced.setEncryption(it.name)
        }
    }
}
