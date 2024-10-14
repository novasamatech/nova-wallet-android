package io.novafoundation.nova.feature_account_impl.presentation.startCreateWallet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.setupCustomDialogDisplayer
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.bindTo
import io.novafoundation.nova.common.view.bottomSheet.action.observeActionBottomSheet
import io.novafoundation.nova.common.view.setState
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.di.AccountFeatureComponent

class StartCreateWalletFragment : BaseFragment<StartCreateWalletViewModel, FragmentStartCreateWalletBinding>() {

    companion object {
        private const val KEY_PAYLOAD = "display_back"

        fun bundle(payload: StartCreateWalletPayload): Bundle {
            return Bundle().apply {
                putParcelable(KEY_PAYLOAD, payload)
            }
        }
    }

    override val binder by viewBinding(FragmentStartCreateWalletBinding::bind)

    override fun initViews() {
        binder.startCreateWalletToolbar.applyStatusBarInsets()
        binder.startCreateWalletToolbar.setHomeButtonListener { viewModel.backClicked() }

        binder. startCreateWalletConfirmName.setOnClickListener { viewModel.confirmNameClicked() }

        onBackPressed { viewModel.backClicked() }

        binder.startCreateWalletCloudBackupButton.setOnClickListener { viewModel.cloudBackupClicked() }
        binder.startCreateWalletManualBackupButton.setOnClickListener { viewModel.manualBackupClicked() }

        binder.startCreateWalletCloudBackupButton.prepareForProgress(viewLifecycleOwner)
        binder.startCreateWalletConfirmName.prepareForProgress(viewLifecycleOwner)
    }

    override fun inject() {
        FeatureUtils.getFeature<AccountFeatureComponent>(context!!, AccountFeatureApi::class.java)
            .startCreateWallet()
            .create(this, argument(KEY_PAYLOAD))
            .inject(this)
    }

    override fun subscribe(viewModel: StartCreateWalletViewModel) {
        setupCustomDialogDisplayer(viewModel)
        observeActionBottomSheet(viewModel)
        binder.startCreateWalletNameInput.bindTo(viewModel.nameInput, viewLifecycleOwner.lifecycleScope)

        viewModel.continueButtonState.observe { state ->
            binder.startCreateWalletConfirmName.setState(state)
        }

        viewModel.isSyncWithCloudEnabled.observe {
            binder.startCreateWalletSyncWithCloudEnabled.isVisible = it
        }

        viewModel.createWalletState.observe {
            binder.startCreateWalletNameInputLayout.isEndIconVisible = it == CreateWalletState.SETUP_NAME
            binder.startCreateWalletNameInput.isFocusable = it == CreateWalletState.SETUP_NAME
            binder.startCreateWalletNameInput.isFocusableInTouchMode = it == CreateWalletState.SETUP_NAME
        }

        viewModel.titleText.observe {
            binder.startCreateWalletTitle.text = it
        }

        viewModel.explanationText.observe {
            binder.startCreateWalletExplanation.text = it
        }

        viewModel.progressFlow.observe {
            binder.startCreateWalletCloudBackupButton.showProgress(it)
            binder.startCreateWalletManualBackupButton.isEnabled = !it
        }

        viewModel.showCloudBackupButton.observe {
            binder.startCreateWalletCloudBackupButton.isVisible = it
        }

        viewModel.showManualBackupButton.observe {
            binder.startCreateWalletManualBackupButton.isVisible = it
        }
    }
}
