package io.novafoundation.nova.feature_onboarding_impl.presentation.importChooser

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.setupCustomDialogDisplayer
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.progress.observeProgressDialog
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.feature_cloud_backup_api.presenter.mixin.observeConfirmationAction
import io.novafoundation.nova.feature_onboarding_api.di.OnboardingFeatureApi
import io.novafoundation.nova.feature_onboarding_impl.R
import io.novafoundation.nova.feature_onboarding_impl.di.OnboardingFeatureComponent
import io.novafoundation.nova.feature_onboarding_impl.presentation.welcome.SelectHardwareWalletBottomSheet
import kotlinx.android.synthetic.main.fragment_import_wallet_options.importOptionCloud
import kotlinx.android.synthetic.main.fragment_import_wallet_options.importOptionHardware
import kotlinx.android.synthetic.main.fragment_import_wallet_options.importOptionJson
import kotlinx.android.synthetic.main.fragment_import_wallet_options.importOptionPassphraseBig
import kotlinx.android.synthetic.main.fragment_import_wallet_options.importOptionPassphraseSmall
import kotlinx.android.synthetic.main.fragment_import_wallet_options.importOptionRawSeed
import kotlinx.android.synthetic.main.fragment_import_wallet_options.importOptionWatchOnly
import kotlinx.android.synthetic.main.fragment_import_wallet_options.importOptionsToolbar

class ImportWalletOptionsFragment : BaseFragment<ImportWalletOptionsViewModel>() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_import_wallet_options, container, false)
    }

    override fun initViews() {
        importOptionsToolbar.applyStatusBarInsets()
        importOptionsToolbar.setHomeButtonListener { viewModel.backClicked() }

        importOptionPassphraseBig.setOnClickListener { importMnemonicClicked() }
        importOptionPassphraseSmall.setOnClickListener { importMnemonicClicked() }
        importOptionCloud.setOnClickListener { viewModel.importCloudClicked() }
        importOptionHardware.setOnClickListener { viewModel.importHardwareClicked() }
        importOptionWatchOnly.setOnClickListener { viewModel.importWatchOnlyClicked() }
        importOptionRawSeed.setOnClickListener { viewModel.importRawSeedClicked() }
        importOptionJson.setOnClickListener { viewModel.importJsonClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<OnboardingFeatureComponent>(context!!, OnboardingFeatureApi::class.java)
            .importWalletOptionsComponentFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: ImportWalletOptionsViewModel) {
        setupCustomDialogDisplayer(viewModel)
        observeProgressDialog(viewModel.progressDialogMixin)
        observeConfirmationAction(viewModel.cloudBackupChangingWarningMixin)

        viewModel.selectHardwareWallet.awaitableActionLiveData.observeEvent {
            SelectHardwareWalletBottomSheet(requireContext(), it.onSuccess)
                .show()
        }

        viewModel.showImportViaCloudButton.observe {
            importOptionCloud.setVisible(it)
            importOptionPassphraseSmall.setVisible(it)
            importOptionPassphraseBig.setVisible(!it)
        }
    }

    private fun importMnemonicClicked() {
        viewModel.importMnemonicClicked()
    }
}
