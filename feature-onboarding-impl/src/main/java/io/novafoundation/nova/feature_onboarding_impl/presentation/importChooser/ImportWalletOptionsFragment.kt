package io.novafoundation.nova.feature_onboarding_impl.presentation.importChooser

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import by.kirich1409.viewbindingdelegate.viewBinding
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.setupCustomDialogDisplayer
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.progress.observeProgressDialog
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.feature_cloud_backup_api.presenter.mixin.observeConfirmationAction
import io.novafoundation.nova.feature_onboarding_api.di.OnboardingFeatureApi
import io.novafoundation.nova.feature_onboarding_impl.R
import io.novafoundation.nova.feature_onboarding_impl.databinding.FragmentImportWalletOptionsBinding
import io.novafoundation.nova.feature_onboarding_impl.di.OnboardingFeatureComponent
import io.novafoundation.nova.feature_onboarding_impl.presentation.welcome.SelectHardwareWalletBottomSheet

class ImportWalletOptionsFragment : BaseFragment<ImportWalletOptionsViewModel, FragmentImportWalletOptionsBinding>() {

    override val binder by viewBinding(FragmentImportWalletOptionsBinding::bind)

    override fun initViews() {
        binder.importOptionsToolbar.applyStatusBarInsets()
        binder.importOptionsToolbar.setHomeButtonListener { viewModel.backClicked() }

        binder.importOptionPassphraseBig.setOnClickListener { importMnemonicClicked() }
        binder.importOptionPassphraseSmall.setOnClickListener { importMnemonicClicked() }
        binder.importOptionCloud.setOnClickListener { viewModel.importCloudClicked() }
        binder.importOptionHardware.setOnClickListener { viewModel.importHardwareClicked() }
        binder.importOptionWatchOnly.setOnClickListener { viewModel.importWatchOnlyClicked() }
        binder.importOptionRawSeed.setOnClickListener { viewModel.importRawSeedClicked() }
        binder.importOptionJson.setOnClickListener { viewModel.importJsonClicked() }
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
            SelectHardwareWalletBottomSheet(requireContext(), it.payload, it.onSuccess)
                .show()
        }

        viewModel.showImportViaCloudButton.observe {
            binder.importOptionCloud.setVisible(it)
            binder.importOptionPassphraseSmall.setVisible(it)
            binder.importOptionPassphraseBig.setVisible(!it)
        }
    }

    private fun importMnemonicClicked() {
        viewModel.importMnemonicClicked()
    }
}
